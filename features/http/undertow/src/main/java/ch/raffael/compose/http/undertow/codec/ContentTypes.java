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

package ch.raffael.compose.http.undertow.codec;

import io.undertow.util.Headers;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.vavr.API.*;

/**
 * Utilities for dealing with content types.
 */
public final class ContentTypes {

  private ContentTypes() {
  }

  public static Tuple2<String, Charset> typeWithCharset(String fullContentType, Charset fallback) {
    return typeWithCharset(fullContentType).map2(cs -> cs.getOrElse(fallback));
  }

  public static Tuple2<String, Option<Charset>> typeWithCharset(String fullContentType) {
    var contentType = fullContentType.trim();
    int pos = contentType.indexOf(';');
    if (pos < 0) {
      return Tuple(contentType, fixedCharset(contentType, None()));
    }
    var rest = contentType.substring(pos + 1);
    contentType = contentType.substring(0, pos).trim();
    var charsetName = Option(Headers.extractQuotedValueFromHeader(rest, "charset"));
    return Tuple(contentType, fixedCharset(contentType, charsetName.map(Charset::forName)));
  }

  private static Option<Charset> fixedCharset(String contentType, Option<Charset> declared) {
    //noinspection SwitchStatementWithTooFewBranches
    switch (contentType) {
      case "application/json":
        return Some(StandardCharsets.UTF_8);
      default:
        return declared;
    }
  }

}
