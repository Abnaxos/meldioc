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

package ch.raffael.compose.util.concurrent;

import java.util.function.Supplier;

/**
 * Support for publishing object so that all changes previously made are
 * visible after the publication point (safe publishing), i.e. all updates
 * to the wrapped object <em>happen-before</em> a call to {@link
 * #published()}.
 *
 * <p>In the context of compose, this will often be used in conjunction with
 * extension point acceptors.
 *
 * @implNote The memory barrier is implemented using a temporary object with
 * a final field that's initialised with the given instance, then returning
 * the value from that final field.
 */
public interface SafePublishable<T> {

  T unsafe();
  default T published() {
    return safePublish(unsafe());
  }

  static <T> SafePublishable<T> of(T instance) {
    return new SafePublishable<>() {
      @Override
      public T unsafe() {
        return instance;
      }
      @Override
      public T published() {
        return safePublish(instance);
      }
      @Override
      public String toString() {
        return SafePublishable.toString(instance);
      }
    };
  }

  static <T> SafePublishable<T> ofSupplier(Supplier<? extends T> supplier) {
    return new SafePublishable<>() {
      @Override
      public T unsafe() {
        return supplier.get();
      }
      @Override
      public T published() {
        return safePublish(supplier.get());
      }
      @Override
      public String toString() {
        return SafePublishable.toString(supplier);
      }
    };
  }

  static <T> T safePublish(T instance) {
    final class Membar {
      final T instance;
      Membar(T instance) {
        this.instance = instance;
      }
    }
    return new Membar(instance).instance;
  }

  private static String toString(Object inner) {
    return SafePublishable.class.getSimpleName() + "[" + inner + "]";
  }

}
