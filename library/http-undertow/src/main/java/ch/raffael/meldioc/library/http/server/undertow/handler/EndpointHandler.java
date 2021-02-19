/*
 *  Copyright (c) 2021 Raffael Herzog
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to
 *  deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 *  sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 *  IN THE SOFTWARE.
 */

package ch.raffael.meldioc.library.http.server.undertow.handler;

import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatus;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import ch.raffael.meldioc.util.Exceptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.function.Function;
import java.util.function.Supplier;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * TODO JavaDoc
 */
public class EndpointHandler<C, B, T> implements HttpHandler {

  private static final Function<? super HttpServerExchange, ?> INITIAL_CONTEXT = e -> {
    throw new IllegalStateException("Context factory not set");
  };

  private final HttpStatus defaultStatus;
  private final Supplier<? extends HttpDecoder<? super C, ? extends B>> decoder;
  private final Processor<? super B, ? extends T> processor;
  private final Supplier<? extends HttpEncoder<? super C, ? super T>> encoder;
  private final Function<? super HttpServerExchange, ? extends C> context;

  public EndpointHandler(
      HttpStatus defaultStatus, Supplier<? extends HttpDecoder<? super C, ? extends B>> decoder,
      Processor<? super B, ? extends T> processor,
      Supplier<? extends HttpEncoder<? super C, ? super T>> encoder,
      Function<? super HttpServerExchange, ? extends C> context) {
    this.defaultStatus = defaultStatus;
    this.decoder = decoder;
    this.processor = processor;
    this.encoder = encoder;
    this.context = context;
  }

  public static <C> EndpointHandler<C, EmptyBody, EmptyBody> initial() {
    return new EndpointHandler<C, EmptyBody, EmptyBody>(HttpStatus.OK,
        HttpDecoder.IgnoreBodyDecoder::emptyBody, Processor.nop(), HttpEncoder::emptyBody, initialContext());
  }

  @SuppressWarnings("unchecked")
  private static <C> Function<? super HttpServerExchange, ? extends C> initialContext() {
    return (Function<? super HttpServerExchange, ? extends C>) INITIAL_CONTEXT;
  }

  public EndpointHandler<C, B, T> defaultStatus(HttpStatus defaultStatus) {
    return new EndpointHandler<>(defaultStatus, decoder, processor, encoder, context);
  }

  public <BB> EndpointHandler<C, BB, BB> decoder(Supplier<? extends HttpDecoder<? super C, ? extends BB>> decoder) {
    return new EndpointHandler<>(defaultStatus, decoder, Processor.nop(), HttpEncoder::emptyBody, context);
  }

  public <BB> EndpointHandler<C, BB, BB> decoder(HttpDecoder<? super C, ? extends BB> decoder) {
    return decoder(() -> decoder);
  }

  public <U> EndpointHandler<C, B, U> processor(Processor<? super T, ? extends U> next) {
    return new EndpointHandler<>(defaultStatus, decoder, processor.append(next), HttpEncoder::emptyBody, context);
  }

  public EndpointHandler<C, B, T> encoder(Supplier<? extends HttpEncoder<? super C, ? super T>> encoder) {
    return new EndpointHandler<>(defaultStatus, decoder, processor, encoder, context);
  }

  public EndpointHandler<C, B, T> encoder(HttpEncoder<? super C, ? super T> encoder) {
    return encoder(() -> encoder);
  }

  public EndpointHandler<C, B, T> context(Function<? super HttpServerExchange, ? extends C> context) {
    return new EndpointHandler<>(defaultStatus, decoder, processor, encoder, context);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
      return;
    }
    var ctx = context.apply(exchange);
    decoder.get().decode(exchange, ctx, (e, b) -> consumeBody(e, ctx, b));
  }

  private void consumeBody(HttpServerExchange exchange, C ctx, B body) {
    var result = process(State.of(exchange, body), processor);
    if (result.isException()) {
      Throwable exception = result.exception();
      ErrorMessageHandler.addMessage(exchange, exception);
      if (exception instanceof HttpStatusException) {
        applyHttpStatus(exchange, ((HttpStatusException) exception).status());
      } else {
        applyHttpStatus(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } else {
      applyHttpStatus(exchange, result.httpStatus.getOrElse(defaultStatus));
      encoder.get().encode(exchange, ctx, result.value.get());
    }
  }

  private void applyHttpStatus(HttpServerExchange exchange, HttpStatus status) {
    exchange.setStatusCode(status.code());
    exchange.setReasonPhrase(status.reason());
  }

  private static <T, U> State<? extends U> process(State<T> state, Processor<? super T, ? extends U> processor) {
    try {
      return processor.process(state);
    } catch (Throwable e) {
      Exceptions.rethrowIfFatal(e);
      return state.exception(e);
    }
  }

  @FunctionalInterface
  public interface Processor<T, R> {
    State<? extends R> process(State<? extends T> state) throws Exception;

    default <U> Processor<T, U> append(Processor<? super R, ? extends U> next) {
      return s -> next.process(this.process(s));
    }

    static <T> Processor<T, T> nop() {
      return s -> s;
    }
  }

  public static final class State<T> {
    final HttpServerExchange exchange;
    final Either<Throwable, T> value;
    final Option<HttpStatus> httpStatus;

    private State(HttpServerExchange exchange, Either<Throwable, T> value, Option<HttpStatus> httpStatus) {
      this.exchange = exchange;
      this.value = value;
      this.httpStatus = httpStatus;
    }

    private static <B> State<B> of(HttpServerExchange exchange, B body) {
      return new State<>(exchange, Either.right(body), none());
    }

    public boolean isException() {
      return value.isLeft();
    }

    public <U> State<U> promoteException() {
      return exception(exception());
    }

    public T value() {
      checkValue();
      return value.get();
    }

    public Throwable exception() {
      checkException();
      if (value.isRight()) {
        throw new IllegalStateException("State is a value: " + value.get());
      }
      return value.getLeft();
    }

    public HttpServerExchange exchange() {
      return exchange;
    }

    public Option<HttpStatus> httpStatus() {
      return httpStatus;
    }

    public <U> State<U> value(U value) {
      checkValue();
      return recover(value);
    }

    public <U> State<U> recover(U value) {
      return new State<>(exchange, Either.right(value), httpStatus);
    }

    public <U> State<U> exception(Throwable exception) {
      Exceptions.rethrowIfFatal(exception);
      if (isException()) {
        exception.addSuppressed(exception());
      }
      return new State<>(exchange, Either.left(exception), httpStatus);
    }

    public State<T> httpStatus(HttpStatus httpStatus) {
      return httpStatus(some(httpStatus));
    }

    public State<T> httpStatus(Option<HttpStatus> httpStatus) {
      return new State<>(exchange, value, httpStatus);
    }

    private void checkValue() {
      if (value.isLeft()) {
        var throwing = new IllegalStateException("State is an exception: " + value.getLeft());
        throwing.addSuppressed(value.getLeft());
        throw throwing;
      }
    }

    private void checkException() {
      if (value.isRight()) {
        throw new IllegalStateException("State is a value: " + value.get());
      }
    }

    @Override
    public String toString() {
      return "State[" + (isException() ? exception() : value() + "->" + httpStatus()) + "]";
    }

    //    @SuppressWarnings("unchecked")
//    public static <U> State<U> covariant(State<? extends U> state) {
//      return (State<U>) state;
//    }
  }
}
