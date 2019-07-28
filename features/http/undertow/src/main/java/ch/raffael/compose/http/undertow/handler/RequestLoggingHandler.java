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
    var id = counter.getAndIncrement();
    if (idConsumer != null) {
      idConsumer.accept(exchange, id);
    }
    try {
      if (mdcKey != null) {
        MDC.put(mdcKey, String.valueOf(id));
      }
      var method = exchange.getRequestMethod();
      var uri = exchange.getRequestURI();
      level.logBegin(logger, marker, id, method, uri);
      var start = System.nanoTime();
      try {
        next.handleRequest(exchange);
      } finally {
        var duration = Duration.ofNanos(System.nanoTime() - start);
        level.logEnd(logger, marker, id, exchange.getStatusCode(), method, uri, duration);
      }
    } finally {
      if (mdcKey != null) {
        MDC.remove(mdcKey);
      }
    }
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

    void logBegin(Logger logger, @Nullable Marker marker, int id, HttpString method, String uri) {
      if (enabled(logger, marker)) {
        log(logger, marker, format(">>HTTP #{}: {} {}", id, method, uri));
      }
    }

    void logEnd(Logger logger, @Nullable Marker marker, int id, int status, HttpString method, String uri, Duration duration) {
      if (enabled(logger, marker)) {
        log(logger, marker, format("<<HTTP #{}: {}: {} {} ({})", id, status, method, uri, duration));
      }
    }

    String format(String msg, Object... args) {
      return MessageFormatter.arrayFormat(msg, args).getMessage();
    }

    abstract void log(Logger logger, @Nullable Marker marker, String msg);

    abstract boolean enabled(Logger logger, @Nullable Marker marker);
  }

}
