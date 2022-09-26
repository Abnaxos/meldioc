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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import ch.raffael.meldioc.library.codec.ContentType;
import ch.raffael.meldioc.library.http.server.undertow.codec.BinaryCodec;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpDecoder;
import ch.raffael.meldioc.library.http.server.undertow.codec.HttpEncoder;
import io.vavr.collection.Traversable;

import java.util.function.Function;

/**
 * TODO JavaDoc
 */
public final class Codecs {

  private final CodecSupplier<String> plainTextCodec = new CodecSupplier<>() {
    @Override
    public HttpDecoder<? extends String> decoder(Frame frame) {
      return frame.dec.plainText();
    }
    @Override
    public HttpEncoder<? super String> encoder(Frame frame) {
      return frame.enc.plainText();
    }
  };
  private final EncoderSupplier<String> htmlCodec = f -> f.enc.html();

  Codecs() {
  }

  public Codecs.CodecSupplier<String> plainText() {
    return plainTextCodec;
  }

  public Codecs.EncoderSupplier<String> html() {
    return htmlCodec;
  }

  public CodecSupplier<byte[] > byteArray(String... types) {
    return CodecSupplier.combined(__ -> new BinaryCodec.ByteArray(types));
  }

  public CodecSupplier<byte[]> byteArray(ContentType... types) {
    return CodecSupplier.combined(__ -> new BinaryCodec.ByteArray(types));
  }

  public CodecSupplier<byte[]> byteArray(Traversable<? extends ContentType> types) {
    return CodecSupplier.combined(__ -> new BinaryCodec.ByteArray(types));
  }

  public CodecSupplier<byte[]> byteBuffer(String... types) {
    return CodecSupplier.combined(__ -> new BinaryCodec.ByteArray(types));
  }

  public CodecSupplier<byte[]> byteBuffer(ContentType... types) {
    return CodecSupplier.combined(__ -> new BinaryCodec.ByteArray(types));
  }

  public CodecSupplier<byte[]> byteBuffer(Traversable<? extends ContentType> types) {
    return CodecSupplier.combined(__ -> new BinaryCodec.ByteArray(types));
  }

  @FunctionalInterface
  interface DecoderSupplier<T> {
    HttpDecoder<? extends T> decoder(Frame frame);
  }

  @FunctionalInterface
  interface EncoderSupplier<T> {
    HttpEncoder<? super T> encoder(Frame frame);
  }

  interface CodecSupplier<T> extends DecoderSupplier<T>, EncoderSupplier<T> {
    static <T, C extends HttpEncoder<T> & HttpDecoder<T>> CodecSupplier<T> combined(Function<? super Frame, ? extends C> fun) {
      return new CodecSupplier<T>() {
        @Override
        public HttpDecoder<? extends T> decoder(Frame frame) {
          return fun.apply(frame);
        }
        @Override
        public HttpEncoder<? super T> encoder(Frame frame) {
          return fun.apply(frame);
        }
      };
    }
  }
}
