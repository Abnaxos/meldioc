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

package ch.raffael.meldioc.library.http.server.undertow.codec;

import ch.raffael.meldioc.library.codec.ContentType;
import ch.raffael.meldioc.library.codec.ContentTypes;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Utilities for dealing with content types.
 */
public final class HttpContentTypes {

  private HttpContentTypes() {
  }

  public static Option<ContentType> contentType(HttpServerExchange exchange) {
    return Option.of(exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE))
        .flatMap(ContentTypes::parseContentType);
  }

  public static Tuple2<String, Charset> typeWithCharset(@Nullable String fullContentType, Charset fallback) {
    return typeWithCharset(fullContentType).map2(cs -> cs.getOrElse(fallback));
  }

  public static Tuple2<String, Option<Charset>> typeWithCharset(@Nullable String fullContentType) {
    // TODO (2019-07-28) this is a bad solution
    if (fullContentType == null) {
      return Tuple.of("", none());
    }
    var contentType = fullContentType.trim();
    int pos = contentType.indexOf(';');
    if (pos < 0) {
      return Tuple.of(contentType, fixedCharset(contentType, none()));
    }
    var rest = contentType.substring(pos + 1);
    contentType = contentType.substring(0, pos).trim();
    var charsetName = Option.of(Headers.extractQuotedValueFromHeader(rest, "charset"));
    return Tuple.of(contentType, fixedCharset(contentType, charsetName.map(Charset::forName)));
  }

  private static Option<Charset> fixedCharset(String contentType, Option<Charset> declared) {
    //noinspection SwitchStatementWithTooFewBranches
    switch (contentType) {
      case "application/json":
        return some(StandardCharsets.UTF_8);
      default:
        return declared;
    }
  }

}
