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

package ch.raffael.meldioc.library.http.server.undertow.handler;

import ch.raffael.meldioc.util.advice.AroundAdvice;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.vavr.control.Option;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static io.vavr.control.Option.some;

/**
 * TODO JavaDoc
 */
public class AdvisedDispatchHandler implements HttpHandler {

  private static final AttachmentKey<AroundAdvice> DISPATCH_ADVICE = AttachmentKey.create(AroundAdvice.class);

  private final HttpHandler next;
  private final Supplier<? extends AroundAdvice> advice;

  public AdvisedDispatchHandler(HttpHandler next, Supplier<? extends AroundAdvice> advice) {
    this.advice = advice;
    this.next = next;
  }

  public static HttpHandler prepend(HttpHandler next, Supplier<? extends AroundAdvice> advice) {
    return prepend(next, some(advice));
  }

  public static HttpHandler prepend(HttpHandler next, Option<? extends Supplier<? extends AroundAdvice>> advice) {
    return advice.<HttpHandler>map(a -> new AdvisedDispatchHandler(next, a)).getOrElse(next);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var advice = this.advice.get();
    exchange.putAttachment(DISPATCH_ADVICE, advice);
    exchange.setDispatchExecutor(standardExecutor(exchange));
    next.handleRequest(exchange);
  }

  public static Executor standardExecutor(HttpServerExchange exchange) {
    return advise(exchange, exchange.getAttachment(DISPATCH_ADVICE), null);
  }

  public static Executor advise(HttpServerExchange exchange, Executor executor) {
    return advise(exchange, exchange.getAttachment(DISPATCH_ADVICE), executor);
  }

  @SuppressWarnings("try")
  private static Executor advise(HttpServerExchange exchange,
      @Nullable AroundAdvice advice, @Nullable Executor executor) {
    Executor x = Objects.requireNonNullElse(executor, r -> exchange.getConnection().getWorker().execute(r));
    if (advice != null) {
      return (r) -> x.execute(() -> {
        try (var __ = advice.before()) {
          r.run();
        }
      });
    } else {
      return x;
    }
  }
}
