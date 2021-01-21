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
  [#import "actions-old.ftl" as a_old]
  [#import "actions.ftl" as a]

  [#function map_tvars params]
    [#return c.tvars(params?map(x -> x.type)?filter(x -> x?starts_with("P")))]
  [/#function]

  [#function map_params variant params]
    [#return (params
    ?filter(x -> x.name?starts_with("p"))
    ?map(x -> "Capture<? extends ${x.type}> ${x.name}") + ["${variant.argType} action"])
    ?join(", ")]
  [/#function]

  [#function map_call_args params]
    [#return params
    ?map(x -> x.name)
    ?map(x -> x?starts_with("p")?then("${x}.get(x)", x))
[#--    ?map(x -> x?starts_with("p")?then("${x}.get(x)", x?starts_with("c")?then("c", x?starts_with("b")?then("b", x))))--]
    ?join(", ")]
  [/#function]
[/#compress]

import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import ch.raffael.meldioc.library.http.server.undertow.handler.EndpointHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.DispatchMode;
import ch.raffael.meldioc.library.http.server.undertow.handler.HttpMethodHandler;
[@a.import_actions/]
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.vavr.collection.Set;

import java.util.function.Function;

/**
 * Builder for HTTP actions.
 */
public class EndpointBuilder<C, B, R> {

  final Frame<C> frame;

  final Set<HttpMethodHandler.Method> methods;
  final Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder;
  final Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder;
  DispatchMode dispatch;

  EndpointBuilder(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
                  Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
                  Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
                  DispatchMode dispatch) {
    this.frame = frame;
    this.methods = methods;
    this.decoder = decoder;
    this.encoder = encoder;
    this.dispatch = dispatch;
  }

  public EndpointBuilder<C, B, R> nonBlocking() {
    dispatch = DispatchMode.NON_BLOCKING;
    return this;
  }

  void conclude(EndpointHandler.Invoker<C, B, R> invoker) {
    methods.forEach(m -> frame.action(m, new LazyActionHandler<>(decoder, encoder, invoker, dispatch)));
  }

  public static final class Empty2Empty<C> extends EndpointBuilder<C, EmptyBody, EmptyBody> {
    Empty2Empty(Frame<C> frame, Set<HttpMethodHandler.Method> methods, DispatchMode dispatch) {
      super(frame, methods, __ -> EmptyBody.decoder(), __ -> EmptyBody.encoder(), dispatch);
    }

    public <BB> Body2Empty<C, BB> acceptWith(HttpDecoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public Body2Empty<C, ? super String> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> Body2Empty<C, T> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    public <RR> Empty2Body<C, RR> produceWith(HttpEncoder<? super C, ? super RR> encoder) {
      return withEncoder(__ -> encoder);
    }

    public Empty2Body<C, CharSequence> producePlainText() {
      return withEncoder(f -> f.enc.plainText());
    }

    public Empty2Body<C, CharSequence> produceHtml() {
      return withEncoder(f -> f.enc.html());
    }

    public Empty2Body<C, Object> produceObject() {
      return withEncoder(f -> f.enc.object(Object.class));
    }

    public <T> Empty2Body<C, T> produce(Class<T> type) {
      return withEncoder(f -> f.enc.object(type));
    }

    @Override
    public Empty2Empty<C> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@a.actions ctx_name="c" body_name="b" body_type="EmptyBody"; variant, params]
      [#if !variant.body && !variant.ret]
        [@c.indent -3]
          public ${map_tvars(params)} void map(${map_params(variant, params)}) {
            conclude((x, c, b) -> {
              action.perform(${map_call_args(params)});
              return EmptyBody.empty();
            });
          }
        [/@c.indent]

      [/#if]
    [/@a.actions]

    private <BB> Body2Empty<C, BB> withDecoder(Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends BB>> decoder) {
      return new Body2Empty<>(frame, methods, decoder, dispatch);
    }

    private <RR> Empty2Body<C, RR> withEncoder(Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super RR>> encoder) {
      return new Empty2Body<>(frame, methods, encoder, dispatch);
    }
  }

  public static final class Empty2Body<C, R> extends EndpointBuilder<C, EmptyBody, R> {

    Empty2Body(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
              Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
              DispatchMode dispatch) {
      super(frame, methods, __ -> EmptyBody.decoder(), encoder, dispatch);
    }

    public <BB> Body2Body<C, BB, R> withDecoder(HttpDecoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public Body2Body<C, ? super String, R> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> Body2Body<C, T, R> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    @Override
    public Empty2Body<C, R> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@a.actions ctx_name="c" body_name="b" body_type="EmptyBody"; variant, params]
      [#if !variant.body && variant.ret]
        [@c.indent -3]
          public ${map_tvars(params)} void map(${map_params(variant, params)}) {
            conclude((x, c, b) ->
              action.perform(${map_call_args(params)}));
          }
        [/@c.indent]

      [/#if]
    [/@a.actions]

    private <BB> Body2Body<C, BB, R> withDecoder(
        Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends BB>> decoder) {
      return new Body2Body<>(frame, methods, decoder, this.encoder, dispatch);
    }
  }

  public static final class Body2Empty<C, B> extends EndpointBuilder<C, B, EmptyBody> {

    Body2Empty(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
              Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
              DispatchMode dispatch) {
      super(frame, methods, decoder, __ -> EmptyBody.encoder(), dispatch);
    }

    public <RR> Body2Body<C, B, RR> produceWith(HttpEncoder<? super C, ? super RR> encoder) {
      return withEncoder(__ -> encoder);
    }

    public Body2Body<C, B, CharSequence> producePlainText() {
      return withEncoder(f -> f.enc.plainText());
    }

    public Body2Body<C, B, CharSequence> produceHtml() {
      return withEncoder(f -> f.enc.html());
    }

    public Body2Body<C, B, Object> produceObject() {
      return withEncoder(f -> f.enc.object(Object.class));
    }

    public <T> Body2Body<C, B, T> produce(Class<T> type) {
      return withEncoder(f -> f.enc.object(type));
    }

    @Override
    public Body2Empty<C, B> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@a.actions ctx_name="c" body_name="b" body_type="B"; variant, params]
      [#if variant.body && !variant.ret]
        [@c.indent -3]
          public ${map_tvars(params)} void map(${map_params(variant, params)}) {
            conclude((x, c, b) -> {
              action.perform(${map_call_args(params)});
              return EmptyBody.empty();
            });
          }
        [/@c.indent]

      [/#if]
    [/@a.actions]

    private <RR> Body2Body<C, B, RR> withEncoder(
        Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super RR>> encoder) {
      return new Body2Body<>(frame, methods, decoder, encoder, dispatch);
    }
  }

  public static final class Body2Body<C, B, R> extends EndpointBuilder<C, B, R> {

    Body2Body(Frame<C> frame, Set<HttpMethodHandler.Method> methods,
              Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
              Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
              DispatchMode dispatch) {
      super(frame, methods, decoder, encoder, dispatch);
    }

    public <BB> Body2Body<C, BB, R> acceptWith(HttpDecoder<? super C, ? extends BB> decoder) {
      return withDecoder(__ -> decoder);
    }

    public Body2Body<C, ? super String, R> acceptPlainText() {
      return withDecoder(f -> f.dec.plainText());
    }

    public <T> Body2Body<C, T, R> accept(Class<T> type) {
      return withDecoder(f -> f.dec.object(type));
    }

    private <RR> Body2Body<C, B, RR> produceWith(Function<Frame<? super C>, ? extends HttpEncoder<? super C, ? super RR>> encoder) {
      return new Body2Body<>(frame, methods, decoder, encoder, dispatch);
    }

    public <RR> Body2Body<C, B, RR> produceWith(HttpEncoder<? super C, ? super RR> encoder) {
      return produceWith(__ -> encoder);
    }

    public Body2Body<C, B, CharSequence> producePlainText() {
      return produceWith(f -> f.enc.plainText());
    }

    public Body2Body<C, B, CharSequence> produceHtml() {
      return produceWith(f -> f.enc.html());
    }

    public Body2Body<C, B, Object> produceObject() {
      return produceWith(f -> f.enc.object(Object.class));
    }

    public <T> Body2Body<C, B, T> produce(Class<T> type) {
      return produceWith(f -> f.enc.object(type));
    }

    @Override
    public Body2Body<C, B, R> nonBlocking() {
      super.nonBlocking();
      return this;
    }

    [@a.actions ctx_name="c" body_name="b" body_type="B"; variant, params]
      [#if variant.body && variant.ret]
        [@c.indent -3]
          public ${map_tvars(params)} void map(${map_params(variant, params)}) {
            conclude((x, c, b) ->
              action.perform(${map_call_args(params)}));
          }
        [/@c.indent]

      [/#if]
    [/@a.actions]

    private <BB> Body2Body<C, BB, R> withDecoder(Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends BB>> decoder) {
      return new Body2Body<>(frame, methods, decoder, this.encoder, dispatch);
    }
  }

  public static final class ActionBuilder {

    public static final class Void {

    }

  }

  static final class LazyActionHandler<C, B, R> {
    final Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder;
    final Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder;
    final EndpointHandler.Invoker<C, B, R> invoker;
    final DispatchMode dispatch;

    LazyActionHandler(Function<? super Frame<? super C>, ? extends HttpDecoder<? super C, ? extends B>> decoder,
                      Function<? super Frame<? super C>, ? extends HttpEncoder<? super C, ? super R>> encoder,
                      EndpointHandler.Invoker<C, B, R> invoker, DispatchMode dispatch) {
      this.decoder = decoder;
      this.encoder = encoder;
      this.invoker = invoker;
      this.dispatch = dispatch;
    }

    HttpHandler handler(Function<? super HttpServerExchange, ? extends C> contextFun, Frame<C> frame) {
      return new EndpointHandler<>(decoder.apply(frame), encoder.apply(frame), contextFun, invoker, dispatch);
    }

    @SuppressWarnings("unchecked")
    <CC extends C> LazyActionHandler<CC, B, R> covariant() {
      return (LazyActionHandler<CC, B, R>) this;
    }
  }
}
