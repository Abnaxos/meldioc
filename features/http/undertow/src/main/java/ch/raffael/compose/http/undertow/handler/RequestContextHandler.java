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

package ch.raffael.compose.http.undertow.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.vavr.control.Option;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TODO JavaDoc
 */
public class RequestContextHandler<C> implements HttpHandler {

  private final AttachmentKey<C> key = AttachmentKey.create(Object.class);
  private final Function<? super HttpServerExchange, ? extends C> contextFactory;
  private final HttpHandler next;

  public RequestContextHandler(Function<? super HttpServerExchange, ? extends C> contextFactory, HttpHandler next) {
    this.next = next;
    this.contextFactory = contextFactory;
  }

  public RequestContextHandler(Supplier<? extends C> contextFactory, HttpHandler next) {
    this.next = next;
    this.contextFactory = __ -> contextFactory.get();
  }

  public static <C> WithContextFactoryBuilder<C> withContextFactory(Function<? super HttpServerExchange, ? extends C> contextFactory) {
    return new WithContextFactoryBuilder<>(contextFactory);
  }

  public static <C> WithContextFactoryBuilder<C> withContextFactory(Supplier<? extends C> contextFactory) {
    return new WithContextFactoryBuilder<>(__ -> contextFactory.get());
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.putAttachment(key, contextFactory.apply(exchange)) != null) {
      throw new IllegalStateException("Duplicate context for " + this);
    }
    next.handleRequest(exchange);
  }

  public C context(HttpServerExchange exchange) {
    return Option.of(exchange.getAttachment(key))
        .getOrElseThrow(() -> new IllegalStateException("No context for " + this));
  }

  public static final class WithContextFactoryBuilder<C> {
    private final Function<? super HttpServerExchange, ? extends C> contextFactory;
    private WithContextFactoryBuilder(Function<? super HttpServerExchange, ? extends C> contextFactory) {
      this.contextFactory = contextFactory;
    }
    public RequestContextHandler<C> handler(Function<Function<? super HttpServerExchange, ? extends C>, ? extends HttpHandler> next) {
      return new RequestContextHandler<>(contextFactory, next.apply(contextFactory));
    }
  }

}
