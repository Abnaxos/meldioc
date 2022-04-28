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

package ch.raffael.meldioc.library.codec.gson;

import ch.raffael.meldioc.library.codec.AbstractCharDataObjectCodec;
import ch.raffael.meldioc.library.codec.ContentType;
import ch.raffael.meldioc.library.codec.ContentTypes;
import ch.raffael.meldioc.library.codec.ObjectCodec;
import ch.raffael.meldioc.library.codec.ObjectCodecFactory;
import ch.raffael.meldioc.library.codec.ObjectDecoder;
import ch.raffael.meldioc.library.codec.ObjectEncoder;
import ch.raffael.meldioc.util.Classes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import io.vavr.control.Option;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public class GsonObjectCodec<T> extends AbstractCharDataObjectCodec<T> {

  private final Gson gson;
  private final Class<T> type;

  public GsonObjectCodec(Gson gson, Class<T> type) {
    this(gson, type, DEFAULT_BUFFER_SIZE, none());
  }

  public GsonObjectCodec(Gson gson, Class<T> type, int bufferSize) {
    this(gson, type, bufferSize, none());
  }

  public GsonObjectCodec(Gson gson, Class<T> type, Charset charset) {
    this(gson, type, DEFAULT_BUFFER_SIZE, some(charset));
  }

  public GsonObjectCodec(Gson gson, Class<T> type, int bufferSize, Charset charset) {
    this(gson, type, bufferSize, some(charset));
  }

  public GsonObjectCodec(Gson gson, Class<T> type, int bufferSize, Option<Charset> charset) {
    super(bufferSize, charset);
    this.gson = gson;
    this.type = type;
  }

  @Override
  protected T decode(Reader source) {
    return gson.fromJson(new JsonReader(source), type);
  }

  @Override
  public boolean isInvalidInput(Throwable exception) {
    return isInvalidInput0(exception);
  }

  @Override
  protected void encode(Object value, Writer target) {
    gson.toJson(value, target);
  }

  @Override
  protected ContentType baseContentType() {
    return ContentTypes.JSON;
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

  private static boolean isInvalidInput0(Throwable exception) {
    return (exception instanceof JsonParseException)
        && !(exception instanceof JsonIOException);
  }

  public static class Factory implements ObjectCodecFactory {
    private final Gson gson;
    private final int bufferSize;
    private final Charset defaultCharset;

    public Factory(Gson gson, int bufferSize, Option<Charset> defaultCharset) {
      this.gson = gson;
      this.bufferSize = bufferSize;
      this.defaultCharset = defaultCharset.getOrElse(StandardCharsets.UTF_8);
    }

    @Override
    public <T> Option<ObjectEncoder<T>> encoder(Class<T> type, Option<ContentType> contentType) {
      return Option.of(create(contentType, type));
    }

    @Override
    public <T> Option<ObjectDecoder<T>> decoder(Option<ContentType> contentType, Class<T> type) {
      return Option.of(create(contentType, type));
    }

    public <T> Option<ObjectCodec<T>> codec(Option<ContentType> contentType, Class<T> type) {
      return Option.of(create(contentType, type));
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

  }
}
