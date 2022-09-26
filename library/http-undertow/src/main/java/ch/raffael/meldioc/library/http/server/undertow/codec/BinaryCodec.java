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
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;

public abstract class BinaryCodec<T> implements HttpEncoder<T>, HttpDecoder<T> {

  private final Seq<ContentType> contentTypes;

  protected BinaryCodec(String... contentTypes) {
    this(Stream.of(contentTypes).map(t -> ContentTypes.parseContentType(t).getOrElseThrow(
        () -> new IllegalArgumentException("Cannot parse content type: " + t))));
  }

  protected BinaryCodec(ContentType contentType) {
    this(List.of(contentType));
  }

  BinaryCodec(Traversable<? extends ContentType> contentTypes) {
    this.contentTypes = contentTypes.isEmpty()
        ? List.of(ContentTypes.OCTET_STREAM)
        : Traversable.<ContentType>narrow(contentTypes).toList();
  }

  @Override
  public void decode(HttpServerExchange exchange, Consumer<? super T> consumer) throws Exception {
    exchange.getRequestReceiver().receiveFullBytes((ex, bytes) -> {
      try {
        consumer.accept(ex, fromBytes(bytes));
      } catch (Exception e) {
        HttpStatusException.serverError(e).endRequest(exchange);
      }
    }, HttpStatusException::endRequestWithServerError);
  }

  @Override
  public void encode(HttpServerExchange exchange, T value) {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,
        contentTypes.headOption().getOrElse(ContentTypes.OCTET_STREAM).render());
    exchange.getResponseSender().send(toByteBuffers(value));
  }

  protected abstract java.nio.ByteBuffer[] toByteBuffers(T value);
  protected abstract T fromBytes(byte[] bytes);

  public static final class ByteArray extends BinaryCodec<byte[]> {
    public ByteArray(String... contentTypes) {
      super(contentTypes);
    }
    public ByteArray(ContentType... contentType) {
      super(Array.of(contentType));
    }
    public ByteArray(Traversable<? extends ContentType> contentTypes) {
      super(contentTypes);
    }
    @Override
    protected java.nio.ByteBuffer[] toByteBuffers(byte[] value) {
      return new java.nio.ByteBuffer[] {java.nio.ByteBuffer.wrap(value)};
    }
    @Override
    protected byte[] fromBytes(byte[] bytes) {
      return bytes;
    }
  }

  public static final class ByteBuffer extends BinaryCodec<java.nio.ByteBuffer> {
    public ByteBuffer(String... contentTypes) {
      super(contentTypes);
    }
    public ByteBuffer(ContentType... contentType) {
      super(Array.of(contentType));
    }
    public ByteBuffer(Traversable<? extends ContentType> contentTypes) {
      super(contentTypes);
    }
    @Override
    protected java.nio.ByteBuffer[] toByteBuffers(java.nio.ByteBuffer value) {
      return new java.nio.ByteBuffer[]{value};
    }
    @Override
    protected java.nio.ByteBuffer fromBytes(byte[] bytes) {
      return java.nio.ByteBuffer.wrap(bytes);
    }
  }

  public static final class ByteBufferArray extends BinaryCodec<java.nio.ByteBuffer[]> {
    public ByteBufferArray(String... contentTypes) {
      super(contentTypes);
    }
    public ByteBufferArray(ContentType... contentType) {
      super(Array.of(contentType));
    }
    public ByteBufferArray(Traversable<? extends ContentType> contentTypes) {
      super(contentTypes);
    }

    @Override
    protected java.nio.ByteBuffer[] toByteBuffers(java.nio.ByteBuffer[] value) {
      return value;
    }

    @Override
    protected java.nio.ByteBuffer[] fromBytes(byte[] bytes) {
      return new java.nio.ByteBuffer[]{java.nio.ByteBuffer.wrap(bytes)};
    }
  }
}
