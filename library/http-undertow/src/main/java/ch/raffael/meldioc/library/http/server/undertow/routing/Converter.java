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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import javax.annotation.Nullable;
import java.util.Objects;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * TODO JavaDoc
 */
@FunctionalInterface
interface Converter<T> {

  @Nullable
  T convert(String name, @Nullable String string) throws HttpStatusException;

  default Converter<Option<T>> option() {
    return (n, v) -> v == null
                     ? none()
                     : some(Objects.requireNonNull(convert(n, v), "convert(n, v)"));
  }

  static Converter<String> asString() {
    return $Converters.STRING;
  }

  static Converter<Integer> asInt() {
    return $Converters.INT;
  }

  static Converter<Boolean> asBoolean() {
    return $Converters.BOOLEAN;
  }

  final class $Converters {
    private $Converters() {
    }

    private static final Set<String> TRUE = HashSet.of("TRUE", "YES", "Y", "ON", "1");

    private static Converter<String> STRING = (n, v) -> v == null ? null : v.trim();

    private static Converter<Integer> INT = (n, v) -> {
      if (v == null) {
        return null;
      }
      v = v.trim();
      try {
        if (v.startsWith("0x") || v.startsWith("0X")) {
          return Integer.valueOf(v.substring(2), 16);
        } else {
          return Integer.valueOf(v);
        }
      } catch (NumberFormatException e) {
        throw HttpStatusException.badRequest("Invalid int value for parameter '" + n + "'");
      }
    };

    private static Converter<Boolean> BOOLEAN = (n, v) -> v == null ? null : TRUE.contains(v.trim().toUpperCase());
  }

}
