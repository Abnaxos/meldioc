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

package ch.raffael.compose.util.fun;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @since 2019-03-25
 */
public final class Fun {

  private Fun() {
  }

  /**
   * Apply the function to the object and return the result of the
   * function.
   *
   * @return The result of the function.
   */
  public static <T, R> R let(T object, Function<? super T, R> fun) {
    return fun.apply(object);
  }

  /**
   * Accept the object using the given consumer and return the input
   * object.
   *
   * @return The input object.
   */
  public static <T> T with(T object, Consumer<? super T> fun) {
    fun.accept(object);
    return object;
  }

  public static <T> Optional<T> some(T value) {
    return Optional.of(value);
  }

  public static <T> Optional<T> none() {
    return Optional.empty();
  }

  public static <T> Optional<T> optional(@Nullable T value) {
    return Optional.ofNullable(value);
  }

}
