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

///<<< n: 1..count
///> "import ch.raffael.meldioc.library.http.server.undertow.routing.Actions.Action$n;"
///>>>

import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatus;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

///> "// vagen ${new Date()}"

/**
 * A capture of some value from the request (path segment, query parameter).
 *
 * <p>The name if usually irrelevant and it's no problem to reuse the same
 * name multiple times. It's just to be able to insert something meaningful
 * in error messages.
 */
/// filename Capture.java
/// = `$Capture
///   --> `Capture
/// = `@$.Public
///   --> `public
@$.Public abstract class $Capture<T> {

  private final String name;

  $Capture(String name) {
    this.name = name;
  }

  public static <T> $Capture<T> of(String name, Supplier<? extends T> fun) {
    return ofExtractor(name, __ -> fun.get());
  }

  public static <T> $Capture<T> of(String name, Function<? super HttpServerExchange, ? extends T> fun) {
    return ofExtractor(name, t -> requireNonNull(fun.apply(t), "fun.apply(t)"));
  }

  public static <T> $Capture<T> ofExtractor(String name, Extractor<? extends T> extractor) {
    return new OfExtractor<>(name, extractor);
  }

  private static <T> T invokeExtractor(HttpServerExchange exchange, Extractor<? extends T> extractor)
      throws HttpStatusException {
    try {
      return extractor.get(exchange);
    } catch (HttpStatusException | RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      // TODO FIXME (2021-01-26) improve exception handling
      throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
  }

  public String name() {
    return name;
  }

  abstract T get(HttpServerExchange exchange) throws HttpStatusException;

  ///<<</ n: 0..<count
  ///
  /// = `$Capture<? extends Tall> vAll
  ///   --> fwd 1..n collect {"Capture<? extends T$it> v$it"} join ', '
  /// = `$Actions.ActionN<? super Tall, ? extends R>
  ///   --> "Action${n+1}<${(["? super T"]+(fwd 1..n collect {"? super T$it"})).join ', '}, ? extends R>"
  /// = `Tall
  ///   --> fwd 1..n collect {"T$it"} join ', '
  /// = `vAll.get(x)
  ///   --> (['get(x)']+(fwd 1..n collect {"v${it}.get(x)"})).join ', '
  /// = `vAll
  ///   --> fwd 1..n collect {"v$it"} join ', '
  /// = `$.x("name")
  ///  --> (['name()'] + (fwd 1..n collect {"v${it}.name()"})).join('+","+')

  ///
  /// -- some corrections:
  /// = `(, `
  ///   --> `(
  /// ~ `(^\s*),\s*
  ///   --> "$_1"
  /// = `, >
  ///   --> `>
  /// = `, ,
  ///   --> `,
  /// ~ `,([^\s"])
  ///   --> ", $_1"
  ///
  ///> "// map self + $n"

  public <R, Tall> $Capture<R> map($Capture<? extends Tall> vAll, $Actions.ActionN<? super Tall, ? extends R> action) {
    return map("f(" + $.x("name") + ")",
        vAll, action);
  }

  public <R, Tall> $Capture<R> map(String name, $Capture<? extends Tall> vAll, $Actions.ActionN<? super Tall, ? extends R> action) {
    return new Mapped<>(name, x ->
        action.perform(vAll.get(x)));
  }
  ///>>>

  // inner classes

  public static final class Attachment<T> extends $Capture<T> {
    private final AttachmentKey<String> key = AttachmentKey.create(String.class);
    private final Converter<? extends T> converter;

    Attachment(String name, Converter<? extends T> converter) {
      super(name);
      this.converter = converter;
    }

    T get(HttpServerExchange exchange) throws HttpStatusException {
      return Objects.requireNonNullElseGet(converter.convert(name(), exchange.getAttachment(key)),
          () -> {
            throw new IllegalStateException("No captured value for '" + name() + "'");
          });
    }

    void capture(HttpServerExchange exchange, String value) {
      exchange.putAttachment(key, value);
    }
  }

  static abstract class Cached<T> extends $Capture<T> {
    private final AttachmentKey<T> key = AttachmentKey.create(Object.class);

    Cached(String name) {
      super(name);
    }

    @Override
    T get(HttpServerExchange exchange) throws HttpStatusException {
      var value = exchange.getAttachment(key);
      if (value == null) {
        value = requireNonNull(extract(exchange), "extract(exchange)");
        exchange.putAttachment(key, value);
      }
      return value;
    }

    abstract T extract(HttpServerExchange exchange) throws HttpStatusException;
  }

  static final class Mapped<T> extends Cached<T> {
    private final Extractor<? extends T> extractor;
    Mapped(String name, Extractor<? extends T> extractor) {
      super(name);
      this.extractor = extractor;
    }
    @Override
    T extract(HttpServerExchange exchange) throws HttpStatusException {
      return invokeExtractor(exchange, extractor);
    }
  }

  static final class OfExtractor<T> extends Cached<T> {
    private final Extractor<? extends T> extractor;
    OfExtractor(String name, Extractor<? extends T> extractor) {
      super(name);
      this.extractor = extractor;
    }
    @Override
    T extract(HttpServerExchange exchange) throws HttpStatusException {
      return invokeExtractor(exchange, extractor);
    }
  }

  @FunctionalInterface
  interface Extractor<T> {
    T get(HttpServerExchange exchange) throws Exception;
  }
}
