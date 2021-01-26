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

import ch.raffael.meldioc.library.http.server.undertow.handler.ErrorMessageHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.ExceptionLogger;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import javax.annotation.Nullable;

/**
 * Throw this exception to return an HTTP error. Also contains some generic
 * utilities for handling errors.
 */
public class HttpStatusException extends Exception {

  private final HttpStatus status;

  public HttpStatusException(HttpStatus status) {
    this(status, null);
  }

  public HttpStatusException(HttpStatus status, @Nullable Throwable cause) {
    super(status.toString(), cause);
    this.status = status;
    if (!status.isValid()) {
      var throwing = new IllegalArgumentException("Invalid HTTP status code " + status.category());
      throwing.addSuppressed(this);
      throw throwing;
    }
  }

  public HttpStatus status() {
    return status;
  }

  public static HttpStatusException badRequest() {
    return badRequest("Bad request");
  }

  public static HttpStatusException badRequest(String reason) {
    return new HttpStatusException(HttpStatus.BAD_REQUEST.reason(reason));
  }

  public static HttpStatusException badRequest(String reason, Throwable cause) {
    return new HttpStatusException(HttpStatus.BAD_REQUEST.reason(reason), cause);
  }

  public static HttpStatusException badRequest(Throwable cause) {
    return new HttpStatusException(HttpStatus.BAD_REQUEST, cause);
  }

  public static HttpStatusException serverError() {
    return new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public static HttpStatusException serverError(String reason) {
    return new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR.reason(reason));
  }

  public static HttpStatusException serverError(String reason, Throwable cause) {
    return new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR.reason(reason), cause);
  }

  public static HttpStatusException serverError(Throwable cause) {
    return new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, cause);
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
    exchange.setStatusCode(status.code());
    status.reasonOption().forEach(exchange::setReasonPhrase);
    ErrorMessageHandler.addMessage(exchange, this);
  }

  public static void endRequestWithServerError(HttpServerExchange exchange, Throwable exception) {
    serverError().endRequest(exchange);
  }

  public static void endRequestWithBadRequest(HttpServerExchange exchange, Throwable exception) {
    serverError(exception).endRequest(exchange);
  }

  public static void endRequestWithBadRequest(HttpServerExchange exchange, String reason) {
    serverError(reason).endRequest(exchange);
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
