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

import ch.raffael.compose.http.undertow.HttpStatusException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.vavr.Tuple2;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple string codecs.
 */
public class TextCodec implements Encoder<Object, CharSequence>, Decoder<Object, String> {

  private static final TextCodec STD_PLAIN_TEXT = plainText(UTF_8);
  private static final TextCodec STD_HTML = html(UTF_8, ISO_8859_1);
  private static final TextCodec STD_XML = xml(UTF_8);
  private static final TextCodec STD_XHTML = xhtml(UTF_8);
  private static final TextCodec JSON = new TextCodec("application/json", UTF_8, false);

  private final String contentType;
  private final Charset encodeCharset;
  private final Charset decodeCharset;
  private final String fullContentType;


  public TextCodec(String contentType, Charset charset, boolean declareCharsetInType) {
    this(contentType, charset, charset, declareCharsetInType);
  }

  public TextCodec(String contentType, Charset encodeCharset, Charset decodeCharset, boolean declareCharsetInType) {
    this.contentType = contentType;
    this.encodeCharset = encodeCharset;
    this.decodeCharset = decodeCharset;
    fullContentType = declareCharsetInType ? contentType + "; charset=" + encodeCharset.name() : contentType;
  }

  public TextCodec(String contentType, Charset charset) {
    this(contentType, charset, true);
  }

  public TextCodec(String contentType, Charset encodeCharset, Charset decodeCharset) {
    this(contentType, encodeCharset, decodeCharset, true);
  }

  public TextCodec(String contentType) {
    this(contentType, UTF_8);
  }

  public static TextCodec plainText() {
    return STD_PLAIN_TEXT;
  }

  public static TextCodec plainText(Charset charset) {
    return new TextCodec("text/plain", charset);
  }

  public static TextCodec html() {
    return STD_HTML;
  }

  public static TextCodec html(Charset charset) {
    return new TextCodec("text/html", charset, ISO_8859_1);
  }

  public static TextCodec html(Charset encodeCharset, Charset decodeCharset) {
    return new TextCodec("text/html", encodeCharset, decodeCharset);
  }

  public static TextCodec xml() {
    return STD_XML;
  }

  public static TextCodec xml(Charset charset) {
    return new TextCodec("application/xml", charset, false);
  }

  public static TextCodec xhtml() {
    return STD_XHTML;
  }

  public static TextCodec xhtml(Charset charset) {
    return new TextCodec("application/xhtml+xml", charset, false);
  }

  public static TextCodec json() {
    return JSON;
  }

  @Override
  public void encode(HttpServerExchange exchange, Object ctx, CharSequence value) {
    if (value instanceof String) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, fullContentType);
      exchange.getResponseSender().send((String)value, encodeCharset);
    } else {
      var bytes = encodeCharset.encode(CharBuffer.wrap(value));
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, fullContentType);
      exchange.getResponseSender().send(bytes);
    }
  }

  @Override
  public void decode(HttpServerExchange exchange, Object ctx, Consumer<? super Object, ? super String> consumer) {
    Tuple2<String, Charset> type = ContentTypes.typeWithCharset(
        exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE), decodeCharset);
    exchange.getRequestReceiver().receiveFullString((ex, data) -> {
      try {
        consumer.accept(ex, data);
      } catch (Exception e) {
        HttpStatusException.serverError(e).endRequest(ex);
      }
    }, HttpStatusException::endRequestWithServerError, type._2);
  }
}
