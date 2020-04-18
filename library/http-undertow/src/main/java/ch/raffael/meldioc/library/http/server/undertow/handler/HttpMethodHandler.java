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

package ch.raffael.meldioc.library.http.server.undertow.handler;

import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import io.vavr.collection.Array;
import io.vavr.collection.Map;

import java.util.function.Function;

/**
 * TODO JavaDoc
 */
public class HttpMethodHandler implements HttpHandler {

  private final Map<HttpString, HttpHandler> handlers;

  private HttpMethodHandler(Map<HttpString, HttpHandler> handlers) {
    this.handlers = handlers;
  }

  public static HttpMethodHandler of(Map<Method, HttpHandler> handlers) {
    return new HttpMethodHandler(handlers.mapKeys(Method::httpName));
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var action = handlers.get(exchange.getRequestMethod());
    if (action.isDefined()) {
      action.get().handleRequest(exchange);
    } else {
      new HttpStatusException(StatusCodes.METHOD_NOT_ALLOWED, "Method not allowed").endRequest(exchange);
    }
  }

  public HttpMethodHandler add(Method method, HttpHandler handler) {
    // TODO (2019-07-28) handle duplicates / overrides
    return new HttpMethodHandler(handlers.put(method.httpName(), handler));
  }

  public enum Method {
    GET, POST, PUT, DELETE;

    static Map<HttpString, Method> METHODS = Array.of(values()).toMap(Method::httpName, Function.identity());

    private final HttpString httpName;

    Method() {
      httpName = new HttpString(name());
    }

    public HttpString httpName() {
      return httpName;
    }
  }
}
