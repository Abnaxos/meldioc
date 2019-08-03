/*
 *  Copyright (c) 2019 Raffael Herzog
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

package ch.raffael.compose.http.undertow.routing;

import ch.raffael.compose.http.undertow.codec.Decoder;
import ch.raffael.compose.http.undertow.codec.EmptyBody;
import ch.raffael.compose.http.undertow.codec.Encoder;
import ch.raffael.compose.http.undertow.handler.ActionHandler;
import ch.raffael.compose.http.undertow.handler.HttpMethodHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.vavr.collection.Set;

import java.util.function.Function;

/**
 * Builder for HTTP actions.
 */
public class ActionBuilder<C, B, R> {

  final Frame<C> frame;

  final Set<HttpMethodHandler.Method> methods;
  final Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends B>> decoder;
  final Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super R>> encoder;

  ActionBuilder(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
                Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends B>> decoder,
                Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super R>> encoder) {
    this.frame = frame;
    this.methods = methods;
    this.decoder = decoder;
    this.encoder = encoder;
  }

  void conclude(ActionHandler.Invoker<C, B, R> invoker) {
    methods.forEach(m -> frame.action(m, new LazyActionHandler<C, B, R>(decoder, encoder, invoker)));
  }

  @FunctionalInterface
  public interface ActionC0<C, R> {
    R perform(C ctx) throws Exception;
  }

  @FunctionalInterface
  public interface ActionC1<C, P1, R> {
    R perform(C ctx, P1 arg1);
  }

  @FunctionalInterface
  public interface ActionC2<C, P1, P2, R> {
    R perform(C ctx, P1 arg1, P2 arg2);
  }

  @FunctionalInterface
  public interface ActionC3<C, P1, P2, P3, R> {
    R perform(C ctx, P1 arg1, P2 arg2, P3 arg3);
  }

  @FunctionalInterface
  public interface Action0<R> {
    R perform() throws Exception;
  }

  @FunctionalInterface
  public interface Action1<P1, R> {
    R perform(P1 arg1);
  }

  @FunctionalInterface
  public interface Action2<P1, P2, R> {
    R perform(P1 arg1, P2 arg2);
  }

  @FunctionalInterface
  public interface Action3<P1, P2, P3, R> {
    R perform(P1 arg1, P2 arg2, P3 arg3);
  }

  public static final class AcceptSome<C, B, R> extends ActionBuilder<C, B, R> {

    AcceptSome(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
               Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends B>> decoder,
               Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super R>> encoder) {
      super(frame, methods, decoder, encoder);
    }

    public <BB> AcceptSome<C, BB, R> acceptWith(Decoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public AcceptSome<C, ? super String, R> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> AcceptSome<C, T, R> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    private <RR> AcceptSome<C, B, RR> produceWith(Function<Frame<? super C>, ? extends Encoder<? super C, ? super RR>> encoder) {
      return new AcceptSome<>(frame, methods, decoder, encoder);
    }

    public <RR> AcceptSome<C, B, RR> produceWith(Encoder<? super C, ? super RR> encoder) {
      return produceWith(__ -> encoder);
    }

    public AcceptSome<C, B, CharSequence> producePlainText() {
      return produceWith(f -> f.enc.plainText());
    }

    public AcceptSome<C, B, CharSequence> produceHtml() {
      return produceWith(f -> f.enc.html());
    }

    public AcceptSome<C, B, Object> produceObject() {
      return produceWith(f -> f.enc.object(Object.class));
    }

    public <T> AcceptSome<C, B, T> produce(Class<T> type) {
      return produceWith(f -> f.enc.object(type));
    }

    public void with(ActionC1<? super C, ? super B, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, b));
    }

    public <P1> void with(Capture<P1> p1, ActionC2<? super C, ? super B, ? super P1, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, b, p1.get(x)));
    }

    public <P1, P2> void with(Capture<P1> p1, Capture<P2> p2, ActionC3<? super C, ? super B, ? super P1, ? super P2, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, b, p1.get(x), p2.get(x)));
    }

    public void with(Action1<? super B, ? extends R> action) {
      conclude((x, c, b) -> action.perform(b));
    }

    public <P1> void with(Capture<P1> p1, Action2<? super B, ? super P1, ? extends R> action) {
      conclude((x, c, b) -> action.perform(b, p1.get(x)));
    }

    public <P1, P2> void with(Capture<P1> p1, Capture<P2> p2, Action3<? super B, ? super P1, ? super P2, ? extends R> action) {
      conclude((x, c, b) -> action.perform(b, p1.get(x), p2.get(x)));
    }

    private <BB> AcceptSome<C, BB, R> withDecoder(Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends BB>> decoder) {
      return new AcceptSome<>(frame, methods, decoder, this.encoder);
    }
  }

  public static final class AcceptNone<C, R> extends ActionBuilder<C, EmptyBody, R> {

    AcceptNone(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
               Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super R>> encoder) {
      super(frame, methods, __ -> EmptyBody.decoder(), encoder);
    }

    public <BB> AcceptSome<C, BB, R> withDecoder(Decoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public AcceptSome<C, ? super String, R> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> AcceptSome<C, T, R> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    public <RR> AcceptNone<C, RR> produceWith(Encoder<? super C, ? super RR> encoder) {
      return withEncoder(__ -> encoder);
    }

    public AcceptNone<C, CharSequence> producePlainText() {
      return withEncoder(f -> f.enc.plainText());
    }

    public AcceptNone<C, CharSequence> produceHtml() {
      return withEncoder(f -> f.enc.html());
    }

    public AcceptNone<C, Object> produceObject() {
      return withEncoder(f -> f.enc.object(Object.class));
    }

    public <T> AcceptNone<C, T> produce(Class<T> type) {
      return withEncoder(f -> f.enc.object(type));
    }

    public void with(ActionC0<? super C, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c));
    }

    public <P1> void with(Capture<P1> p1, ActionC1<? super C, ? super P1, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, p1.get(x)));
    }

    public <P1, P2> void with(Capture<P1> p1, Capture<P2> p2, ActionC2<? super C, ? super P1, ? super P2, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, p1.get(x), p2.get(x)));
    }

    public <P1, P2, P3> void with(Capture<P1> p1, Capture<P2> p2, Capture<P3> p3, ActionC3<? super C, ? super P1, ? super P2, ? super P3, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, p1.get(x), p2.get(x), p3.get(x)));
    }

    public <P1> void with(Capture<P1> p1, Action1<? super P1, ? extends R> action) {
      conclude((x, c, b) -> action.perform(p1.get(x)));
    }

    public <P1, P2> void with(Capture<P1> p1, Capture<P2> p2, Action2<? super P1, ? super P2, ? extends R> action) {
      conclude((x, c, b) -> action.perform(p1.get(x), p2.get(x)));
    }

    public <P1, P2, P3> void with(Capture<P1> p1, Capture<P2> p2, Capture<P3> p3, Action3<? super P1, ? super P2, ? super P3, ? extends R> action) {
      conclude((x, c, b) -> action.perform(p1.get(x), p2.get(x), p3.get(x)));
    }

    private <BB> AcceptSome<C, BB, R> withDecoder(
        Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends BB>> decoder) {
      return new AcceptSome<>(frame, methods, decoder, this.encoder);
    }

    private <RR> AcceptNone<C, RR> withEncoder(
        Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super RR>> encoder) {
      return new AcceptNone<>(frame, methods, encoder);
    }
  }

  static final class LazyActionHandler<C, B, R> {
    final Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends B>> decoder;
    final Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super R>> encoder;
    final ActionHandler.Invoker<C, B, R> invoker;

    LazyActionHandler(Function<? super Frame<? super C>, ? extends Decoder<? super C, ? extends B>> decoder,
                      Function<? super Frame<? super C>, ? extends Encoder<? super C, ? super R>> encoder,
                      ActionHandler.Invoker<C, B, R> invoker) {
      this.decoder = decoder;
      this.encoder = encoder;
      this.invoker = invoker;
    }

    HttpHandler handler(Function<? super HttpServerExchange, ? extends C> contextFun, Frame<C> frame) {
      return new ActionHandler<>(decoder.apply(frame), encoder.apply(frame), contextFun, invoker, true);
    }

    @SuppressWarnings("unchecked")
    <CC extends C> LazyActionHandler<CC, B, R> covariant() {
      return (LazyActionHandler<CC, B, R>) this;
    }

  }
}
