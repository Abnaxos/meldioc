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

package ch.raffael.compose.library.http.server.undertow.handler;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * A simple logging handler
 */
public class RequestLoggingHandler implements HttpHandler {


  public static final String STANDARD_REQUEST_ID_MDC_KEY = "http-request-id";

  private final AttachmentKey<Info> startNanosKey = AttachmentKey.create(Info.class);
  private final AtomicInteger counter;
  private final Level level;
  private final Logger logger;
  private final HttpHandler next;
  @Nullable
  private final String mdcKey;
  @Nullable
  private final Marker marker;
  @Nullable
  private final BiConsumer<? super HttpServerExchange, ? super Integer> idConsumer;

  public RequestLoggingHandler(Level level, Logger logger, HttpHandler next) {
    this(level, logger, next, null, null, null, new AtomicInteger());
  }

  private RequestLoggingHandler(Level level, Logger logger, HttpHandler next,
                                @Nullable String mdcKey, @Nullable Marker marker,
                                @Nullable BiConsumer<? super HttpServerExchange, ? super Integer> idConsumer,
                                AtomicInteger counter) {
    this.level = level;
    this.logger = logger;
    this.next = next;
    this.mdcKey = mdcKey;
    this.marker = marker;
    this.idConsumer = idConsumer;
    this.counter = counter;
  }

  public static RequestLoggingHandler trace(Logger logger, HttpHandler next) {
    return new RequestLoggingHandler(Level.TRACE, logger, next);
  }

  public static RequestLoggingHandler debug(Logger logger, HttpHandler next) {
    return new RequestLoggingHandler(Level.DEBUG, logger, next);
  }

  public static RequestLoggingHandler info(Logger logger, HttpHandler next) {
    return new RequestLoggingHandler(Level.INFO, logger, next);
  }

  public RequestLoggingHandler withStandardMdcKey() {
    return withMdcKey(STANDARD_REQUEST_ID_MDC_KEY);
  }

  public RequestLoggingHandler withMdcKey(String mdcKey) {
    return new RequestLoggingHandler(level, logger, next, mdcKey, marker, idConsumer, counter);
  }

  public RequestLoggingHandler withMarker(Marker marker) {
    return new RequestLoggingHandler(level, logger, next, mdcKey, marker, idConsumer, counter);
  }

  public RequestLoggingHandler addIdConsumer(BiConsumer<? super HttpServerExchange, ? super Integer> idConsumer) {
    BiConsumer<? super HttpServerExchange, ? super Integer> newConsumer;
    if (this.idConsumer != null) {
      var prev = this.idConsumer;
      newConsumer = (ex, id) -> {
        prev.accept(ex, id);
        idConsumer.accept(ex, id);
      };
    } else {
      newConsumer = idConsumer;
    }
    return new RequestLoggingHandler(level, logger, next, mdcKey, marker, newConsumer, counter);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var info = new Info(System.nanoTime(), counter.getAndIncrement(), exchange);
    exchange.putAttachment(startNanosKey, info);
    if (mdcKey != null) {
      MDC.put(mdcKey, String.valueOf(info.id));
    }
    level.logBegin(logger, marker, info);
    exchange.addExchangeCompleteListener(this::exchangeComplete);
    if (idConsumer != null) {
      idConsumer.accept(exchange, info.id);
    }
    next.handleRequest(exchange);
  }

  private void exchangeComplete(HttpServerExchange exchange, ExchangeCompletionListener.NextListener next) {
    try {
      Info info = exchange.getAttachment(startNanosKey);
      if (info != null) {
        level.logEnd(logger, marker, info, exchange);
      }
    } finally {
      if (mdcKey != null) {
        MDC.remove(mdcKey);
      }
    }
    next.proceed();
  }

  public enum Level {
    TRACE {
      @Override
      void log(Logger logger, @Nullable Marker marker, String msg) {
        logger.trace(marker, msg);
      }
      @Override
      boolean enabled(Logger logger, @Nullable Marker marker) {
        return logger.isTraceEnabled(marker);
      }
    },
    DEBUG {
      @Override
      void log(Logger logger, @Nullable Marker marker, String msg) {
        logger.debug(marker, msg);
      }
      @Override
      boolean enabled(Logger logger, @Nullable Marker marker) {
        return logger.isDebugEnabled(marker);
      }
    },
    INFO {
      @Override
      void log(Logger logger, @Nullable Marker marker, String msg) {
        logger.info(marker, msg);
      }
      @Override
      boolean enabled(Logger logger, @Nullable Marker marker) {
        return logger.isInfoEnabled(marker);
      }
    };

    void logBegin(Logger logger, @Nullable Marker marker, Info info) {
      if (enabled(logger, marker)) {
        log(logger, marker, format(">>HTTP #{}: {} {}", info.id, info.method, info.uri));
      }
    }

    void logEnd(Logger logger, @Nullable Marker marker, Info info, HttpServerExchange exchange) {
      if (enabled(logger, marker)) {
        var duration = Duration.ofNanos(System.nanoTime() - info.startTime);
        log(logger, marker, format("<<HTTP #{}: {}: {} {} ({})",
            info.id, exchange.getStatusCode(), info.method, info.uri, duration));
      }
    }

    String format(String msg, Object... args) {
      return MessageFormatter.arrayFormat(msg, args).getMessage();
    }

    abstract void log(Logger logger, @Nullable Marker marker, String msg);

    abstract boolean enabled(Logger logger, @Nullable Marker marker);
  }

  private static final class Info {
    final long startTime;
    final int id;
    final HttpString method;
    final String uri;

    private Info(long startTime, int id, HttpServerExchange exchange) {
      this.startTime = startTime;
      this.id = id;
      this.method = exchange.getRequestMethod();
      this.uri = exchange.getRequestURI();
    }
  }
}
