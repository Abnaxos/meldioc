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
import io.vavr.collection.Set;

/**
 * TODO JavaDoc
 */
public class ActionBuilder<C, B, R> {

  final Frame<C> frame;

  final Set<HttpMethodHandler.Method> methods;
  final Decoder<? super C, ? extends B> decoder;
  final Encoder<? super C, ? super R> encoder;

  ActionBuilder(Frame<C> frame, Set<HttpMethodHandler.Method> methods, Decoder<? super C, ? extends B> decoder, Encoder<? super C, ? super R> encoder) {
    this.frame = frame;
    this.methods = methods;
    this.decoder = decoder;
    this.encoder = encoder;
  }

  void conclude(ActionHandler.Invoker<? super C, ? super B, ? extends R> invoker) {
    methods.forEach(m -> frame.action(m, (c) -> new ActionHandler<>(decoder, encoder, c, invoker)));
  }

  @FunctionalInterface
  public interface Action0<C, R> {
    R perform(C ctx) throws Exception;
  }

  @FunctionalInterface
  public interface Action1<C, P1, R> {
    R perform(C ctx, P1 arg1);
  }

  @FunctionalInterface
  public interface Action2<C, P1, P2, R> {
    R perform(C ctx, P1 arg1, P2 arg2);
  }

  @FunctionalInterface
  public interface Action3<C, P1, P2, P3, R> {
    R perform(C ctx, P1 arg1, P2 arg2, P3 arg3);
  }

  public static final class AcceptSome<C, B, R> extends ActionBuilder<C, B, R> {

    AcceptSome(Frame<C> frame, Set<HttpMethodHandler.Method> methods, Decoder<? super C, ? extends B> decoder, Encoder<? super C, ? super R> encoder) {
      super(frame, methods, decoder, encoder);
    }

    public <BB> AcceptSome<C, BB, R> acceptWith(Decoder<? super C, ? extends BB> decoder) {
      return new AcceptSome<>(frame, methods, decoder, this.encoder);
    }

    public AcceptSome<C, CharSequence, R> acceptPlainText() {
      return acceptWith(frame.dec.plainText());
    }

    public <T> AcceptSome<C, T, R> accept(Class<T> type) {
      return acceptWith(frame.dec.object(type));
    }

    public <RR> AcceptSome<C, B, RR> produceWith(Encoder<? super C, ? super RR> encoder) {
      return new AcceptSome<>(frame, methods, decoder, encoder);
    }

    public AcceptSome<C, B, CharSequence> producePlainText() {
      return produceWith(frame.enc.plainText());
    }

    public AcceptSome<C, B, CharSequence> produceHtml() {
      return produceWith(frame.enc.html());
    }

    public AcceptSome<C, B, Object> produceObject() {
      return produceWith(frame.enc.object(Object.class));
    }

    public <T> AcceptSome<C, B, T> produce(Class<T> type) {
      return produceWith(frame.enc.object(type));
    }

    public void with(Action1<? super C, ? super B, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, b));
    }

    public <P1> void with(Capture<P1> p1, Action2<? super C, ? super B, ? super P1, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, b, p1.get(x)));
    }

    public <P1, P2> void with(Capture<P1> p1, Capture<P2> p2, Action3<? super C, ? super B, ? super P1, ? super P2, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, b, p1.get(x), p2.get(x)));
    }

  }

  public static final class AcceptNone<C, R> extends ActionBuilder<C, EmptyBody, R> {

    AcceptNone(Frame<C> frame, Set<HttpMethodHandler.Method> methods, Encoder<? super C, ? super R> encoder) {
      super(frame, methods, EmptyBody.decoder(), encoder);
    }

    public <BB> AcceptSome<C, BB, R> acceptWith(Decoder<? super C, ? extends BB> decoder) {
      return new AcceptSome<>(frame, methods, decoder, this.encoder);
    }

    public AcceptSome<C, CharSequence, R> acceptPlainText() {
      return acceptWith(frame.dec.plainText());
    }

    public <T> AcceptSome<C, T, R> accept(Class<T> type) {
      return acceptWith(frame.dec.object(type));
    }

    public <RR> AcceptNone<C, RR> produceWith(Encoder<? super C, ? super RR> encoder) {
      return new AcceptNone<>(frame, methods, encoder);
    }

    public AcceptNone<C, CharSequence> producePlainText() {
      return produceWith(frame.enc.plainText());
    }

    public AcceptNone<C, CharSequence> produceHtml() {
      return produceWith(frame.enc.html());
    }

    public AcceptNone<C, Object> produceObject() {
      return produceWith(frame.enc.object(Object.class));
    }

    public <T> AcceptNone<C, T> produce(Class<T> type) {
      return produceWith(frame.enc.object(type));
    }

    public void with(Action0<? super C, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c));
    }

    public <P1> void with(Capture<P1> p1, Action1<? super C, ? super P1, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, p1.get(x)));
    }

    public <P1, P2> void with(Capture<P1> p1, Capture<P2> p2, Action2<? super C, ? super P1, ? super P2, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, p1.get(x), p2.get(x)));
    }

    public <P1, P2, P3> void with(Capture<P1> p1, Capture<P2> p2, Capture<P3> p3, Action3<? super C, ? super P1, ? super P2, ? super P3, ? extends R> action) {
      conclude((x, c, b) -> action.perform(c, p1.get(x), p2.get(x), p3.get(x)));
    }

  }

}
