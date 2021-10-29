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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import ch.raffael.meldioc.library.codec.ObjectCodecFactory;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpObjectCodecFactory;
import ch.raffael.meldioc.library.http.server.undertow.handler.AccessCheckHandler;
import ch.raffael.meldioc.library.http.server.undertow.security.Role;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpMethod;
import io.vavr.collection.HashSet;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import java.util.function.Function;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

///> ! "// dslgen ${new Date()}"
/// filename RoutingDefinition.java
/// normalize spaces
///
/// = $RoutingDefinition
///   --> RoutingDefinition
/// ---
/// ~ <,\s
///   --> <
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
public abstract class $RoutingDefinition<C> {
  ///<<< false
  private static <T> T $() {throw new RuntimeException();}
  ///>>>

  Frame<C> rootFrame;
  Frame<C> currentFrame;

  private final Codecs<C> codec = new Codecs<>();

  ///<<<
  /// = $()
  ///   --> this
  protected $RoutingDefinition() {
    currentFrame = new Frame<>($(), new DslTrace(none(), DslTrace.Kind.FRAME), none());
    rootFrame = currentFrame;
  }
  ///>>>

  public static <C> $RoutingDefinition<C> empty() {
    class Empty extends $RoutingDefinition<C> {}
    return new Empty();
  }

  public Capture<C> ctx() {
    return currentFrame.requestContext;
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

  public EndpointBuilder.Method<C> endpoint(HttpMethod... methods) {
    return currentFrame.endpoint(LinkedHashSet.of(methods));
  }

  public EndpointBuilder.Method<C> endpoint(String path, HttpMethod... methods) {
    return currentFrame.endpoint(path, LinkedHashSet.of(methods));
  }

  public EndpointBuilder.Method<C> get() {
    return endpoint(HttpMethod.GET);
  }

  public EndpointBuilder.Method<C> get(String path) {
    return endpoint(path, HttpMethod.GET);
  }

  public EndpointBuilder.Method<C> head() {
    return endpoint(HttpMethod.HEAD);
  }

  public EndpointBuilder.Method<C> head(String path) {
    return endpoint(path, HttpMethod.HEAD);
  }

  public EndpointBuilder.Method<C> post() {
    return endpoint(HttpMethod.POST);
  }

  public EndpointBuilder.Method<C> post(String path) {
    return endpoint(path, HttpMethod.POST);
  }

  public EndpointBuilder.Method<C> put() {
    return endpoint(HttpMethod.PUT);
  }

  public EndpointBuilder.Method<C> put(String path) {
    return endpoint(path, HttpMethod.PUT);
  }

  public EndpointBuilder.Method<C> patch() {
    return endpoint(HttpMethod.PATCH);
  }

  public EndpointBuilder.Method<C> patch(String path) {
    return endpoint(path, HttpMethod.PATCH);
  }

  public EndpointBuilder.Method<C> delete() {
    return endpoint(HttpMethod.DELETE);
  }

  public EndpointBuilder.Method<C> delete(String path) {
    return endpoint(path, HttpMethod.DELETE);
  }

  public void restrict(AccessCheckHandler.AccessRestriction value) {
    currentFrame.restriction = some(value);
  }

  public <R extends Role> void restrict(Function<? super String, ? extends Option<? extends R>> mapper,
                                        Set<? extends R> roles) {
    restrict(AccessCheckHandler.accessByRole(mapper, roles));
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final <R extends Role> void restrict(Function<? super String, ? extends Option<? extends R>> mapper,
                                              R... roles) {
    restrict(AccessCheckHandler.accessByRole(mapper, HashSet.of(roles)));
  }

  public <R extends Enum<?> & Role> void restrict(Class<R> roleEnum, Set<? extends R> roles) {
    restrict(AccessCheckHandler.accessByRole(roleEnum, roles));
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final <R extends Enum<?> & Role> void restrict(Class<R> roleEnum, R... roles) {
    restrict(AccessCheckHandler.accessByRole(roleEnum, HashSet.of(roles)));
  }

  public Codecs<C> codec() {
    return codec;
  }

  public void objectCodec(HttpObjectCodecFactory<? super C> objectCodecFactory) {
    currentFrame.objectCodecFactory = some(objectCodecFactory);
  }

  public void objectCodec(ObjectCodecFactory objectCodecFactory) {
    objectCodec(HttpObjectCodecFactory.wrapBuffered(objectCodecFactory));
  }

  public void merge(RoutingDefinition<? super C> that) {
    currentFrame.merge(that.rootFrame);
  }

  ///<<</ n: 0..count
  ///
  /// ~ (Pn )?argN
  ///   --> ! fwd 1..n collect {"${_1.emptyOr "P$it "}arg$it"} join ', '
  /// ~ (\? super )?Pn
  ///   --> ! fwd 1..n collect {"${_1}P$it"} join ', '
  /// ===
  /// = ActionN
  ///   --> ! "Action$n"
  /// ===
  /// = <>
  ///   -->
  /// ===
  /// = $Actions
  ///   --> Actions
  /// ===
  /// = $of
  ///   --> of
  @SuppressWarnings("overloads")
  public static <Pn, R> $Actions.ActionN<Pn, R> action($Actions.ActionN<? super Pn, ? extends R> action) {
    return $Actions.of(action);
  }

  @SuppressWarnings("overloads")
  public static <Pn> $Actions.ActionNVoid<Pn> action($Actions.ActionNVoid<? super Pn> action) {
    return $Actions.of(action);
  }

  public static <Pn, R> $Actions.ActionN<Pn, R> action(R returnValue, $Actions.ActionNVoid<? super Pn> action) {
    return $Actions.of(returnValue, action);
  }
  ///>>>
}
