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

package ch.raffael.compose.http.undertow.routing;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public final class PathSegmentHandler implements HttpHandler {

  private final Option<HttpHandler> hereHandler;
  private final Map<String, HttpHandler> exactSegments;
  private final Option<Tuple2<Seq<? extends Capture.Attachment<?>>, HttpHandler>> captureHandler;
  private final HttpHandler defaultHandler;

  private PathSegmentHandler(Option<HttpHandler> hereHandler,
                             Map<String, HttpHandler> exactSegments,
                             Option<Tuple2<Seq<? extends Capture.Attachment<?>>, HttpHandler>> captureHandler,
                             HttpHandler defaultHandler) {
    this.hereHandler = hereHandler;
    this.exactSegments = exactSegments;
    this.captureHandler = captureHandler;
    this.defaultHandler = defaultHandler;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var segment = exchange.getRelativePath();
    if (segment.isEmpty() || segment.equals("/")) {
      if (!segment.isEmpty()) {
        exchange.setResolvedPath(exchange.getResolvedPath() + segment);
        exchange.setRelativePath("");
      }
      hereHandler.getOrElse(() -> defaultHandler).handleRequest(exchange);
    } else {
      if (!exchange.getRelativePath().startsWith("/")) {
        throw new IllegalStateException("Relative path must start with '/'");
      }
      segment = segment.substring(1);
      int slash = segment.indexOf('/');
      if (slash >= 0) {
        segment = segment.substring(0, slash);
      }
      var exact = exactSegments.get(segment);
      if (exact.isDefined()) {
        updateMatch(exchange, segment);
        exact.get().handleRequest(exchange);
      } else {
        var cap = captureHandler;
        if (cap.isDefined()) {
          for (var c : cap.get()._1) {
            c.capture(exchange, segment);
          }
          updateMatch(exchange, segment);
          cap.get()._2.handleRequest(exchange);
        } else {
          defaultHandler.handleRequest(exchange);
        }
      }
    }
  }

  private void updateMatch(HttpServerExchange exchange, String segment) {
    exchange.setRelativePath(exchange.getRelativePath().substring(segment.length() + 1));
    exchange.setResolvedPath(exchange.getResolvedPath() + "/" + segment);
  }

  public static final class Builder {

    private Option<HttpHandler> hereHandler = None();
    private Map<String, HttpHandler> exactSegments = Map();
    private Option<Tuple2<Seq<? extends Capture.Attachment<?>>, HttpHandler>> capture = None();
    private HttpHandler defaultHandler = ResponseCodeHandler.HANDLE_404;

    private Builder() {
    }

    public Builder hereHandler(HttpHandler hereHandler) {
      this.hereHandler = Some(hereHandler);
      return this;
    }

    public Builder exactSegment(String segment, HttpHandler handler) {
      exactSegments = exactSegments.put(segment, handler);
      return this;
    }

    public Builder capture(Capture.Attachment<?> capture, HttpHandler handler) {
      this.capture = Some(Tuple(Seq(capture), handler));
      return this;
    }

    public Builder capture(Seq<? extends Capture.Attachment<?>> capture, HttpHandler handler) {
      this.capture = Some(Tuple(capture, handler));
      return this;
    }

    public Builder defaultHandler(HttpHandler defaultHandler) {
      this.defaultHandler = defaultHandler;
      return this;
    }

    public PathSegmentHandler build() {
      return new PathSegmentHandler(hereHandler, exactSegments, capture, defaultHandler);
    }
  }
}
