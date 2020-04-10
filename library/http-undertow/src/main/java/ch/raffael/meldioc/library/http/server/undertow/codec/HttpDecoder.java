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

package ch.raffael.meldioc.library.http.server.undertow.codec;

import ch.raffael.meldioc.library.codec.ObjectDecoder;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.vavr.control.Option;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Read the HTTP request body data to a Java object.
 */
public interface HttpDecoder<C, B> {

  void decode(HttpServerExchange exchange, C ctx, Consumer<? super C, ? super B> consumer) throws Exception;

  static <B> HttpDecoder<Object, B> wrapBuffered(ObjectDecoder<? extends B> decoder) {
    return (exchange, ctx, consumer) ->
        exchange.getRequestReceiver().receiveFullBytes((ex, bytes) -> {
          try {
            consumer.accept(exchange, decoder.decode(bytes));
          } catch (Exception e) {
            HttpStatusException.badRequest(e.toString(), e).endRequest(exchange);
          }
        }, HttpStatusException::endRequestWithServerError);
  }

  static Option<String> contentType(HeaderMap headers) {
    var contentType = headers.getFirst(Headers.CONTENT_TYPE);
    if (contentType == null) {
      return none();
    } else {
      int pos = contentType.indexOf(';');
      return some((pos < 0 ? contentType : contentType.substring(0, pos)).trim());
    }
  }

  @FunctionalInterface
  interface Consumer<C, B> {
    void accept(HttpServerExchange exchange, B body) throws Exception;
  }

}
