/*
 *  Copyright (c) 2022 Raffael Herzog
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
import ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action1;
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
public class EndpointHandler<B, T> implements HttpHandler {

  private static final Function<? super HttpServerExchange, ?> INITIAL_CONTEXT = e -> {
    throw new IllegalStateException("Context factory not set");
  };

  private final HttpStatus defaultStatus;
  private final Supplier<? extends HttpDecoder<? extends B>> decoder;
  private final Processor<? super B, ? extends T> processor;
  private final Option<Supplier<? extends HttpEncoder<? super T>>> encoder;

  public EndpointHandler(
      HttpStatus defaultStatus, Supplier<? extends HttpDecoder<? extends B>> decoder,
      Processor<? super B, ? extends T> processor,
      Option<Supplier<? extends HttpEncoder<? super T>>> encoder) {
    this.defaultStatus = defaultStatus;
    this.decoder = decoder;
    this.processor = processor;
    this.encoder = encoder;
  }

  public static <C> EndpointHandler<EmptyBody, EmptyBody> initial() {
    // good code red in IDEA:
    //noinspection Convert2Diamond
    return new EndpointHandler<EmptyBody, EmptyBody>(HttpStatus.OK,
        HttpDecoder.IgnoreBodyDecoder::emptyBody, Processor.nop(), none());
  }

  public EndpointHandler<B, T> defaultStatus(HttpStatus defaultStatus) {
    return new EndpointHandler<>(defaultStatus, decoder, processor, encoder);
  }

  public <BB> EndpointHandler<BB, BB> decoder(Supplier<? extends HttpDecoder<? extends BB>> decoder) {
    return new EndpointHandler<>(defaultStatus, decoder, Processor.nop(), none());
  }

  public <BB> EndpointHandler<BB, BB> decoder(HttpDecoder<? extends BB> decoder) {
    return decoder(() -> decoder);
  }

  public <U> EndpointHandler<B, U> processor(Processor<? super T, ? extends U> next) {
    return new EndpointHandler<>(defaultStatus, decoder, processor.append(next), none());
  }

  public EndpointHandler<B, T> encoder(Supplier<? extends HttpEncoder<? super T>> encoder) {
    return new EndpointHandler<>(defaultStatus, decoder, processor, some(encoder));
  }

  public EndpointHandler<B, T> encoder(HttpEncoder<? super T> encoder) {
    return encoder(() -> encoder);
  }

  public EndpointHandler<B, T> fallbackEncoder(Supplier<? extends HttpEncoder<? super T>> encoder) {
    if (this.encoder.isDefined()) {
      return this;
    }
    return encoder(encoder);
  }

  public EndpointHandler<B, T> fallbackEncoder(HttpEncoder<? super T> encoder) {
    return fallbackEncoder(() -> encoder);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
      return;
    }
    decoder.get().decode(exchange, this::consumeBody);
  }

  private void consumeBody(HttpServerExchange exchange, B body) {
    var result = Processor.invoke(State.of(exchange, body), processor);
    if (result.isException()) {
      Throwable exception = result.exception();
      ErrorMessageHandler.addMessage(exchange, exception);
      if (result.exceptionHttpStatus().isDefined()) {
        applyHttpStatus(exchange, result.exceptionHttpStatus().get());
      } else if (exception instanceof HttpStatusException) {
        applyHttpStatus(exchange, ((HttpStatusException) exception).status());
      } else {
        applyHttpStatus(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } else {
      applyHttpStatus(exchange, result.httpStatus.getOrElse(defaultStatus));
      if (encoder.isEmpty() || result.value() instanceof EmptyBody) {
        EmptyBody.encoder().encode(exchange, EmptyBody.empty());
      } else {
        encoder.get().get().encode(exchange, result.value());
      }
    }
  }

  private void applyHttpStatus(HttpServerExchange exchange, HttpStatus status) {
    exchange.setStatusCode(status.code());
    exchange.setReasonPhrase(status.reason());
  }

  @FunctionalInterface
  public interface Processor<T, R> {
    State<? extends R> process(State<? extends T> state) throws Exception;

    default <U> Processor<T, U> append(Processor<? super R, ? extends U> next) {
      return s -> invoke(invoke(s, this), next);
    }

    static <T> Processor<T, T> nop() {
      return s -> s;
    }

    static <T, U> State<? extends U> invoke(State<T> state, Processor<? super T, ? extends U> processor) {
      try {
        return processor.process(state);
      } catch (Throwable e) {
        Exceptions.rethrowIfFatal(e, state.isException() ? state.exception() : null);
        return state.exception(e);
      }
    }
  }

  public static final class State<T> {
    final HttpServerExchange exchange;
    final Either<Throwable, T> value;
    final Option<HttpStatus> httpStatus;
    final Option<HttpStatus> exceptionHttpStatus;

    private State(HttpServerExchange exchange, Either<Throwable, T> value,
                  Option<HttpStatus> httpStatus, Option<HttpStatus> exceptionHttpStatus) {
      this.exchange = exchange;
      this.value = value;
      this.httpStatus = httpStatus;
      this.exceptionHttpStatus = exceptionHttpStatus;
    }

    private static <B> State<B> of(HttpServerExchange exchange, B body) {
      return new State<>(exchange, Either.right(body), none(), none());
    }

    public boolean isException() {
      return value.isLeft();
    }

    @SuppressWarnings("unchecked")
    public <U> State<U> promoteException() {
      checkException();
      return (State<U>) this;
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

    public Option<HttpStatus> exceptionHttpStatus() {
      return exceptionHttpStatus;
    }

    public <U> State<U> value(U value) {
      checkValue();
      return new State<>(exchange, Either.right(value), httpStatus, none());
    }

    public <U> State<U> recover(U value) {
      return new State<>(exchange, Either.right(value), exceptionHttpStatus.orElse(httpStatus), none());
    }

    public <U> State<U> map(Action1<? super T, ? extends U> mapper) {
      if (isException()) return promoteException();
      try {
        return value(mapper.perform(value()));
      } catch (Exception e) {
        return exception(e);
      }
    }

    @SuppressWarnings("ObjectEquality")
    public <U> State<U> exception(Throwable exception) {
      Exceptions.rethrowIfFatal(exception);
      if (isException() && exception != exception()) {
        exception.addSuppressed(exception());
      }
      return new State<>(exchange, Either.left(exception), httpStatus, none());
    }

    public State<T> httpStatus(HttpStatus httpStatus) {
      return httpStatus(some(httpStatus));
    }

    public State<T> httpStatus(Option<HttpStatus> httpStatus) {
      return new State<>(exchange, value, httpStatus.orElse(this.httpStatus), exceptionHttpStatus);
    }

    public State<T> clearHttpStatus() {
      return new State<>(exchange, value, none(), exceptionHttpStatus);
    }

    public State<T> exceptionHttpStatus(HttpStatus exceptionHttpStatus) {
      return exceptionHttpStatus(some(exceptionHttpStatus));
    }

    public State<T> exceptionHttpStatus(Option<HttpStatus> exceptionHttpStatus) {
      return new State<>(exchange, value, httpStatus, exceptionHttpStatus.orElse(this.exceptionHttpStatus));
    }

    public State<T> clearExceptionHttpStatus() {
      return new State<>(exchange, value, httpStatus, none());
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
      return "State[" + (isException() ? exception() : value() + "->" + httpStatus() + "/" + exceptionHttpStatus()) + "]";
    }
  }
}
