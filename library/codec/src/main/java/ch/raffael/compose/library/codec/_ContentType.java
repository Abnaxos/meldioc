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

import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.immutables.value.Value;

import java.nio.charset.Charset;

import static io.vavr.API.*;
import static java.nio.charset.StandardCharsets.UTF_8;


@Immutable.Public
abstract class _ContentType {

  public static ContentType of(String type, String subtype) {
    return ContentType.of(type, subtype, Map());
  }

  @Value.Parameter
  public abstract String type();

  @Value.Parameter
  public abstract String subtype();

  @Value.Parameter
  public abstract Map<String, String> attributes();

  public ContentType withoutAttributes() {
    return ((ContentType) this).withAttributes(Map());
  }

  public ContentType addCharsetAttribute(Charset charset) {
    return ((ContentType) this).withAttributes(attributes().put(ContentTypes.CHARSET_ATTR, charset.name()));
  }

  public ContentType withDefaultCharset(Charset charset) {
    var self = (ContentType) this;
    return attributes().containsKey(ContentTypes.CHARSET_ATTR)
           ? self
           : self.addCharsetAttribute(charset);
  }

  @Value.Lazy
  public Option<Charset> charset() {
    // actually, the specification says charset=... is forbidden for JSON and should be ignored if present
    // JSON is either UTF-8, UTF-16LE or UTF-16BE, otherwise, it's not valid JSON according to the spec
    // we're being lenient here, there's a lot of software that doesn't know this
    return attributes().get(ContentTypes.CHARSET_ATTR)
        .map(Charset::forName)
        .orElse(() -> equalsTypeOnly(ContentTypes.XML) || equalsTypeOnly(ContentTypes.JSON) ? Some(UTF_8) : None());
  }

  public Charset charset(Charset fallback) {
    return charset().getOrElse(fallback);
  }

  public boolean equalsTypeOnly(ContentType that) {
    return type().equals(that.type()) && subtype().equals(that.subtype());
  }

  public String render() {
    return render(new StringBuilder()).toString();
  }

  public StringBuilder render(StringBuilder buf) {
    var self = (ContentType) this;
    var attr = attributes();
    if (ContentTypes.isUnicodeType(self) && charset().map(ContentTypes::isImpliedUnicodeCharset).getOrElse(false)) {
      attr = attr.remove(ContentTypes.CHARSET_ATTR);
    }
    buf.append(type()).append('/').append(subtype());
    if (!attr.isEmpty()) {
      buf.append(';');
      attributes().forEach((k, v) -> {
        String quote = "";
        for (int i = 0; i < v.length(); i++) {
          if (!ContentTypes.Parser.isTokenChar(v.charAt(i))) {
            quote = "\"";
            break;
          }
        }
        buf.append(' ').append(k).append('=').append(quote).append(v).append(quote);
      });
    }
    return buf;
  }
}
