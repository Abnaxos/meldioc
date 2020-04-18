/*
 *  Copyright (c) 2020 Raffael Herzog
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

import ch.raffael.meldioc.util.IOStreams;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Abstract object codec for character data like JSON or XML.
 */
public abstract class AbstractCharDataObjectCodec<T> implements ObjectCodec<T> {

  public static final int DEFAULT_BUFFER_SIZE = IOStreams.DEFAULT_BUFFER_SIZE;
  public static final int MIN_BUFFER_SIZE = 2;
  public static final ContentType CONTENT_TYPE = ContentTypes.JSON;
  public static final Option<ContentType> SOME_CONTENT_TYPE = some(CONTENT_TYPE);

  private static final int PROBE_SIZE = 2;

  private final int bufferSize;
  private final Option<Charset> charset;

  protected AbstractCharDataObjectCodec() {
    this(DEFAULT_BUFFER_SIZE, none());
  }

  protected AbstractCharDataObjectCodec(Option<Charset> charset) {
    this(DEFAULT_BUFFER_SIZE, charset);
  }

  protected AbstractCharDataObjectCodec(int bufferSize) {
    this(bufferSize, none());
  }

  protected AbstractCharDataObjectCodec(int bufferSize, Option<Charset> charset) {
    if (bufferSize < MIN_BUFFER_SIZE) {
      throw new IllegalArgumentException("Buffer size " + bufferSize + " too small, need at least " + MIN_BUFFER_SIZE);
    }
    this.bufferSize = bufferSize;
    this.charset = charset;
  }

  @Override
  public T decode(InputStream stream) throws IOException {
    var scs =
        IOStreams.probe(stream, charset, bufferSize, PROBE_SIZE, (head, __) -> ContentTypes.detectUnicodeCharset(head));
    return decode(new InputStreamReader(scs._2, scs._1.getOrElse(this::defaultCharset)));
  }

  @Override
  public T decode(byte[] data) throws IOException {
    return decode(new InputStreamReader(
        new ByteArrayInputStream(data), charset.getOrElse(ContentTypes.detectUnicodeCharset(data))));
  }

  @Override
  public ContentType encode(T value, OutputStream target) throws IOException {
    var charset = this.charset.getOrElse(this::defaultCharset);
    OutputStreamWriter out = new OutputStreamWriter(target, charset);
    encode(value, out);
    out.flush();
    return actualContentType(charset);
  }

  @Override
  public Tuple2<byte[], ContentType> encode(T value) throws IOException {
    var out = new ByteArrayOutputStream();
    var ct = encode(value, out);
    return Tuple.of(out.toByteArray(), ct);
  }

  protected abstract T decode(Reader source) throws IOException;

  protected abstract void encode(T value, Writer target) throws IOException;

  protected Charset defaultCharset() {
    return StandardCharsets.UTF_8;
  }

  protected abstract ContentType baseContentType();

  private ContentType actualContentType(Charset charset) {
    ContentType ct = baseContentType();
    if (!ContentTypes.isImpliedUnicodeCharset(charset)) {
      ct = ct.addCharsetAttribute(charset);
    }
    return ct;
  }

  /**
   * TODO JavaDoc
   */
  public abstract static class Configuration<SELF, T, S extends Consumer<? super T>> {

    private Seq<Consumer<? super T>> configurators;
    private Option<Integer> bufferSize = none();
    private Option<Charset> defaultCharset = none();

    public Configuration(Seq<Consumer<? super T>> configurators) {
      this.configurators = configurators;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public final SELF removeStandardConfigurators(S... remove) {
      if (remove == null || remove.length == 0) {
        removeStandardConfigurators(allStandardConfigurators());
      } else {
        configurators = configurators.removeAll(Arrays.asList(remove));
      }
      return self();
    }

    protected abstract S[] allStandardConfigurators();

    public SELF configure(Consumer<? super T> configurator) {
      configurators = configurators.append(configurator);
      return self();
    }

    public SELF bufferSize(int bufferSize) {
      if (this.bufferSize.isDefined()) {
        throw new IllegalStateException("Buffer size already set");
      }
      this.bufferSize = some(bufferSize);
      return self();
    }

    public SELF defaultCharset(Charset defaultCharset) {
      if (this.defaultCharset.isDefined()) {
        throw new IllegalStateException("Default charset already set");
      }
      this.defaultCharset = some(defaultCharset);
      return self();
    }

    protected Seq<Consumer<? super T>> configurators() {
      return configurators;
    }

    protected Option<Integer> bufferSize() {
      return bufferSize;
    }

    protected Option<Charset> defaultCharset() {
      return defaultCharset;
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
      return (SELF) this;
    }
  }
}
