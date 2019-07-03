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
import io.vavr.control.Option;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public class GenericStringEncoder implements Encoder {

  private final String contentType;
  private final Charset charset;
  private final boolean declareCharsetInType;

  public GenericStringEncoder(String contentType, Charset charset, boolean declareCharsetInType) {
    this.contentType = contentType;
    this.charset = charset;
    this.declareCharsetInType = declareCharsetInType;
  }

  public GenericStringEncoder(String contentType, Charset charset) {
    this(contentType, charset, true);
  }

  public GenericStringEncoder(String contentType) {
    this(contentType, StandardCharsets.UTF_8);
  }

  public static GenericStringEncoder plainText() {
    return plainText(StandardCharsets.UTF_8);
  }

  public static GenericStringEncoder plainText(Charset charset) {
    return new GenericStringEncoder("text/plain", charset);
  }

  @Override
  public Option<Responder> prepare(HttpServerExchange exchange, Object response) {
    if (response instanceof CharSequence) {
      return Some(new Responder() {
        @Override
        public boolean wantRespond() {
          return true;
        }
        @Override
        public String contentType() {
          return contentType;
        }
        @Override
        public Option<Charset> charset() {
          return Some(charset);
        }

        @Override
        public void respond() {
          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, declareCharsetInType
                                                                  ? contentType + "; charset=" + charset.name()
                                                                  : contentType);
          exchange.getResponseSender().send(response.toString(), charset);
        }
      });
    } else {
      return None();
    }
  }
}
