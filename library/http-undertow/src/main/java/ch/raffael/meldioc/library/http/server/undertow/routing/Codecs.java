/*
 *  Copyright (c) 2021 Raffael Herzog
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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;

/**
 * TODO JavaDoc
 */
public final class Codecs<C> {

  private final CodecSupplier<C, String> plainTextCodec = new CodecSupplier<>() {
    @Override
    public HttpDecoder<? super C, ? extends String> decoder(Frame<C> frame) {
      return frame.dec.plainText();
    }

    @Override
    public HttpEncoder<? super C, ? super String> encoder(Frame<C> frame) {
      return frame.enc.plainText();
    }
  };
  private final EncoderSupplier<C, String> htmlCodec = f -> f.enc.html();

  Codecs() {
  }

  public Codecs.CodecSupplier<C, String> plainText() {
    return plainTextCodec;
  }

  public Codecs.EncoderSupplier<C, String> html() {
    return htmlCodec;
  }

  @FunctionalInterface
  interface DecoderSupplier<C, T> {
    HttpDecoder<? super C, ? extends T> decoder(Frame<C> frame);
  }

  @FunctionalInterface
  interface EncoderSupplier<C, T> {
    HttpEncoder<? super C, ? super T> encoder(Frame<C> frame);
  }

  interface CodecSupplier<C, T> extends DecoderSupplier<C, T>, EncoderSupplier<C, T> {}
}
