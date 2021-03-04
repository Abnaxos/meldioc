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
[/#compress]

import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import ch.raffael.meldioc.library.http.server.undertow.handler.EndpointHandler;
[@a.import_actions/]
import ch.raffael.meldioc.library.http.server.undertow.util.HttpMethod;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatus;
import io.undertow.server.HttpServerExchange;
import io.vavr.collection.Set;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Builder for HTTP actions.
 */
@SuppressWarnings("WeakerAccess")
public class EndpointBuilder<C, B, T> {

  final DslTrace trace;
  final BiConsumer<EndpointBuilder<C, ?, ?>, EndpointBuilder<C, ?, ?>> updateCallback;
  final Set<HttpMethod> methods;
  final BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init;

  EndpointBuilder(DslTrace trace,
                  BiConsumer<EndpointBuilder<C, ?, ?>, EndpointBuilder<C, ?, ?>> updateCallback,
                  Set<HttpMethod> methods,
                  BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
    this.trace = trace;
    this.updateCallback = updateCallback;
    this.methods = methods;
    this.init = init;
  }

  EndpointBuilder(EndpointBuilder<C, ?, ?> prev,
                  BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
    this(prev, prev.trace, prev.methods, init);
  }

  EndpointBuilder(EndpointBuilder<C, ?, ?> prev, Set<HttpMethod> methods,
                  BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
    this(prev, prev.trace, methods, init);
  }

  EndpointBuilder(EndpointBuilder<C, ?, ?> prev, DslTrace trace, Set<HttpMethod> methods,
                  BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
    this.updateCallback = prev.updateCallback;
    this.trace = trace;
    this.methods = methods;
    this.init = init;
    updateCallback.accept(prev, this);
  }

  EndpointHandler<C, B, T> handler(Frame<C> frame, Function<? super HttpServerExchange, ? extends C> context) {
    return init.apply(frame, EndpointHandler.initial()).context(context);
  }

  <BB, RR> BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, BB, RR>> addInit(
      BiFunction<Frame<C>, EndpointHandler<C, B, T>, EndpointHandler<C, BB, RR>> init) {
    return (f, h) -> init.apply(f, this.init.apply(f, h));
  }

  <BB, RR> BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, BB, RR>> addInit(
      Function<EndpointHandler<C, B, T>, EndpointHandler<C, BB, RR>> init) {
    return (f, h) -> init.apply(this.init.apply(f, h));
  }

  <CC extends C> EndpointBuilder<CC, B, T> fork(
      DslTrace trace,
      BiConsumer<EndpointBuilder<CC, ?, ?>, EndpointBuilder<CC, ?, ?>> updateCallback) {
    return new EndpointBuilder<>(trace, updateCallback, methods, this.<CC>contextCovariant().init);
  }

  @SuppressWarnings("unchecked")
  <CC extends C> EndpointBuilder<CC, B, T> contextCovariant() {
    return (EndpointBuilder<CC, B, T>) this;
  }

  public static class Method<C> extends Decoding<C> {
    Method(DslTrace trace, BiConsumer<EndpointBuilder<C, ?, ?>, EndpointBuilder<C, ?, ?>> updateCallback,
           Set<HttpMethod> methods) {
      super(trace, updateCallback, methods);
    }

    Method(EndpointBuilder<C, ?, ?> prev,
           BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, EmptyBody, EmptyBody>> init) {
      super(prev, init);
    }

    Method(EndpointBuilder<C, ?, ?> prev, Set<HttpMethod> methods,
           BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, EmptyBody, EmptyBody>> init) {
      super(prev, methods, init);
    }

    public Method<C> method(HttpMethod... methods) {
      return new Method<>(this, this.methods.addAll(Arrays.asList(methods)), init);
    }

    public Method<C> get() {
      return method(HttpMethod.GET);
    }

    public Method<C> post() {
      return method(HttpMethod.POST);
    }

    public Method<C> put() {
      return method(HttpMethod.PUT);
    }

    public Method<C> patch() {
      return method(HttpMethod.PATCH);
    }

    public Method<C> delete() {
      return method(HttpMethod.DELETE);
    }
  }

  public static class Decoding<C> extends Processing<C, EmptyBody, EmptyBody> {

    Decoding(DslTrace trace, BiConsumer<EndpointBuilder<C, ?, ?>, EndpointBuilder<C, ?, ?>> updateCallback,
             Set<HttpMethod> methods) {
      super(trace, updateCallback, methods, (f, p) -> p);
    }

    Decoding(EndpointBuilder<C, ?, ?> prev,
             BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, EmptyBody, EmptyBody>> init) {
      super(prev, init);
    }

    Decoding(EndpointBuilder<C, ?, ?> prev, Set<HttpMethod> methods,
           BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, EmptyBody, EmptyBody>> init) {
      super(prev, methods, init);
    }

    public <T> Processing<C, T, T> accept(Codecs.DecoderSupplier<C, ? extends T> decoder) {
      return new Processing<>(this, addInit((f, h) -> h.decoder(decoder.decoder(f))));
    }

    public <T> Processing<C, T, T> accept(HttpDecoder<? super C, ? extends T> decoder) {
      return new Processing<>(this, addInit(h -> h.decoder(decoder)));
    }

    public <T> Processing<C, T, T> accept(Class<T> type) {
      return new Processing<>(this, addInit((f, h) -> h.decoder(f.dec.object(type))));
    }
  }

  public static class Processing<C, B, T> extends Response<C, B, T> {

    Processing(DslTrace trace, BiConsumer<EndpointBuilder<C, ?, ?>, EndpointBuilder<C, ?, ?>> updateCallback,
               Set<HttpMethod> methods, BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>,
        EndpointHandler<C, B, T>> init) {
      super(trace, updateCallback, methods, init);
    }

    Processing(EndpointBuilder<C, ?, ?> prev,
               BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
      super(prev, init);
    }

    Processing(EndpointBuilder<C, ?, ?> prev, Set<HttpMethod> methods,
               BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
      super(prev, methods, init);
    }

    [#macro mapping variant name predef=[] pre_args=[] void="" builder="Processing<C, B, #>" result="result" exception="" prev="this"]
      [#local v = variant]
      [#if void?split("=")?size == 2]
        [#local void_type=void?split("=")[0]?trim]
        [#local void_value=void?split("=")[1]?trim]
      [#else ]
        [#local void_type="???"]
        [#local void_value="???"]
      [/#if]
      [#local recovery = exception?has_content]
      [#local builder_inferred = builder?replace("<.*>", "<>", "r")]
      [#local pipe = builder?starts_with("Pipe<")]
      [#local call]
        [@c.squash]
          action.perform(${(predef+v.vararg_params?map(x -> "${x.name}.get(s.exchange())"))?join(", ")})
        [/@c.squash]
      [/#local]
      [@c.indent -4]
        public [@c.squash]
          ${c.tvars((recovery?then(["X extends Throwable"], v.void?then([], ["U"]))+v.vararg_params?map(x -> x.type)))}
          ${builder?replace("#", v.void?then(void_type, "U"))}
          ${name}(
        [/@c.squash]
            ${(pre_args+v.vararg_params?map(x -> "Capture<? extends ${x.type}> ${x.name}")+["${v.arg_type} action"])?join(", ")}) {
          [@c.indent -2]
            [#if !pipe]
              return new ${builder_inferred}(${prev}, addInit(h -> h.processor(s -> {
                [#if recovery]
                if (!(s.isException() && ${exception}.isInstance(s.exception()))) return s;
                [#else]
                if (s.isException()) return s.promoteException();
                [/#if]
                [#if v.void]
                ${call};
                return s.${recovery?then("recover", "value")}(${void_value});
                [#else]
                var result = ${call};
                return s.${recovery?then("recover", "value")}(${result});
                [/#if]
              })));
            [#else]
              return new ${builder_inferred}(s ->
                  ${call});
            [/#if]
          [/@c.indent]
        }
      [/@c.indent]

    [/#macro]

    [@c.indent 2]
      [@a.actions ret_var="U" void=false; v]
        [@mapping variant=v name="map" /]
      [/@a.actions]
      [@a.actions predef=["T b"] ret_var="U" void=false; v]
        [@mapping variant=v name="map" predef=["s.value()"] /]
      [/@a.actions]
      [@a.actions ret_var="U" nonvoid=false; v]
        [@mapping variant=v name="tap" predef=[] void="T=s.value()" /]
      [/@a.actions]
      [@a.actions predef=["T b"] ret_var="U" nonvoid=false; v]
        [@mapping variant=v name="tap" predef=["s.value()"] void="T=s.value()" /]
      [/@a.actions]
      [@a.actions ret_var="U" nonvoid=false; v]
        [@mapping variant=v name="consume" void="EmptyBody=EmptyBody.instance()" /]
      [/@a.actions]
      [@a.actions predef=["T b"] ret_var="U" nonvoid=false; v]
        [@mapping variant=v name="consume" predef=["s.value()"] void="EmptyBody=EmptyBody.instance()" /]
      [/@a.actions]
      [@a.actions ret_var="T" void=false; v]
        [@mapping variant=v name="recover" pre_args=["Class<X> excType"] builder="Processing<C, B, T>" exception="excType" /]
      [/@a.actions]
      [@a.actions predef=["X exc"] ret_var="T" void=false; v]
        [@mapping variant=v name="recover" predef=["excType.cast(s.exception())"] pre_args=["Class<X> excType"] builder="Processing<C, B, T>" exception="excType" /]
      [/@a.actions]
      [@a.actions ret_var="U" void=false; v]
        [@mapping variant=v name="pipe" predef=[] builder="Pipe<#>"/]
      [/@a.actions]
    [/@c.indent]

    public final class Pipe<V> {

      private final Action1<EndpointHandler.State<? extends T>, ? extends V> pipeFun;

      Pipe(Action1<EndpointHandler.State<? extends T>, ? extends V> pipeFun) {
        this.pipeFun = pipeFun;
      }

      [@c.indent 3]
        [@a.actions ret_var="U" predef=["V v"] void=false; v]
          [@mapping variant=v name="map" predef=["pipeFun.perform(s)"] prev="Processing.this" /]
        [/@a.actions]
        [@a.actions predef=["V v", "T b"] ret_var="U" void=false; v]
          [@mapping variant=v name="map" predef=["pipeFun.perform(s)", "s.value()"] prev="Processing.this" /]
        [/@a.actions]
        [@a.actions  predef=["V v"]ret_var="U" nonvoid=false; v]
          [@mapping variant=v name="tap" predef=["pipeFun.perform(s)"] void="T=s.value()" prev="Processing.this" /]
        [/@a.actions]
        [@a.actions predef=["V v", "T b"] ret_var="U" nonvoid=false; v]
          [@mapping variant=v name="tap" predef=["pipeFun.perform(s)", "s.value()"] void="T=s.value()" prev="Processing.this" /]
        [/@a.actions]
        [@a.actions  predef=["V v"]ret_var="U" nonvoid=false; v]
          [@mapping variant=v name="consume" predef=["pipeFun.perform(s)"] void="EmptyBody=EmptyBody.instance()" prev="Processing.this" /]
        [/@a.actions]
        [@a.actions predef=["V v", "T b"] ret_var="U" nonvoid=false; v]
          [@mapping variant=v name="consume" predef=["pipeFun.perform(s)", "s.value()"] void="EmptyBody=EmptyBody.instance()" prev="Processing.this" /]
        [/@a.actions]
        [@a.actions ret_var="T" predef=["V v"] void=false; v]
          [@mapping variant=v name="recover" predef=["pipeFun.perform(s)"] pre_args=["Class<X> excType"] builder="Processing<C, B, T>" exception="excType" prev="Processing.this" /]
        [/@a.actions]
        [@a.actions predef=["V v", "X exc"] ret_var="T" void=false; v]
          [@mapping variant=v name="recover" predef=["pipeFun.perform(s)", "excType.cast(s.exception())"] pre_args=["Class<X> excType"] builder="Processing<C, B, T>" exception="excType" prev="Processing.this" /]
        [/@a.actions]
        [@a.actions ret_var="U" predef=["V v"] void=false; v]
          [@mapping variant=v name="pipe" predef=["pipeFun.perform(s)"] builder="Pipe<#>" /]
        [/@a.actions]
        [@a.actions ret_var="U" predef=["V v", "T b"] void=false; v]
          [@mapping variant=v name="pipe" predef=["pipeFun.perform(s)", "s.value()"] builder="Pipe<#>" /]
        [/@a.actions]
      [/@c.indent]
    }
  }

  public static class Response<C, B, T> extends EndpointBuilder<C, B, T> {

    Response(DslTrace trace, BiConsumer<EndpointBuilder<C, ?, ?>, EndpointBuilder<C, ?, ?>> updateCallback,
             Set<HttpMethod> methods, BiFunction<Frame<C>,
        EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
      super(trace, updateCallback, methods, init);
    }

    Response(EndpointBuilder<C, ?, ?> prev,
             BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
      super(prev, init);
    }

    Response(EndpointBuilder<C, ?, ?> prev, Set<HttpMethod> methods,
             BiFunction<Frame<C>, EndpointHandler<C, EmptyBody, EmptyBody>, EndpointHandler<C, B, T>> init) {
      super(prev, methods, init);
    }

    public EndpointBuilder<C, B, T> respond() {
      return respond(HttpStatus.OK);
    }

    public EndpointBuilder<C, B, T> respond(HttpStatus status) {
      return respond(status, Object.class);
    }

    public EndpointBuilder<C, B, T> respond(Class<? super T> type) {
      return respond(HttpStatus.OK, type);
    }

    public EndpointBuilder<C, B, T> respond(HttpStatus status, Class<? super T> type) {
      return new EndpointBuilder<>(this, addInit((f, h) -> h.defaultStatus(status).encoder(f.enc.object(type))));
    }

    public EndpointBuilder<C, B, T> respond(Codecs.EncoderSupplier<C, ? super T> encoder) {
      return respond(HttpStatus.OK, encoder);
    }

    public EndpointBuilder<C, B, T> respond(HttpStatus status, Codecs.EncoderSupplier<C, ? super T> encoder) {
      return new EndpointBuilder<>(this, addInit((f, h) -> h.defaultStatus(status).encoder(encoder.encoder(f))));
    }

    public EndpointBuilder<C, B, T> respond(HttpEncoder<? super C, ? super T> encoder) {
      return respond(HttpStatus.OK, encoder);
    }

    public EndpointBuilder<C, B, T> respond(HttpStatus status, HttpEncoder<? super C, ? super T> encoder) {
      return new EndpointBuilder<>(this, addInit(h -> h.defaultStatus(status).encoder(encoder)));
    }

    public EndpointBuilder<C , B, EmptyBody> respondEmpty() {
      return respondEmpty(HttpStatus.OK);
    }

    public EndpointBuilder<C , B, EmptyBody> respondEmpty(HttpStatus status) {
      return new EndpointBuilder<>(this,
          this.addInit(h ->
              h.processor(s -> s.map(__ -> EmptyBody.instance()))
                  .defaultStatus(status)
                  .encoder(EmptyBody.encoder())));
    }
  }
}
