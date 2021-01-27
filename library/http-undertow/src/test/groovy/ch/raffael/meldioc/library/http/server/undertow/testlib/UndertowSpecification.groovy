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

package ch.raffael.meldioc.library.http.server.undertow.testlib


import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition
import ch.raffael.meldioc.library.http.server.undertow.util.RequestContexts
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

abstract class UndertowSpecification extends Specification {

  private static final ClassValue<Class<? extends RoutingDefinition<RequestContexts.Empty>>> ROUTING_DEFINITION_CLASS =
      new ClassValue<Class<? extends RoutingDefinition<RequestContexts.Empty>>>() {
        @Override
        protected Class<? extends RoutingDefinition<RequestContexts.Empty>> computeValue(Class<?> type) {
          def a = type.getAnnotation(Routing)
          if (a) {
            return a.value()
          }
          def n = "${type.name}Routing"
          try {
            return Class.forName(n, false, type.classLoader) as Class<? extends RoutingDefinition<RequestContexts.Empty>>
          } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Routing class not found: $n", e)
          }
        }
      }

  @Shared
  @AutoCleanup
  def undertow = new UndertowTestServer(ROUTING_DEFINITION_CLASS.get(getClass()))
  @Shared
  def httpClient = new RESTClient(undertow.url("/"))

  def get(Map<String, ?> args) {
    return httpClient.get(args) as HttpResponseDecorator
  }

  def post(Map<String, ?> args) {
    return httpClient.post(args) as HttpResponseDecorator
  }

  def put(Map<String, ?> args) {
    return httpClient.put(args) as HttpResponseDecorator
  }

  def patch(Map<String, ?> args) {
    return httpClient.patch(args) as HttpResponseDecorator
  }

  def delete(Map<String, ?> args) {
    return httpClient.delete(args) as HttpResponseDecorator
  }

  def head(Map<String, ?> args) {
    return httpClient.head(args) as HttpResponseDecorator
  }

  def options(Map<String, ?> args) {
    return httpClient.options(args) as HttpResponseDecorator
  }
}
