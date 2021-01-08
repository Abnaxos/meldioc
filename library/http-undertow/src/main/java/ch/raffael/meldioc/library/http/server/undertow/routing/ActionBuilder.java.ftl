/*
 *  Copyright (c) 2020 Raffael Herzog
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

package ch.raffael.meldioc.library.http.server.undertow.routing;
[#compress]
  [#import "/parameters.ftl" as p]
  [#import "/codegen.ftl" as c]
  [#import "actions.ftl" as a]
  [#import "actions-impl.ftl" as impl]
[/#compress]

import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import ch.raffael.meldioc.library.http.server.undertow.handler.ActionHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.DispatchMode;
import ch.raffael.meldioc.library.http.server.undertow.handler.HttpMethodHandler;
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
  final Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder;
  final Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder;
  DispatchMode dispatch;

  ActionBuilder(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
                Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
                Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
                DispatchMode dispatch) {
    this.frame = frame;
    this.methods = methods;
    this.decoder = decoder;
    this.encoder = encoder;
    this.dispatch = dispatch;
  }

  public ActionBuilder<C, B, R> nonBlocking() {
    dispatch = DispatchMode.NON_BLOCKING;
    return this;
  }

  void conclude(ActionHandler.Invoker<C, B, R> invoker) {
    methods.forEach(m -> frame.action(m, new LazyActionHandler<>(decoder, encoder, invoker, dispatch)));
  }

  [#list 0..p.pcount as i]

  @FunctionalInterface
  public interface ${a.action_t(i, false, false)} {
    void perform(${c.joinf(1..*i, "P# param#")}) throws Exception;
  }

  @FunctionalInterface
  public interface ${a.action_t(i, false, true)} {
    R perform(${c.joinf(1..*i, "P# param#")}) throws Exception;
  }

  @FunctionalInterface
  public interface ${a.action_t(i, true, false)} {
    void perform(C ctx${c.joinf(1..*i, "P# param#", ", #")}) throws Exception;
  }

  @FunctionalInterface
  public interface ${a.action_t(i, true, true)} {
    R perform(C ctx${c.joinf(1..*i, "P# param#", ", #")}) throws Exception;
  }

  [/#list]

  public static final class None2None<C> extends ActionBuilder<C, EmptyBody, EmptyBody> {
    None2None(Frame<C> frame, Set<HttpMethodHandler.Method> methods, DispatchMode dispatch) {
      super(frame, methods, __ -> EmptyBody.decoder(), __ -> EmptyBody.encoder(), dispatch);
    }

    public <BB> Some2None<C, BB> acceptWith(HttpDecoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public Some2None<C, ? super String> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> Some2None<C, T> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    public <RR> None2Some<C, RR> produceWith(HttpEncoder<? super C, ? super RR> encoder) {
      return withEncoder(__ -> encoder);
    }

    public None2Some<C, CharSequence> producePlainText() {
      return withEncoder(f -> f.enc.plainText());
    }

    public None2Some<C, CharSequence> produceHtml() {
      return withEncoder(f -> f.enc.html());
    }

    public None2Some<C, Object> produceObject() {
      return withEncoder(f -> f.enc.object(Object.class));
    }

    public <T> None2Some<C, T> produce(Class<T> type) {
      return withEncoder(f -> f.enc.object(type));
    }

    @Override
    public None2None<C> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@impl.dispatch_methods false false /]

    private <BB> Some2None<C, BB> withDecoder(Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends BB>> decoder) {
      return new Some2None<>(frame, methods, decoder, dispatch);
    }

    private <RR> None2Some<C, RR> withEncoder(Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super RR>> encoder) {
      return new None2Some<>(frame, methods, encoder, dispatch);
    }
  }

  public static final class None2Some<C, R> extends ActionBuilder<C, EmptyBody, R> {

    None2Some(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
              Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
              DispatchMode dispatch) {
      super(frame, methods, __ -> EmptyBody.decoder(), encoder, dispatch);
    }

    public <BB> Some2Some<C, BB, R> withDecoder(HttpDecoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public Some2Some<C, ? super String, R> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> Some2Some<C, T, R> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    @Override
    public None2Some<C, R> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@impl.dispatch_methods false true /]

    private <BB> Some2Some<C, BB, R> withDecoder(
        Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends BB>> decoder) {
      return new Some2Some<>(frame, methods, decoder, this.encoder, dispatch);
    }
  }

  public static final class Some2None<C, B> extends ActionBuilder<C, B, EmptyBody> {

    Some2None(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
              Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
              DispatchMode dispatch) {
      super(frame, methods, decoder, __ -> EmptyBody.encoder(), dispatch);
    }

    public <RR> Some2Some<C, B, RR> produceWith(HttpEncoder<? super C, ? super RR> encoder) {
      return withEncoder(__ -> encoder);
    }

    public Some2Some<C, B, CharSequence> producePlainText() {
      return withEncoder(f -> f.enc.plainText());
    }

    public Some2Some<C, B, CharSequence> produceHtml() {
      return withEncoder(f -> f.enc.html());
    }

    public Some2Some<C, B, Object> produceObject() {
      return withEncoder(f -> f.enc.object(Object.class));
    }

    public <T> Some2Some<C, B, T> produce(Class<T> type) {
      return withEncoder(f -> f.enc.object(type));
    }

    @Override
    public Some2None<C, B> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@impl.dispatch_methods true false /]

    private <RR> Some2Some<C, B, RR> withEncoder(
        Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super RR>> encoder) {
      return new Some2Some<>(frame, methods, decoder, encoder, dispatch);
    }
  }

  public static final class Some2Some<C, B, R> extends ActionBuilder<C, B, R> {

    Some2Some(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
              Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
              Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
              DispatchMode dispatch) {
      super(frame, methods, decoder, encoder, dispatch);
    }

    public <BB> Some2Some<C, BB, R> acceptWith(HttpDecoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public Some2Some<C, ? super String, R> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> Some2Some<C, T, R> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    private <RR> Some2Some<C, B, RR> produceWith(Function<Frame<? super C>, ? extends HttpEncoder<? super C, ? super RR>> encoder) {
      return new Some2Some<>(frame, methods, decoder, encoder, dispatch);
    }

    public <RR> Some2Some<C, B, RR> produceWith(HttpEncoder<? super C, ? super RR> encoder) {
      return produceWith(__ -> encoder);
    }

    public Some2Some<C, B, CharSequence> producePlainText() {
      return produceWith(f -> f.enc.plainText());
    }

    public Some2Some<C, B, CharSequence> produceHtml() {
      return produceWith(f -> f.enc.html());
    }

    public Some2Some<C, B, Object> produceObject() {
      return produceWith(f -> f.enc.object(Object.class));
    }

    public <T> Some2Some<C, B, T> produce(Class<T> type) {
      return produceWith(f -> f.enc.object(type));
    }

    @Override
    public Some2Some<C, B, R> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@impl.dispatch_methods true true /]

    private <BB> Some2Some<C, BB, R> withDecoder(Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends BB>> decoder) {
      return new Some2Some<>(frame, methods, decoder, this.encoder, dispatch);
    }
  }

  static final class LazyActionHandler<C, B, R> {
    final Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder;
    final Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder;
    final ActionHandler.Invoker<C, B, R> invoker;
    final DispatchMode dispatch;

    LazyActionHandler(Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
                      Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
                      ActionHandler.Invoker<C, B, R> invoker, DispatchMode dispatch) {
      this.decoder = decoder;
      this.encoder = encoder;
      this.invoker = invoker;
      this.dispatch = dispatch;
    }

    HttpHandler handler(Function<? super HttpServerExchange, ? extends C> contextFun, Frame<C> frame) {
      return new ActionHandler<>(decoder.apply(frame), encoder.apply(frame), contextFun, invoker, dispatch);
    }

    @SuppressWarnings("unchecked")
    <CC extends C> LazyActionHandler<CC, B, R> covariant() {
      return (LazyActionHandler<CC, B, R>) this;
    }
  }
}
