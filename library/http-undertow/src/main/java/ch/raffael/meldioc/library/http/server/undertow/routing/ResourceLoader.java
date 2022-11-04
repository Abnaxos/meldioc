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
import ch.raffael.meldioc.library.http.server.undertow.codec.EmptyBody;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpMethod;
import ch.raffael.meldioc.util.IOStreams;
import io.vavr.collection.HashSet;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.util.function.Function;

class ResourceLoader implements Actions.Action0<byte[]> {
  private final Class<?> resourceClass;
  private final String resource;
  private final Actions.Action1<? super byte[], ? extends byte[]> processor;
  private final Object lock = new Object();
  private volatile Cache cache;

  ResourceLoader(Class<?> resourceClass, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor, Cache cache) {
    this.resourceClass = resourceClass;
    this.resource = resource;
    this.processor = processor;
    this.cache = cache;
  }

  public static EndpointBuilder<EmptyBody, byte[]> apply(Frame frame,
      ContentType contentType, Class<?> resourceClass, String resource) {
    return frame.endpoint(HashSet.of(HttpMethod.GET))
        .map(new ResourceLoader(resourceClass, resource, noProcessor(), new Cache.Hard()))
        .respond(new BinaryCodec.ByteArray(contentType));
  }

  public static EndpointBuilder<EmptyBody, byte[]> apply(Frame frame,
      String contentType, Class<?> resourceClass, String resource) {
    return apply(frame, ContentType.of(contentType), resourceClass, resource);
  }

  public static EndpointBuilder<EmptyBody, byte[]> apply(Frame frame,
      ContentType contentType, Class<?> resourceClass, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor) {
    return frame.endpoint(HashSet.of(HttpMethod.GET))
        .map(new ResourceLoader(resourceClass, resource, processor, new Cache.Hard()))
        .respond(new BinaryCodec.ByteArray(contentType));
  }

  public static EndpointBuilder<EmptyBody, byte[]> apply(Frame frame,
      String contentType, Class<?> resourceClass, String resource,
      Actions.Action1<? super byte[], ? extends byte[]> processor) {
    return apply(frame, ContentType.of(contentType), resourceClass, resource, processor);
  }

  private static Actions.Action1<byte[], byte[]> noProcessor() {
    return b -> b;
  }

  @Override
  public byte[] perform() throws Exception {
    var b = cache.get();
    if (b == null) {
      synchronized (lock) {
        b = cache.get();
        if (b == null) {
          b = processor.perform(load());
          cache = cache.put(b);
        }
      }
    }
    return b;
  }

  private byte[] load() throws IOException {
    try (InputStream stream = resourceClass.getResourceAsStream(resource)) {
      if (stream == null) {
        throw new IllegalArgumentException("resource " + resourceClass + "::" + resource);
      }
      return IOStreams.readFully(stream);
    }
  }

  private interface Cache {
    byte @Nullable [] get();
    Cache put(byte[] bytes);

    final class Hard implements Cache {
      private byte @Nullable [] bytes;
      @Override
      public byte @Nullable [] get() {
        return bytes;
      }
      @Override
      public Cache put(byte[] bytes) {
        this.bytes = bytes;
        return this;
      }
    }
    final class Ref implements Cache {
      private final Function<? super byte[], ? extends Reference<? extends byte[]>> refFun;
      @Nullable
      private Reference<? extends byte[]> ref;
      private Ref(Function<? super byte[], ? extends Reference<? extends byte[]>> refFun) {
        this.refFun = refFun;
      }
      @Override
      public byte @Nullable [] get() {
        var r = ref;
        if (r == null) {
          return null;
        }
        var b = r.get();
        if (b == null) {
          ref = null;
          return null;
        }
        return b;
      }
      @Override
      public Ref put(byte[] bytes) {
        ref = refFun.apply(bytes);
        return this;
      }
    }
  }
}
