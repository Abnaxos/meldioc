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

package ch.raffael.meldioc.library.http.server.undertow.util;

import ch.raffael.meldioc.library.http.server.undertow.handler.ErrorMessageHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.ExceptionLogger;
import ch.raffael.meldioc.logging.Logging;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;

/**
 * Throw this exception to return an HTTP error. Also contains some generic
 * utilities for handling errors.
 */
public class HttpStatusException extends Exception {

  private static final Logger LOG = Logging.logger();

  private final int statusCode;

  public HttpStatusException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public HttpStatusException(int statusCode, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public HttpStatusException(int statusCode, Throwable cause) {
    super(cause.toString(), cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Kind getKind() {
    return Kind.ofCode(getStatusCode());
  }

  public static HttpStatusException badRequest() {
    return badRequest("Bad request");
  }

  public static HttpStatusException badRequest(String message) {
    return new HttpStatusException(StatusCodes.BAD_REQUEST, message);
  }

  public static HttpStatusException badRequest(String message, Throwable cause) {
    return new HttpStatusException(StatusCodes.BAD_REQUEST, message, cause);
  }

  public static HttpStatusException badRequest(Throwable cause) {
    return new HttpStatusException(StatusCodes.BAD_REQUEST, cause);
  }

  public static HttpStatusException serverError(String message) {
    return new HttpStatusException(StatusCodes.INTERNAL_SERVER_ERROR, message);
  }

  public static HttpStatusException serverError(String message, Throwable cause) {
    return new HttpStatusException(StatusCodes.INTERNAL_SERVER_ERROR, message, cause);
  }

  public static HttpStatusException serverError(Throwable cause) {
    return new HttpStatusException(StatusCodes.INTERNAL_SERVER_ERROR, cause);
  }

  public static class Handler implements HttpHandler {
    private final HttpHandler next;

    public Handler(HttpHandler next) {
      this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      try {
        next.handleRequest(exchange);
      } catch (HttpStatusException e) {
        e.endRequest(exchange);
      }
    }
  }

  public void endRequest(HttpServerExchange exchange) {
    ExceptionLogger.get(exchange).log(exchange, this);
    exchange.setStatusCode(getStatusCode());
    ErrorMessageHandler.addMessage(exchange, this);
  }

  public static void endRequestWithServerError(HttpServerExchange exchange, Throwable exception) {
    new HttpStatusException(StatusCodes.INTERNAL_SERVER_ERROR, exception).endRequest(exchange);
  }

  public static void endRequestWithBadRequest(HttpServerExchange exchange, Throwable exception) {
    new HttpStatusException(StatusCodes.BAD_REQUEST, exception).endRequest(exchange);
  }

  public static void endRequestWithBadRequest(HttpServerExchange exchange, String message) {
    new HttpStatusException(StatusCodes.BAD_REQUEST, message).endRequest(exchange);
  }

  public enum Kind {
    INFO, SUCCESS, REDIRECT, CLIENT_ERROR, SERVER_ERROR;

    static Kind ofCode(int code) {
      if (code >= 100 && code < 200) {
        return INFO;
      } else if (code >= 200 && code < 300) {
        return SUCCESS;
      } else if (code >= 300 && code < 400) {
        return REDIRECT;
      } else if (code >= 400 && code < 500) {
        return CLIENT_ERROR;
      } else {
        return SERVER_ERROR;
      }
    }
  }
}
