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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import ch.raffael.meldioc.library.codec.ContentType;
import ch.raffael.meldioc.library.codec.ObjectCodecFactory;
import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpObjectCodecFactory;
import ch.raffael.meldioc.library.http.server.undertow.handler.AccessCheckHandler;
import ch.raffael.meldioc.library.http.server.undertow.security.Role;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpMethod;
import io.undertow.server.HttpHandler;
import io.vavr.collection.HashSet;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import java.util.function.Function;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

///> "// vagen ${new Date()}"
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
/// filename RoutingDefinition.java
/// normalize spaces
///
/// = `$RoutingDefinition
///   --> `RoutingDefinition
/// = `@$.Public
///   --> `public
/// ~ `<,\s
///   --> `<
@$.Public abstract class $RoutingDefinition {
  Frame rootFrame;
  Frame currentFrame;

  private final Codecs codec = new Codecs();

  ///<<<
  /// = `$.x()
  ///   --> `this
  protected $RoutingDefinition() {
    currentFrame = new Frame($.x(), new DslTrace(none(), DslTrace.Kind.FRAME), none());
    rootFrame = currentFrame;
  }
  ///>>>

  public static $RoutingDefinition empty() {
    class Empty extends $RoutingDefinition {}
    return new Empty();
  }

  public RoutingBuilder.InitialFragment path() {
    return RoutingBuilder.begin(currentFrame);
  }

  public RoutingBuilder.Fragment0 path(String path) {
    return RoutingBuilder.begin(currentFrame).path(path);
  }

  public QueryCaptureBuilder query(String name) {
    return new QueryCaptureBuilder(name);
  }

  public EndpointBuilder.Method endpoint(HttpMethod... methods) {
    return currentFrame.endpoint(LinkedHashSet.of(methods));
  }

  public EndpointBuilder.Method endpoint(String path, HttpMethod... methods) {
    return currentFrame.endpoint(path, LinkedHashSet.of(methods));
  }

  public EndpointBuilder.Method get() {
    return endpoint(HttpMethod.GET);
  }

  public EndpointBuilder.Method get(String path) {
    return endpoint(path, HttpMethod.GET);
  }

  public EndpointBuilder.Method head() {
    return endpoint(HttpMethod.HEAD);
  }

  public EndpointBuilder.Method head(String path) {
    return endpoint(path, HttpMethod.HEAD);
  }

  public EndpointBuilder.Method post() {
    return endpoint(HttpMethod.POST);
  }

  public EndpointBuilder.Method post(String path) {
    return endpoint(path, HttpMethod.POST);
  }

  public EndpointBuilder.Method put() {
    return endpoint(HttpMethod.PUT);
  }

  public EndpointBuilder.Method put(String path) {
    return endpoint(path, HttpMethod.PUT);
  }

  public EndpointBuilder.Method patch() {
    return endpoint(HttpMethod.PATCH);
  }

  public EndpointBuilder.Method patch(String path) {
    return endpoint(path, HttpMethod.PATCH);
  }

  public EndpointBuilder.Method delete() {
    return endpoint(HttpMethod.DELETE);
  }

  public EndpointBuilder.Method delete(String path) {
    return endpoint(path, HttpMethod.DELETE);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(ContentType contentType, String resource) {
    return ResourceLoader.apply(currentFrame, contentType, StackWalker.getInstance().getCallerClass(), resource);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(ContentType contentType, Class<?> resourceClass, String resource) {
    return ResourceLoader.apply(currentFrame, contentType, resourceClass, resource);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(String contentType, String resource) {
    return ResourceLoader.apply(currentFrame, contentType, StackWalker.getInstance().getCallerClass(), resource);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(String contentType, Class<?> resourceClass, String resource) {
    return ResourceLoader.apply(currentFrame, contentType, resourceClass, resource);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(ContentType contentType, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor) {
    return ResourceLoader.apply(currentFrame,
        contentType, StackWalker.getInstance().getCallerClass(), resource, processor);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(ContentType contentType, Class<?> resourceClass, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor) {
    return ResourceLoader.apply(currentFrame,
        contentType, resourceClass, resource, processor);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(String contentType, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor) {
    return ResourceLoader.apply(currentFrame,
        contentType, StackWalker.getInstance().getCallerClass(), resource, processor);
  }

  public EndpointBuilder<EmptyBody, byte[]> resource(String contentType, Class<?> resourceClass, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor) {
    return ResourceLoader.apply(currentFrame,
        contentType, resourceClass, resource, processor);
  }

  public void handler(Function<? super HttpHandler, ? extends HttpHandler> handler) {
    currentFrame.handler(handler);
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

  public Codecs codec() {
    return codec;
  }

  public void objectCodec(HttpObjectCodecFactory objectCodecFactory) {
    currentFrame.objectCodecFactory = some(objectCodecFactory);
  }

  public void objectCodec(ObjectCodecFactory objectCodecFactory) {
    objectCodec(HttpObjectCodecFactory.wrapBuffered(objectCodecFactory));
  }

  public void merge(RoutingDefinition that) {
    currentFrame.merge(that.rootFrame);
  }

  ///<<</ n: 0..count
  ///
  /// ~ `(Pn )?argN
  ///   --> fwd 1..n collect {"${_1.emptyOr "P$it "}arg$it"} join ', '
  /// ~ `(\? super )?Pn
  ///   --> fwd 1..n collect {"${_1}P$it"} join ', '
  /// = `ActionN
  ///   --> "Action$n"
  /// = `<>
  ///   -->
  /// = `$Actions
  ///   --> `Actions
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
