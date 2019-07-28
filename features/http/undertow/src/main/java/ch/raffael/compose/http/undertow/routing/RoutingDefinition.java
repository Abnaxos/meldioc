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
import ch.raffael.compose.http.undertow.handler.HttpMethodHandler;
import ch.raffael.compose.http.undertow.handler.HttpMethodHandler.Method;
import ch.raffael.compose.util.VavrX;
import io.undertow.server.HttpHandler;
import io.vavr.API;
import io.vavr.collection.Array;
import io.vavr.collection.Traversable;

import static io.vavr.API.*;


/**
 * TODO JavaDoc
 */
public abstract class RoutingDefinition<C> {

  final Frame<C> root;
  Frame<C> currentFrame;

  protected RoutingDefinition() {
    currentFrame = new Frame<>(this, None());
    root = currentFrame;
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
    return new ActionBuilder.AcceptNone<>(currentFrame, API.Set(methods), EmptyBody.encoder());
  }

  public ActionBuilder.AcceptNone<C, EmptyBody> get() {
    return handle(HttpMethodHandler.Method.GET);
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

  public static HttpHandler createHandlerTree(RoutingDefinition routingDefinition) {
    if (routingDefinition.currentFrame.parent.isDefined()) {
      throw new IllegalStateException("Routing definition is not at top frame");
    }
    return routingDefinition.currentFrame.handler();
  }

//  public void mount(RoutingDefinition<? super C> routingDefinition) {
//
//  }
//
//  public void mount(String path, RoutingDefinition<? super C> routingDefinition) {
//
//  }
//
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
//  public <T> Value<T> body(Class<T> body) {
//    // TODO FIXME (2019-06-29) implement
//    return null;
//  }
//
//  public Value<String> remainingPath() {
//    // TODO FIXME (2019-06-29) implement
//    return null;
//  }


}
