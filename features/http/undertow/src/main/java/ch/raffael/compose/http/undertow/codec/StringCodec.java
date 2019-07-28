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

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TODO JavaDoc
 */
public class StringCodec implements Encoder<Object, CharSequence>, Decoder<Object, String> {

  private static final StringCodec PLAIN_TEXT_UTF8 = plainText(UTF_8);
  private static final StringCodec HTML_UTF8 = html(UTF_8);
  private static final StringCodec XML_UTF8 = xml(UTF_8);
  private static final StringCodec XHTML_UTF8 = xhtml(UTF_8);
  private static final StringCodec JSON = new StringCodec("application/json", UTF_8, false);

  private final String contentType;
  private final Charset charset;
  private final boolean declareCharsetInType;
  private final String fullContentType;


  public StringCodec(String contentType, Charset charset, boolean declareCharsetInType) {
    this.contentType = contentType;
    this.charset = charset;
    this.declareCharsetInType = declareCharsetInType;
    fullContentType = declareCharsetInType ? contentType + "; charset=" + charset.name() : contentType;
  }

  public StringCodec(String contentType, Charset charset) {
    this(contentType, charset, true);
  }

  public StringCodec(String contentType) {
    this(contentType, UTF_8);
  }

  public static StringCodec plainText() {
    return PLAIN_TEXT_UTF8;
  }

  public static StringCodec plainText(Charset charset) {
    return new StringCodec("text/plain", charset);
  }

  public static StringCodec html() {
    return HTML_UTF8;
  }

  public static StringCodec html(Charset charset) {
    return new StringCodec("text/html", charset);
  }

  public static StringCodec xml() {
    return XML_UTF8;
  }

  public static StringCodec xml(Charset charset) {
    return new StringCodec("application/xml", charset, false);
  }

  public static StringCodec xhtml() {
    return XHTML_UTF8;
  }

  public static StringCodec xhtml(Charset charset) {
    return new StringCodec("application/xhtml+xml", charset, false);
  }

  public static StringCodec json() {
    return JSON;
  }

  @Override
  public void encode(HttpServerExchange exchange, Object ctx, CharSequence value) {
    if (value instanceof String) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, fullContentType);
      exchange.getResponseSender().send((String)value, charset);
    } else {
      var bytes = charset.encode(CharBuffer.wrap(value));
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, fullContentType);
      exchange.getResponseSender().send(bytes);
    }
  }

  @Override
  public String decode(HttpServerExchange exchange, Object ctx) {
    // TODO FIXME (2019-07-20) not implemented
    return null;
  }
}
