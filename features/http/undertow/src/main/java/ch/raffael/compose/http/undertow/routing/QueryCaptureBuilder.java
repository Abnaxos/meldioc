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

package ch.raffael.compose.http.undertow.routing;

import ch.raffael.compose.http.undertow.HttpStatusException;
import io.undertow.server.HttpServerExchange;
import io.vavr.API;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public final class QueryCaptureBuilder {

  private final String name;

  protected QueryCaptureBuilder(String name) {
    this.name = name;
  }

  private static Collection<String> getAll(String name, HttpServerExchange exchange) {
    return Objects.requireNonNullElseGet(exchange.getQueryParameters().get(name), Collections::emptyList);
  }

  private static Option<String> getFirst(String name, HttpServerExchange exchange) {
    return Option(exchange.getQueryParameters().get(name)).flatMap(v -> v.isEmpty() ? None() : Some(v.peekFirst()));
  }

  public Single<String> asString() {
    return new Single<>(name, Converter.asString());
  }

  public Single<Integer> asInt() {
    return new Single<>(name, Converter.asInt());
  }

  public Single<Boolean> asBoolean() {
    return new Single<>(name, Converter.asBoolean());
  }

  public static final class Single<T> extends Capture<Option<T>> {

    private final Converter<? extends T> converter;

    private Single(String name, Converter<? extends T> converter) {
      super(name);
      this.converter = converter;
    }

    public Capture<T> required() {
      return new Capture<>(name()) {
        @Override
        T get(HttpServerExchange exchange) throws HttpStatusException {
          return Single.this.get(exchange)
              .getOrElseThrow(() -> HttpStatusException.badRequest("Missing parameter '" + name() + "'"));
        }
      };
    }

    public Capture<T> rq() {
      return required();
    }

    public Capture<T> orElse(T orElse) {
      return new Capture<>(name()) {
        @Override
        T get(HttpServerExchange exchange) throws HttpStatusException {
          return Single.this.get(exchange).getOrElse(orElse);
        }
      };
    }

    public Capture<T> orElse(Supplier<? extends T> orElse) {
      return new Capture<>(name()) {
        @Override
        T get(HttpServerExchange exchange) throws HttpStatusException {
          return Single.this.get(exchange).getOrElse(orElse);
        }
      };
    }

    public Collection<Seq<T>> list() {
      return new Collection<>(name(), List::empty, Seq::append, API::Seq);
    }

    public Collection<Set<T>> set() {
      return new Collection<>(name(), LinkedHashSet::empty, Set::add, API::Set);
    }

    @Override
    Option<T> get(HttpServerExchange exchange) throws HttpStatusException {
      var value = getFirst(name(), exchange);
      return !value.isDefined() ? None() : Some(converter.convert(name(), value.get()));
    }

    public final class Collection<C extends Traversable<T>> extends ch.raffael.compose.http.undertow.routing.Capture<C> {

      private final Supplier<? extends C> initial;
      private final BiFunction<? super C, ? super T, ? extends C> appender;
      private final Supplier<? extends C> orElse;

      private Collection(String name,
                         Supplier<? extends C> initial, BiFunction<? super C, ? super T, ? extends C> appender,
                         Supplier<? extends C> orElse) {
        super(name);
        this.initial = initial;
        this.appender = appender;
        this.orElse = orElse;
      }

      public Capture<C> nonEmpty() {
        return new Capture<>(name()) {
          @Override
          C get(HttpServerExchange exchange) throws HttpStatusException {
            var value = Collection.this.get(exchange);
            if (value.isEmpty()) {
              throw HttpStatusException.badRequest("Missing query parameter '" + name() + "'");
            }
            return value;
          }
        };
      }

      public Capture<C> orElse(C orElse) {
        return new Collection<>(name(), initial, appender, () -> orElse);
      }

      public Capture<C> orElse(Supplier<? extends C> orElse) {
        return new Collection<>(name(), initial, appender, orElse);
      }

      @Override
      C get(HttpServerExchange exchange) throws HttpStatusException {
        var all = getAll(name(), exchange);
        if (!all.isEmpty()) {
          var result = initial.get();
          for (var v : all) {
            result = appender.apply(result, converter.convert(name(), v));
          }
          return result;
        } else {
          return orElse.get();
        }
      }
    }
  }
}
