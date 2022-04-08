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

package ch.raffael.meldioc.library.codec.jackson;

import ch.raffael.meldioc.library.codec.AbstractCharDataObjectCodec;
import ch.raffael.meldioc.library.codec.ContentType;
import ch.raffael.meldioc.library.codec.ContentTypes;
import ch.raffael.meldioc.library.codec.ObjectCodec;
import ch.raffael.meldioc.library.codec.ObjectCodecFactory;
import ch.raffael.meldioc.library.codec.ObjectDecoder;
import ch.raffael.meldioc.library.codec.ObjectEncoder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * TODO JavaDoc
 */
public class JacksonObjectCodec<T> extends AbstractCharDataObjectCodec<T> {
  private static final Set<Class<? extends Throwable>> CLIENT_ERRORS = LinkedHashSet.of(
      JsonParseException.class, InputCoercionException.class, MismatchedInputException.class,
      ValueInstantiationException.class, UnresolvedForwardReference.class);

  private final Class<T> type;
  private final ObjectMapper objectMapper;

  public JacksonObjectCodec(ObjectMapper objectMapper, Class<T> type) {
    this(objectMapper, type, DEFAULT_BUFFER_SIZE, none());
  }

  public JacksonObjectCodec(ObjectMapper objectMapper, Class<T> type, Charset charset) {
    this(objectMapper, type, DEFAULT_BUFFER_SIZE, some(charset));
  }

  public JacksonObjectCodec(ObjectMapper objectMapper, Class<T> type, Option<Charset> charset) {
    this(objectMapper, type, DEFAULT_BUFFER_SIZE, charset);
  }

  public JacksonObjectCodec(ObjectMapper objectMapper, Class<T> type, int bufferSize) {
    this(objectMapper, type, bufferSize, none());
  }

  public JacksonObjectCodec(ObjectMapper objectMapper, Class<T> type, int bufferSize, Charset charset) {
    this(objectMapper, type, bufferSize, some(charset));
  }

  public JacksonObjectCodec(ObjectMapper objectMapper, Class<T> type, int bufferSize, Option<Charset> charset) {
    super(bufferSize, charset);
    this.type = type;
    this.objectMapper = objectMapper;
  }

  @Override
  protected T decode(Reader source) throws IOException {
    return objectMapper.readValue(source, type);
  }

  @Override
  public boolean isInvalidInput(Throwable exception) {
    return isInvalidInput0(exception);
  }

  @Override
  protected void encode(T value, Writer target) throws IOException {
    objectMapper.writeValue(target, value);
  }

  @Override
  protected ContentType baseContentType() {
    return ContentTypes.JSON;
  }

  private static boolean isInvalidInput0(Throwable exception) {
    return CLIENT_ERRORS.exists(c -> c.isInstance(exception));
  }

  public static class Factory implements ObjectCodecFactory {
    private final ObjectMapper objectMapper;
    private final int bufferSize;
    private final Charset defaultCharset;

    public Factory(ObjectMapper objectMapper, int bufferSize, Option<Charset> defaultCharset) {
      this.objectMapper = objectMapper;
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
    private <T> JacksonObjectCodec<T> create(Option<ContentType> contentType, Class<T> type) {
      return contentType
          .orElse(SOME_CONTENT_TYPE)
          .filter(ct -> ct.equalsTypeOnly(CONTENT_TYPE))
          .map(ct -> new JacksonObjectCodec<>(objectMapper, type, bufferSize, ct.charset(defaultCharset)))
          .getOrNull();
    }

    @Override
    public boolean canEncode(Class<?> type) {
      return true;
    }

    @Override
    public boolean canEncodeAs(ContentType contentType) {
      return contentType.equalsTypeOnly(ContentTypes.JSON);
    }

    @Override
    public boolean canDecode(ContentType contentType) {
      return contentType.equalsTypeOnly(ContentTypes.JSON);
    }

    @Override
    public boolean canDecodeAs(Class<?> type) {
      return true;
    }

    @SuppressWarnings("removal")
    @Override
    @Deprecated(forRemoval = true)
    public boolean isInvalidInput(Throwable exception) {
      return isInvalidInput0(exception);
    }
  }
}
