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
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.vavr.control.Option;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple string codecs.
 */
public class TextCodec implements HttpEncoder<CharSequence>, HttpDecoder<String> {

  private final ContentType inputContentType;
  private final ContentType outputContentType;

  public TextCodec(ContentType inputContentType, ContentType outputContentType) {
    this.inputContentType = inputContentType.withDefaultCharset(UTF_8);
    this.outputContentType = outputContentType.withDefaultCharset(UTF_8);
  }

  public TextCodec(ContentType contentType) {
    this(contentType, contentType);
  }

  public TextCodec(ContentType contentType, Charset charset) {
    this(contentType.addCharsetAttribute(charset));
  }

  public TextCodec(ContentType contentType, Charset inCharset, Charset outCharset) {
    this(contentType.addCharsetAttribute(inCharset), contentType.addCharsetAttribute(outCharset));
  }

  public static TextCodec plainText() {
    return plainText(UTF_8);
  }

  public static TextCodec plainText(Charset charset) {
    return new TextCodec(ContentTypes.PLAIN_TEXT, charset);
  }

  public static TextCodec plainText(Charset inCharset, Charset outCharset) {
    return new TextCodec(ContentTypes.PLAIN_TEXT, inCharset, outCharset);
  }

  public static TextCodec html() {
    return html(ISO_8859_1, UTF_8);
  }

  public static TextCodec html(Charset charset) {
    return new TextCodec(ContentTypes.HTML.addCharsetAttribute(charset));
  }

  public static TextCodec html(Charset inCharset, Charset outCharset) {
    return new TextCodec(ContentTypes.HTML, inCharset, outCharset);
  }

  public static TextCodec xml() {
    return xml(UTF_8);
  }

  public static TextCodec xml(Charset charset) {
    return new TextCodec(ContentTypes.XML, charset);
  }

  public static TextCodec xml(Charset inCharset, Charset outCharset) {
    return new TextCodec(ContentTypes.XML, inCharset, outCharset);
  }

  public static TextCodec xhtml() {
    return html(UTF_8);
  }

  public static TextCodec xhtml(Charset charset) {
    return new TextCodec(ContentTypes.XHTML.addCharsetAttribute(charset));
  }

  public static TextCodec xhtml(Charset inCharset, Charset outCharset) {
    return new TextCodec(ContentTypes.XHTML, inCharset, outCharset);
  }

  public static TextCodec json() {
    return new TextCodec(ContentTypes.JSON);
  }

  @Override
  public void encode(HttpServerExchange exchange, CharSequence value) {
    var contentType = Option.of(exchange.getRequestHeaders().getFirst(Headers.ACCEPT))
        .filter(String::isBlank)
        .map(ContentTypes::parseContentTypeListQ)
        .flatMap(ctl -> ctl.find(ct -> ct.equalsTypeOnly(outputContentType)))
        .map(ct -> ct.withDefaultCharset(outputContentType.charset().get()))
        .getOrElse(outputContentType);
    if (value instanceof String) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType.render());
      exchange.getResponseSender().send((String)value, contentType.charset().get());
    } else {
      var bytes = outputContentType.charset().get().encode(CharBuffer.wrap(value));
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType.render());
      exchange.getResponseSender().send(bytes);
    }
  }

  @Override
  public void decode(HttpServerExchange exchange, Consumer<? super String> consumer) {
    var contentType = Option.of(exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE))
        .flatMap(ContentTypes::parseContentType)
        .map(ct -> ct.withDefaultCharset(inputContentType.charset().get()))
        .getOrElse(inputContentType);
    exchange.getRequestReceiver().receiveFullString((ex, data) -> {
      try {
        consumer.accept(ex, data);
      } catch (Exception e) {
        HttpStatusException.serverError(e).endRequest(ex);
      }
    }, HttpStatusException::endRequestWithServerError);
  }

}
