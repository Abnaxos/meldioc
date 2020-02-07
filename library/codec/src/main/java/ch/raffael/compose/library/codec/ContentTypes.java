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

package ch.raffael.compose.library.codec;

import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.vavr.API.*;

/**
 * Utilities for dealing with content types.
 */
public final class ContentTypes {

  public static final String CHARSET_ATTR = "charset";
  public static final String Q_ATTR = "q";
  public static final ContentType XML = _ContentType.of("application", "xml");
  public static final ContentType JSON = _ContentType.of("application", "json");
  public static final ContentType HTML = _ContentType.of("text", "html");
  public static final ContentType XHTML = _ContentType.of("application", "xhtml+xml");
  public static final ContentType PLAIN_TEXT = _ContentType.of("text", "plain");

  public static final Set<Charset> IMPLIED_UNICODE_CHARSETS =
      Set(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE);

  private ContentTypes() {
  }

  public static boolean isUnicodeType(ContentType contentType) {
    return contentType.equalsTypeOnly(XML)
        || contentType.equalsTypeOnly(JSON)
        || contentType.equalsTypeOnly(XHTML);
  }

  public static boolean isXml(ContentType contentType) {
    return contentType.equalsTypeOnly(ContentTypes.XML);
  }

  public static boolean isJson(ContentType contentType) {
    return contentType.equalsTypeOnly(ContentTypes.JSON);
  }

  public static Charset detectUnicodeCharset(byte[] bytes) {
    if (bytes.length >= 2) {
      return detectUnicodeCharset(bytes[0], bytes[1]);
    }
    return StandardCharsets.UTF_8;
  }

  public static Charset detectUnicodeCharset(int b0, int b1) {
    if (b0 < 0 || b1 < 0) {
      return StandardCharsets.UTF_8;
    } else if (b0 == 0 && b1 != 0) {
      return StandardCharsets.UTF_16BE;
    } else if (b0 != 0 && b1 == 0) {
      return StandardCharsets.UTF_16LE;
    } else {
      return StandardCharsets.UTF_8;
    }
  }

  public static boolean isImpliedUnicodeCharset(Charset charset) {
    return IMPLIED_UNICODE_CHARSETS.contains(charset);
  }

  /**
   * Parses a content type as of
   * <a href="https://tools.ietf.org/html/rfc2045#page-12>RFC 2045</a>.
   * The parser is lenient, i.e. it tries to parse as much as possible
   * and returns what could be parsed. It doesn't throw any exceptions
   * on errors.
   */
  public static Option<ContentType> parseContentType(String contentTypeString) {
    return new Parser(contentTypeString).toContentType();
  }

  /**
   * Parses a list of content types separated with ',' (HTTP Accept header)
   * disregarding 'q=';
   */
  public static Seq<ContentType> parseContentTypeList(String contentTypeString) {
    Seq<String> split = Seq();
    boolean inQuote = false;
    int prev = 0;
    int pos;
    for (pos = 0; pos < contentTypeString.length(); pos++) {
      char c = contentTypeString.charAt(pos);
      if (c == '"') {
        inQuote = !inQuote;
      } else if (!inQuote && c == ',') {
        split = split.append(contentTypeString.substring(prev, pos));
        prev = pos + 1;
      }
    }
    if (prev < pos) {
      split = split.append(contentTypeString.substring(prev, pos));
    }
    return split
        .flatMap(ContentTypes::parseContentType);
  }

  /**
   * Parses a list of content types separated with ',' (HTTP Accept header)
   * and sorts the result by 'q='.
   */
  public static Seq<ContentType> parseContentTypeListQ(String contentTypeString) {
    return parseContentTypeList(contentTypeString)
        .sortBy((ContentType ct) ->
            ct.attributes().get(Q_ATTR).map(q -> {
              try {
                return Double.parseDouble(q);
              } catch (NumberFormatException e) {
                return 0.0;
              }
            }).getOrElse(1.0))
        .reverse();
  }

  static final class Parser {
    private static final String TSPECIAL = "()<>@,;:\\\"/[]?=";

    private final String source;
    private int position = 0;
    @SuppressWarnings("StringBufferField")
    private final StringBuilder buf;

    @Nullable
    private String type = null;
    @Nullable
    private String subtype = null;
    private Map<String, String> attributes = Map();

    private Parser(String source) {
      this.source = source;
      buf = new StringBuilder(source.length());
    }

    static boolean isTokenChar(char c) {
      // <32: CTL, 32: SPC
      return c >= 33 && c <= 127 && TSPECIAL.indexOf(c) < 0;
    }

    private Option<ContentType> toContentType() {
      type = null;
      subtype = null;
      attributes = Map();
      parse();
      if (type != null && subtype != null) {
        return Some(ContentType.of(type, subtype, attributes));
      } else {
        return None();
      }
    }

    private void parse() {
      skipSpace();
      if (end()) {
        return;
      }
      type = token();
      if (type.isEmpty() || !skipSpace()) {
        return;
      }
      if (c() != '/' || end()) {
        return;
      }
      next();
      if (!skipSpace()) {
        return;
      }
      subtype = token();
      if (subtype.isEmpty() || !skipSpace()) {
        return;
      }
      if (c() != ';') {
        return;
      }
      next();
      while (skipSpace()) {
        String name = token();
        if (name.isEmpty()) {
          return;
        }
        if (!skipSpace()) {
          return;
        }
        if (c() != '=') {
          return;
        }
        if (!next()) {
          return;
        }
        String value;
        if (c() == '"') {
          value = quoted();
          if (value == null) {
            return;
          }
        } else {
          value = token();
          if (value.isEmpty()) {
            return;
          }
        }
        attributes = attributes.put(name, value);
      }
    }

    private String token() {
      buf.setLength(0);
      while (!end() && isTokenChar()) {
        buf.append(c());
        next();
      }
      return buf.toString();
    }

    @Nullable
    private String quoted() {
      buf.setLength(0);
      next();
      while (more() && c() != '"') {
        buf.append(c());
        next();
      }
      return c() == '"' ? buf.toString() : null;
    }

    private char c() {
      return source.charAt(position);
    }

    private boolean next() {
      position++;
      return !end();
    }

    private boolean more() {
      return !end();
    }

    private boolean end() {
      return position >= source.length();
    }

    private boolean skipSpace() {
      while (true) {
        if (end()) {
          return false;
        }
        if (!Character.isWhitespace(c())) {
          return true;
        }
        next();
      }
    }

    private boolean isTokenChar() {
      return isTokenChar(c());
    }
  }
}
