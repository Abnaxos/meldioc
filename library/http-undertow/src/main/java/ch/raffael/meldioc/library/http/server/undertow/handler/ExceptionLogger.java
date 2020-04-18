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
import ch.raffael.meldioc.logging.Logging;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * TODO JavaDoc
 */
public interface ExceptionLogger {
  AttachmentKey<ExceptionLogger> EXCEPTION_LOGGER_KEY = AttachmentKey.create(ExceptionLogger.class);

  void log(HttpServerExchange exchange, Throwable exception);

  static ExceptionLogger get(HttpServerExchange exchange) {
    return Objects.requireNonNullElse(exchange.getAttachment(EXCEPTION_LOGGER_KEY), Default.instance());
  }

  class Default implements ExceptionLogger {
    private static Logger LOG = Logging.logger();
    private static final Default INSTANCE = new Default();

    protected Default() {
    }

    public static Default instance() {
      return INSTANCE;
    }

    @Override
    public void log(HttpServerExchange exchange, Throwable exception) {
      if (exception instanceof HttpStatusException) {
        HttpStatusException se = (HttpStatusException) exception;
        switch (se.getKind()) {
          case INFO:
          case SUCCESS:
          case REDIRECT:
            LOG.debug("{} {}: Returning: {} {}",
                exchange.getRequestMethod(), exchange.getRequestURI(), se.getStatusCode(), se);
            break;
          case CLIENT_ERROR:
            LOG.info("{} {}: Client error: {} {}",
                exchange.getRequestMethod(), exchange.getRequestURI(), se.getStatusCode(), se);
            break;
          default:
            LOG.error("{} {}: Server error: {}",
                exchange.getRequestMethod(), exchange.getRequestURI(), se.getStatusCode(), se);
        }
      }
    }
  }
}
