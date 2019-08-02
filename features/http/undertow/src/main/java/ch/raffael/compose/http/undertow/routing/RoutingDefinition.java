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

import ch.raffael.compose.http.undertow.Role;
import ch.raffael.compose.http.undertow.codec.EmptyBody;
import ch.raffael.compose.http.undertow.codec.ObjectCodecFactory;
import ch.raffael.compose.http.undertow.handler.HttpMethodHandler;
import ch.raffael.compose.http.undertow.handler.HttpMethodHandler.Method;
import ch.raffael.compose.util.VavrX;
import io.vavr.API;
import io.vavr.collection.Array;
import io.vavr.collection.Traversable;

import static io.vavr.API.*;


/**
 * Base class for routing definitions. The general pattern to use this is as
 * follows:
 *
 * <pre>
 * new RoutingDefinition() {{
 *   // routing DSL code goes here
 * }}
 * </pre>
 *
 * You can of course use it however you want, the above is just the DSL-like
 * approach.
 */
public abstract class RoutingDefinition<C> {

  Frame<C> rootFrame;
  Frame<C> currentFrame;

  protected RoutingDefinition() {
    currentFrame = new Frame<>(this, None());
    rootFrame = currentFrame;
  }

  public static <C> RoutingDefinition<C> empty() {
    class Empty extends RoutingDefinition<C> {}
    return new Empty();
  }

  public RoutingBuilder<C>.InitialFragment path() {
    return RoutingBuilder.begin(currentFrame);
  }

  public RoutingBuilder<C>.Fragment0 path(String path) {
    return RoutingBuilder.begin(currentFrame).path(path);
  }

  public QueryCaptureBuilder query(String name) {
    return new QueryCaptureBuilder(name);
  }

  public ActionBuilder.AcceptNone<C, EmptyBody> handle(Method... methods) {
    return new ActionBuilder.AcceptNone<>(currentFrame, API.Set(methods), __ -> EmptyBody.encoder());
  }

  public ActionBuilder.AcceptNone<C, EmptyBody> get() {
    return handle(HttpMethodHandler.Method.GET);
  }

  public ActionBuilder.AcceptNone<C, EmptyBody> post() {
    return handle(HttpMethodHandler.Method.POST);
  }

  public ActionBuilder.AcceptNone<C, EmptyBody> put() {
    return handle(HttpMethodHandler.Method.PUT);
  }

  public ActionBuilder.AcceptNone<C, EmptyBody> delete() {
    return handle(HttpMethodHandler.Method.DELETE);
  }

  public void restrict(Traversable<? extends Role> roles) {
    // TODO FIXME (2019-06-29) implement
    roles.distinct().map(Role::name);
  }

  public void restrict(Iterable<? extends Role> roles) {
    restrict(VavrX.traversableOf(roles));
  }

  public void restrict(Role... roles) {
    restrict(Array.of(roles));
  }

  public void objectCodec(ObjectCodecFactory<? super C> objectCodecFactory) {
    currentFrame.objectCodecFactory = Some(objectCodecFactory);
  }

  public void merge(RoutingDefinition<? super C> that) {
    currentFrame.merge(that.rootFrame);
  }

//  public void codec(Encoder encoder, Decoder<?> decoder) {
//
//  }
//
//  public void codec(Decoder<?> decoder) {
//
//  }
//
//  public void codec(Encoder encoder) {
//
//  }
//
//  public <T> Capture<T> body(Class<T> body) {
//    return null;
//  }
//
//  public Value<String> remainingPath() {
//    return null;
//  }


}
