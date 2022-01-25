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

package ch.raffael.meldioc.library.http.server.undertow.testlib

import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition
import ch.raffael.meldioc.library.http.server.undertow.util.RequestContexts
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration

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

  def client = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.NEVER)
      .connectTimeout(Duration.ofSeconds(1))
      .build()

  def <T> HttpResponse<T> http(HttpResponse.BodyHandler<T> bodyHandler, @DelegatesTo(RequestBuilder) Closure<?> config) {
    def rq = new RequestBuilder(HttpRequest.newBuilder())
    config = (Closure)config.clone()
    config.resolveStrategy = Closure.OWNER_FIRST
    config.delegate = rq
    config.call(rq)
    client.send rq.build(), bodyHandler
  }

  HttpResponse<String> http(@DelegatesTo(RequestBuilder) Closure<?> config) {
    http(HttpResponse.BodyHandlers.ofString(), config)
  }

  class RequestBuilder implements HttpRequest.Builder {

    private final HttpRequest.Builder delegate

    RequestBuilder(HttpRequest.Builder delegate) {
      this.delegate = delegate
    }

    private URI uri
    private String path
    private Map<String, Object> query = [:]

    @Override
    RequestBuilder uri(URI uri) {
      this.uri = uri
      this
    }

    RequestBuilder path(String path) {
      this.path = path
      this
    }

    RequestBuilder query(Map<String, Object> query) {
      this.query.putAll(query)
      this
    }

    @Override
    RequestBuilder expectContinue(boolean enable) {
      delegate.expectContinue(enable)
      this
    }

    @Override
    RequestBuilder version(HttpClient.Version version) {
      delegate.version(version)
      this
    }

    @Override
    RequestBuilder header(String name, String value) {
      delegate.header(name, value)
      this
    }

    @Override
    RequestBuilder headers(String... headers) {
      delegate.headers(headers)
      this
    }

    RequestBuilder header(String name, Object value) {
      delegate.header(name, value as String)
      this
    }

    RequestBuilder headers(Map<String, Object> headers) {
      headers.each {k, v -> header k, v}
      this
    }

    @Override
    RequestBuilder timeout(Duration duration) {
      delegate.timeout(duration)
      this
    }

    @Override
    RequestBuilder setHeader(String name, String value) {
      delegate.setHeader(name, value)
      this
    }

    RequestBuilder contentType(String contentType) {
      header 'Content-Type', contentType
    }

    @Override
    RequestBuilder GET() {
      delegate.GET()
      this
    }

    @Override
    RequestBuilder POST(HttpRequest.BodyPublisher bodyPublisher) {
      delegate.POST(bodyPublisher)
      this
    }

    RequestBuilder POST(String content, Charset charset = StandardCharsets.UTF_8) {
      POST(HttpRequest.BodyPublishers.ofString(content, charset))
    }

    @Override
    RequestBuilder PUT(HttpRequest.BodyPublisher bodyPublisher) {
      delegate.PUT(bodyPublisher)
      this
    }

    RequestBuilder PUT(String content, Charset charset = StandardCharsets.UTF_8) {
      PUT(HttpRequest.BodyPublishers.ofString(content, charset))
    }

    @Override
    RequestBuilder DELETE() {
      delegate.DELETE()
      this
    }

    RequestBuilder HEAD() {
      method 'HEAD', HttpRequest.BodyPublishers.noBody()
    }

    RequestBuilder OPTIONS() {
      method 'OPTIONS', HttpRequest.BodyPublishers.noBody()
    }

    @Override
    RequestBuilder method(String method, HttpRequest.BodyPublisher bodyPublisher) {
      delegate.method(method, bodyPublisher)
      this
    }

    @Override
    RequestBuilder copy() {
      def c = new RequestBuilder(delegate)
      c.uri = uri
      c.path = path
      c.query = query
      return c
    }

    @Override
    HttpRequest build() {
      if (uri != null) {
        delegate.uri(uri)
      }
      if (path != null) {
        delegate.uri(undertow.uri(query, path))
      }
      delegate.build()
    }
  }
}
