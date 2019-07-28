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

package ch.raffael.compose.http.undertow;

import io.undertow.attribute.ResponseReasonPhraseAttribute;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO JavaDoc
 */
public class HttpStatusException extends Exception {

  private static final Logger LOG = LoggerFactory.getLogger(HttpStatusException.class);

  private final int statusCode;

  public HttpStatusException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public static HttpStatusException badRequest() {
    return badRequest("Bad request");
  }

  public static HttpStatusException badRequest(String message) {
    return new HttpStatusException(StatusCodes.BAD_REQUEST, message);
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
        String msg = e.getLocalizedMessage();
        LOG.debug("Returning {}: {}", e.getStatusCode(), msg, e);
        exchange.setStatusCode(e.getStatusCode());
        ResponseReasonPhraseAttribute.INSTANCE.writeAttribute(exchange, msg);
      }
    }
  }


}
