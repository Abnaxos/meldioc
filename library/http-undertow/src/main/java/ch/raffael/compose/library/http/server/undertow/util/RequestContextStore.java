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

package ch.raffael.compose.library.http.server.undertow.util;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.util.function.Function;

/**
 * Support for storing a request context in the {@code HttpServerExchange}.
 */
public final class RequestContextStore<C> implements Function<HttpServerExchange, C> {

  private final AttachmentKey<C> key = AttachmentKey.create(Object.class);
  private final Function<? super HttpServerExchange, ? extends C> factory;

  public RequestContextStore(Function<? super HttpServerExchange, ? extends C> factory) {
    this.factory = factory;
  }

  @Override
  public C apply(HttpServerExchange exchange) {
    var ctx = exchange.getAttachment(key);
    if (ctx == null) {
      ctx = factory.apply(exchange);
      exchange.putAttachment(key, ctx);
    }
    return ctx;
  }

  public HttpHandler createHandler(HttpHandler next) {
    return (exchange) -> {
      apply(exchange);
      next.handleRequest(exchange);
    };
  }

}
