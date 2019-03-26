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

package ch.raffael.compose.util;

import java.util.function.Function;

/**
 * @since 2019-03-18
 */
@Deprecated(forRemoval = true)
public final class Freezable<T> {

  private final boolean frozen;
  private final T value;

  private Freezable(boolean frozen, T value) {
    this.frozen = frozen;
    this.value = value;
  }

  public static <T> Freezable<T> of(T initialValue) {
    return new Freezable<>(false, initialValue);
  }

  public Freezable<T> update(Function<? super T, ? extends T> fun) {
    if (frozen) {
      // TODO (2019-03-18) better error message
      throw new IllegalStateException(this + " has been frozen");
    }
    return new Freezable<>(false, fun.apply(value));
  }

  public Freezable<T> freeze() {
    return frozen ? this : new Freezable<>(true, value);
  }

  public T value() {
    return value;
  }

}
