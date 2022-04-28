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

package ch.raffael.meldioc.library.http.server.undertow.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.vavr.collection.Array;
import io.vavr.collection.Map;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.util.function.Function;

/**
 * TODO JavaDoc
 */
public enum HttpMethod {
  GET, HEAD, POST, PUT, PATCH, DELETE,
  OPTIONS;

  private static final Map<HttpString, HttpMethod> METHODS =
      Array.of(values()).toMap(HttpMethod::httpName, Function.identity());
  private static final Map<String, HttpMethod> METHODS_STRINGS =
      METHODS.mapKeys(HttpString::toString);

  private final HttpString httpName;

  HttpMethod() {
    httpName = new HttpString(name());
  }

  public HttpString httpName() {
    return httpName;
  }

  public static Option<HttpMethod> forName(HttpString name) {
    return METHODS.get(name);
  }

  public static Option<HttpMethod> forName(String name) {
    return METHODS_STRINGS.get(name);
  }

  public boolean isUserImplementable() {
    return ordinal() <= DELETE.ordinal();
  }

  public HttpMethod checkUserImplementable() throws IllegalArgumentException {
    if (!isUserImplementable()) {
      throw new IllegalStateException("Method  not user implementable: " + this);
    }
    return this;
  }

  public static boolean isOptionsRequest(HttpServerExchange exchange) {
    return Methods.OPTIONS.equals(exchange.getRequestMethod());
  }

  public static void optionsResponse(HttpServerExchange exchange, Traversable<String> methods) {
    addAllowHeader(exchange, methods);
    exchange.getResponseHeaders().add(Headers.CONTENT_LENGTH, 0);
    HttpStatus.NO_CONTENT.apply(exchange);
    exchange.endExchange();
  }

  public static void methodNotAllowedResponse(HttpServerExchange exchange, Traversable<String> methods) {
    new HttpStatusException(HttpStatus.METHOD_NOT_ALLOWED).endRequest(addAllowHeader(exchange, methods));
  }

  private static HttpServerExchange addAllowHeader(HttpServerExchange exchange, Traversable<String> methods) {
    exchange.getResponseHeaders().add(Headers.ALLOW, methods.toLinkedSet().add(Methods.OPTIONS_STRING).mkString(","));
    return exchange;
  }
}
