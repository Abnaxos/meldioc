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

package ch.raffael.compose.tooling.util;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * TODO javadoc
 */
public class Verified<T> implements Supplier<T> {

  private final Supplier<T> supplier;

  private Verified(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  public static <T> Verified<T> of(Supplier<T> supplier) {
    return new Verified<>(supplier);
  }

  public static <T> Verified<T> of(T supplier) {
    return new Verified<>(() -> supplier);
  }

  public static <T> Verified<T> verify(Supplier<T> supplier) {
    return of(supplier);
  }

  public static <T> Verified<T> verify(T supplier) {
    return of(() -> supplier);
  }

  @Override
  public T get() {
    return Objects.requireNonNull(supplier.get());
  }

  public Verified<T> memoize() {
    return new Verified<>(new Supplier<>() {
      private boolean initialized = false;
      @Nullable
      private T value = null;
      @Override
      public T get() {
        if (!initialized) {
          value = Objects.requireNonNull(supplier.get());
          initialized = true;
        }
        assert value != null;
        return value;
      }
    });
  }

  public <U> Verified<U> map(Function<? super T, ? extends U> fun) {
    return new Verified<>(() -> {
      var v = supplier.get();
      return fun.apply(v);
    });
  }

  public Verified<T> with(Consumer<? super T> verifier) {
    return map((v) -> {
      verifier.accept(v);
      return v;
    });
  }

  public Verified<T> verify(Predicate<? super T> verifier, Function<? super T, String> msg) {
    return with(v -> {
      if (!verifier.test(v)) {
        throw new VerificationException(msg.apply(v));
      }
    });
  }

  public Verified<T> verify(Predicate<? super T> verifier, Supplier<String> msg) {
    return verify(verifier, (v) -> msg.get());
  }

  public Verified<T> verify(Predicate<? super T> verifier, String msg, Object... args) {
    return verify(verifier, () -> String.format(msg, (Object[]) args));
  }

  public Verified<T> nonnull(String format, Object... args) {
    return verify(Objects::nonNull, format, args);
  }

  @SuppressWarnings("unchecked")
  public <U extends T> Verified<U> instanceOf(Class<U> type, String format, Object... args) {
    return (Verified<U>) verify(type::isInstance, format, args);
  }

  @SuppressWarnings("unchecked")
  public <U extends T> Verified<U> instanceOf(Class<U> type) {
    return (Verified<U>) verify(type::isInstance, v -> String.format("%s not instance of %s", v, type.getName()));
  }

  public Verified<T> equalTo(T that) {
    return verify(v -> v.equals(that), "Value %s expeted, got %s", that, this);
  }

  public static <T> Consumer<T> verifierOf(Predicate<? super T> predicate, Function<? super T, String> messageFun) {
    return (v) -> {
      if (!predicate.test(v)) {
        throw new VerificationException(messageFun.apply(v));
      }
    };
  }

  public static <T> Consumer<T> verifierOf(Predicate<? super T> predicate, String format, Object... args) {
    return verifierOf(predicate, (v) -> String.format(format, args));
  }

  public static <T, O extends Optional<? extends T>> Consumer<O> present(String format, Object... args) {
    return verifierOf(Optional::isPresent, format, args);
  }

  public static <T, O extends Optional<? extends T>> Consumer<O> present(Function<? super O, String> messageFun) {
    return verifierOf(Optional::isPresent, messageFun);
  }

}
