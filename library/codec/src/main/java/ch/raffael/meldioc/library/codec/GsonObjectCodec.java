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

package ch.raffael.meldioc.library.codec;

import ch.raffael.meldioc.util.Classes;
import ch.raffael.meldioc.util.IOStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.gson.VavrGson;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;

import static ch.raffael.meldioc.logging.Logging.logger;
import static io.vavr.API.*;

public class GsonObjectCodec<T> implements ObjectCodec<T> {

  private static final Logger LOG = logger();

  public static final int MIN_BUFFER_SIZE = 2;
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  public static final ContentType CONTENT_TYPE = ContentTypes.JSON;
  public static final Option.Some<ContentType> SOME_CONTENT_TYPE = Some(CONTENT_TYPE);

  private static final int PROBE_SIZE = 2;

  private final Gson gson;
  private final Class<T> type;
  private final int bufferSize;
  private final Option<Charset> charset;

  public GsonObjectCodec(Gson gson, Class<T> type) {
    this(gson, type, IOStreams.DEFAULT_BUFFER_SIZE, None());
  }

  public GsonObjectCodec(Gson gson, Class<T> type, int bufferSize) {
    this(gson, type, bufferSize, None());
  }

  public GsonObjectCodec(Gson gson, Class<T> type, Charset charset) {
    this(gson, type, IOStreams.DEFAULT_BUFFER_SIZE, Some(charset));
  }

  public GsonObjectCodec(Gson gson, Class<T> type, int bufferSize, Charset charset) {
    this(gson, type, bufferSize, Some(charset));
  }

  public GsonObjectCodec(Gson gson, Class<T> type, int bufferSize, Option<Charset> charset) {
    if (bufferSize < MIN_BUFFER_SIZE) {
      throw new IllegalArgumentException("Buffer size " + bufferSize + " too small, need at least " + MIN_BUFFER_SIZE);
    }
    this.gson = gson;
    this.type = type;
    this.bufferSize = bufferSize;
    this.charset = charset;
  }

  @Override
  public T decode(InputStream stream) throws IOException {
    var scs =
        IOStreams.probe(stream, charset, bufferSize, PROBE_SIZE, (head, __) -> ContentTypes.detectUnicodeCharset(head));
    return gson.fromJson(new JsonReader(new InputStreamReader(scs._2, scs._1.getOrElse(DEFAULT_CHARSET))), type);
  }

  @Override
  public T decode(byte[] data) {
    return gson.fromJson(new JsonReader(new InputStreamReader(
        new ByteArrayInputStream(data), charset.getOrElse(ContentTypes.detectUnicodeCharset(data)))), type);
  }

  @Override
  public ContentType encode(T value, OutputStream target) throws IOException {
    var charset = this.charset.getOrElse(DEFAULT_CHARSET);
    OutputStreamWriter out = new OutputStreamWriter(target, charset);
    gson.toJson(value, out);
    out.flush();
    return actualContentType(charset);
  }

  @Override
  public Tuple2<byte[], ContentType> encode(T value) throws IOException {
    var out = new ByteArrayOutputStream();
    var ct = encode(value, out);
    return Tuple(out.toByteArray(), ct);
  }

  private ContentType actualContentType(Charset charset) {
    ContentType ct = ContentTypes.JSON;
    if (!ContentTypes.isImpliedUnicodeCharset(charset)) {
      ct = ct.addCharsetAttribute(charset);
    }
    return ct;
  }

  public static GsonBuilder standardGsonBuilder() {
    return registerVavr(probeRegisterVavr(loadServiceLoaderTypeAdapters(new GsonBuilder())));
  }

  public static GsonBuilder loadServiceLoaderTypeAdapters(GsonBuilder builder) {
    return loadServiceLoaderTypeAdapters(GsonObjectCodec.class, builder);
  }

  public static GsonBuilder loadServiceLoaderTypeAdapters(Class<?> refClass, GsonBuilder builder) {
    ServiceLoader.load(TypeAdapterFactory.class, Classes.classLoader(refClass)).stream()
        .map(ServiceLoader.Provider::get)
        .forEach(builder::registerTypeAdapterFactory);
    return builder;
  }

  public static GsonBuilder loadServiceLoaderTypeAdapters(ClassLoader classLoader, GsonBuilder builder) {
    ServiceLoader.load(TypeAdapterFactory.class, classLoader).stream()
        .map(ServiceLoader.Provider::get)
        .forEach(builder::registerTypeAdapterFactory);
    return builder;
  }

  public static GsonBuilder probeRegisterVavr(GsonBuilder builder) {
    try {
      Class.forName("io.vavr.gson.VavrGson");
      LOG.debug("Installing VavrGson");
      registerVavr(builder);
    } catch (ClassNotFoundException e) {
      LOG.debug("Not installing VavrGson: {}", e.toString());
    }
    return builder;
  }

  public static GsonBuilder registerVavr(GsonBuilder builder) {
    VavrGson.registerAll(builder);
    return builder;
  }

  public static class Factory implements ObjectCodecFactory {
    private final Gson gson;
    private final int bufferSize;
    private final Charset defaultCharset;

    public Factory(Gson gson, int bufferSize, Option<Charset> defaultCharset) {
      this.gson = gson;
      this.bufferSize = bufferSize;
      this.defaultCharset = defaultCharset.getOrElse(DEFAULT_CHARSET);
    }

    @Override
    public <T> Option<ObjectEncoder<T>> encoder(Class<T> type, Option<ContentType> contentType) {
      return Option(create(contentType, type));
    }

    @Override
    public <T> Option<ObjectDecoder<T>> decoder(Option<ContentType> contentType, Class<T> type) {
      return Option(create(contentType, type));
    }

    public <T> Option<ObjectCodec<T>> codec(Option<ContentType> contentType, Class<T> type) {
      return Option(create(contentType, type));
    }

    @Nullable
    private <T> GsonObjectCodec<T> create(Option<ContentType> contentType, Class<T> type) {
      return contentType
          .orElse(SOME_CONTENT_TYPE)
          .filter(ct -> ct.equalsTypeOnly(CONTENT_TYPE))
          .map(ct -> new GsonObjectCodec<>(gson, type, bufferSize, ct.charset(defaultCharset)))
          .getOrNull();
    }

    @Override
    public boolean canEncode(Class<?> type) {
      return true;
    }

    @Override
    public boolean canEncodeAs(ContentType contentType) {
      return contentType.equalsTypeOnly(CONTENT_TYPE);
    }

    @Override
    public boolean canDecode(ContentType contentType) {
      return contentType.equalsTypeOnly(contentType);
    }

    @Override
    public boolean canDecodeAs(Class<?> type) {
      return true;
    }

    @Override
    public boolean isInvalidInput(Throwable exception) {
      return (exception instanceof JsonParseException)
          && !(exception instanceof JsonIOException);
    }
  }
}
