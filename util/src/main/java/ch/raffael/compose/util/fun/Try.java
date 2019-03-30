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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * TODO javadoc
 */
public abstract class Try<T> {

  private Try() {
  }

  public static <T> Try<T> success(T result) {
    staticRequireNonNullArg(result, "result");
    return new Success<>(result);
  }

  private static Try<Void> voidSuccess() {
    return Void.VOID_SUCCESS;
  }

  public static <T> Try<T> failure(Throwable exception) {
    staticRequireNonNullArg(exception, "exception");
    return new Failure<>(rethrowFatalError(exception));
  }

  public static <T> Try<T> perform(ThrowingSupplier<? extends T> fun) {
    staticRequireNonNullArg(fun, "fun");
    try {
      return success(staticRequireNonNullResult(fun.get(), "fun"));
    } catch (Throwable e) {
      return failure(e);
    }
  }

  public static Try<Void> perform(ThrowingRunnable fun) {
    staticRequireNonNullArg(fun, "fun");
    try {
      fun.run();
      return voidSuccess();
    } catch (Throwable e) {
      return failure(e);
    }
  }

  public static <T extends AutoCloseable, R> Try<R> withResource(T resource, ThrowingFunction<? super T, ? extends R> fun) {
    staticRequireNonNullArg(resource, "resource");
    staticRequireNonNullArg(fun, "fun");
    try (T r = resource) {
      return success(staticRequireNonNullResult(fun.apply(r), "fun"));
    } catch (Throwable e) {
      return failure(e);
    }
  }

  public static <T extends AutoCloseable> Try<Void> withResource(T resource, ThrowingConsumer<? super T> fun) {
    staticRequireNonNullArg(resource, "resource");
    staticRequireNonNullArg(fun, "fun");
    try (T r = resource) {
      fun.accept(resource);
      return voidSuccess();
    } catch (Throwable e) {
      return failure(e);
    }
  }

  public static <T, R> Try<R> withResource(T resource, ThrowingConsumer<? super T> closeFun, ThrowingFunction<? super T, ? extends R> fun) {
    staticRequireNonNullArg(resource, "resource");
    staticRequireNonNullArg(closeFun, "closeFun");
    staticRequireNonNullArg(fun, "fun");
    try (AutoCloseableWrapper<T> ac = new AutoCloseableWrapper<>(resource, closeFun)) {
      return success(staticRequireNonNullResult(fun.apply(resource), "fun"));
    } catch (Throwable e) {
      return failure(e);
    }
  }

  public static <T> Try<Void> withResource(T resource, ThrowingConsumer<? super T> closeFun, ThrowingConsumer<? super T> fun) {
    staticRequireNonNullArg(resource, "resource");
    staticRequireNonNullArg(closeFun, "closeFun");
    staticRequireNonNullArg(fun, "fun");
    try (AutoCloseableWrapper<T> ac = new AutoCloseableWrapper<>(resource, closeFun)) {
      fun.accept(resource);
      return voidSuccess();
    } catch (Throwable e) {
      return failure(e);
    }
  }


  /*
   * API
   */

  public abstract boolean isSuccess();

  public final boolean isFailure() {
    return !isSuccess();
  }

  public abstract <R> Try<R> map(Function<? super T, ? extends R> fun);

  public abstract <R> Try<R> flatMap(Function<? super T, ? extends Try<? extends R>> fun);

  public abstract Try<T> filter(Predicate<? super T> fun);

  public abstract void forEach(Consumer<T> fun) throws IllegalTryStateException;

  public Try<T> recover(Function<? super Throwable, ? extends T> fun) {
    return recover(Throwable.class, fun);
  }

  public abstract <E extends Throwable> Try<T> recover(Class<E> exceptionType, Function<? super E, ? extends T> fun);

  public Try<T> flatRecover(Function<? super Throwable, ? extends Try<? extends T>> fun) {
    return flatRecover(Throwable.class, fun);
  }

  public abstract <E extends Throwable> Try<T> flatRecover(Class<E> exceptionType, Function<? super E, ? extends Try<? extends T>> fun);

  public Try<T> mapException(Function<? super Throwable, ? extends Throwable> fun) {
    return mapException(Throwable.class, fun);
  }

  public abstract <E extends Throwable> Try<T> mapException(Class<E> exceptionType, Function<? super E, ? extends Throwable> fun);

  public abstract <U, R> Try<R> fold(Try<U> that, BiFunction<? super T, ? super U, ? extends R> fun);

  public abstract <U, R> Try<R> flatFold(Try<U> that, BiFunction<? super T, ? super U, ? extends Try<? extends R>> fun);

  public static <T, U> Collector<Try<T>, ?, Try<U>> folding(Collector<T, ?, U> downstream) {
    staticRequireNonNullArg(downstream, "downstream");
    return new FoldingCollector<>(downstream);
  }

  public abstract T get() throws IllegalTryStateException;

  public abstract Throwable getException() throws IllegalTryStateException;

  public T rethrowOrGet() {
    return rethrow().get();
  }

  public <E extends Throwable> T rethrowOrGet(Class<E> exceptionType) throws E {
    return rethrow(exceptionType).get();
  }

  public <E1 extends Throwable, E2 extends Throwable> T rethrowOrGet(Class<E1> exceptionType1, Class<E2> exceptionType2) throws E1, E2 {
    return rethrow(exceptionType1, exceptionType2).get();
  }

  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> T rethrowOrGet(Class<E1> exceptionType1, Class<E2> exceptionType2, Class<E3> exceptionType3) throws E1, E2, E3 {
    return rethrow(exceptionType1, exceptionType2, exceptionType3).get();
  }

  public abstract Try<T> rethrow() throws RuntimeException, Error;

  public abstract <E extends Throwable> Try<T> rethrow(Class<E> exceptionType) throws E, RuntimeException, Error;

  public abstract <E1 extends Throwable, E2 extends Throwable> Try<T> rethrow(Class<E1> exceptionType1, Class<E2> exceptionType2) throws E1, E2, RuntimeException, Error;

  public abstract <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Try<T> rethrow(Class<E1> exceptionType1, Class<E2> exceptionType2, Class<E3> exceptionType3) throws E1, E2, E3, RuntimeException, Error;

  public abstract Try<T> rethrowIfUnchecked() throws RuntimeException, Error;

  public abstract <E extends Throwable > Try<T> rethrowIfInstanceOf(Class<E> exceptionType) throws E, RuntimeException, Error;

  public abstract <E extends Throwable > Try<T> rethrowUnlessInstanceOf(Class<E> exceptionType) throws RuntimeException, Error;


  /*
   * utilities
   */

  public abstract Try<T> addException(@Nullable Throwable exception);

  /**
   * This is Java, so alas, we can't mark type parameters as covariant.
   * {@code Try} is immutable, so we can upcast safely. This method just makes
   * this clear and avoids spreading the {@code @SuppressWarnings("unchecked")}
   * all over the code.
   *
   * This needs to be static because we can't declare a type variable
   * {@code <R super T>}. We have to use {@code <? extends R>} instead for
   * the argument to ensure that the types are actually covariant.
   */
  @SuppressWarnings("unchecked")
  public static <R> Try<R> covariantCast(Try<? extends R> t) {
    return (Try<R>)t;
  }

  public static boolean isCheckedExceptionType(Class<? extends Throwable> exceptionType) {
    return RuntimeException.class.isAssignableFrom(exceptionType) || Error.class.isAssignableFrom(exceptionType);
  }

  public static boolean isCheckedException(Throwable exception) {
    return exception instanceof RuntimeException || exception instanceof Error;
  }

  public static AssertionError wrapCheckedException(Throwable exception) {
    staticRequireNonNullArg(exception, "exception");
    return new AssertionError("Unhandled checked exception: " + exception, exception);
  }

  /**
   * Rethrow errors that we should rethrow directly.
   *
   * @see #isFatalError(Throwable)
   */
  public static <T extends Throwable> T rethrowFatalError(T e) {
    return rethrowFatalError(e, null);
  }

  /**
   * Rethrow errors that we should rethrow directly. Adds the given
   * previously encountered exception as suppressed if not {@code null}.
   *
   * @see #isFatalError(Throwable)
   */
  public static <T extends Throwable> T rethrowFatalError(T exception, @Nullable Throwable prev) {
    Objects.requireNonNull(exception, "exception");
    if (isFatalError(exception)) {
      if (prev != null) {
        exception.addSuppressed(prev);
      }
      throw (Error)exception;
    } else {
      return exception;
    }
  }

  /**
   * Returns {@code true} if the given exception is a fatal error that
   * should be rethrown directly and without thinking.
   */
  @SuppressWarnings("RedundantIfStatement")
  public static boolean isFatalError(@Nullable Throwable exception) {
    // these are very fatal, don't even try to react, it may be dangerous
    // a well-configured JVM has dumped core by now
    if (exception instanceof VirtualMachineError) return true;
    // this is disputable, I know
    if (exception instanceof LinkageError) return true;
    // for the sake of completeness, Thread.stop() is deprecated for very good reasons
    // we should never, ever encounter a ThreadDeath, but if we do, we should rethrow it
    if (exception instanceof ThreadDeath) return true;
    return false;
  }

  @Nonnull
  @SuppressWarnings({"unchecked", "RedundantSuppression"})
  private Failure<T> asFailure() {
    if (!isFailure()) {
      throw new IllegalStateException("Not failure");
    }
    return (Failure<T>)this;
  }


  /*
   * internal utilities
   */

  protected T result() {
    return ((Success<T>)this).result;
  }

  protected Throwable exception() {
    return ((Failure<T>)this).exception;
  }

  protected <U> U requireNonNullArg(U ref, String message) {
    return requireNonNullArg(ref, message, null);
  }

  protected <U> U requireNonNullArg(U ref, String message, @Nullable Try<?> anotherTry) {
    if (ref == null) {
      throw throwProcessingException(new NullPointerException("arg:" + message), anotherTry);
    }
    return ref;
  }

  protected <U> U requireNonNullTransform(U ref, String message) {
    if (ref == null) {
      throw new NullPointerException("transform:" + message);
    }
    return ref;
  }

  private static <T> T staticRequireNonNullArg(T ref, String message) {
    if (ref == null) {
      throw new NullPointerException("arg:" + message);
    }
    return ref;
  }

  private static <T> T staticRequireNonNullResult(T ref, String message) {
    if (ref == null) {
      throw new NullPointerException("retval:" + message);
    }
    return ref;
  }

  /**
   * Always throws, never returns.
   */
  protected RuntimeException throwProcessingException(Throwable exception) {
    throw throwProcessingException(exception, null);
  }

  /**
   * Always throws, never returns.
   */
  protected RuntimeException throwProcessingException(Throwable exception, @Nullable Try<?> anotherTry) {
    Objects.requireNonNull(exception, "exception");
    if (isFailure()) {
      exception.addSuppressed(exception());
    }
    if (anotherTry != null && anotherTry.isFailure()) {
      exception.addSuppressed(Objects.requireNonNull(anotherTry.exception(), "anotherTry.exception()"));
    }
    if (exception instanceof RuntimeException) {
      throw (RuntimeException)exception;
    } else if (exception instanceof Error) {
      throw (Error)exception;
    } else {
      throw wrapCheckedException(exception);
    }
  }


  /*
   * helper types
   */

  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingRunnable {
    void run() throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;
  }

  public static final class IllegalTryStateException extends IllegalStateException {
    public IllegalTryStateException(Try<?> t, String method) {
      this(t, method, null);
    }
    public IllegalTryStateException(Try<?> t, @Nullable String method, @Nullable Throwable cause) {
      super(t + (method != null && !method.isEmpty() ? "::" + method : ""), cause);
    }
    @Override
    public String toString() {
      return getClass().getCanonicalName() + ": " + getMessage();
    }
  }

  public static final class Void {
    private Void() {}
    private static final Void INSTANCE = new Void();
    private static final Try<Void> VOID_SUCCESS = success(instance());
    public static Void instance() {
      return INSTANCE;
    }
    @Override
    public String toString() {
      return "Void";
    }
  }


  /*
   * implementation
   */

  public static final class Success<T> extends Try<T> {

    private final T result;

    private Success(T result) {
      assert result != null;
      this.result = result;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public <R> Try<R> map(Function<? super T, ? extends R> fun) {
      requireNonNullArg(fun, "fun");
      try {
        return success(requireNonNullTransform(fun.apply(result), "fun"));
      } catch (Throwable e) {
        throw throwProcessingException(e);
      }
    }

    @Override
    public <R> Try<R> flatMap(Function<? super T, ? extends Try<? extends R>> fun) {
      requireNonNullArg(fun, "fun");
      try {
        return covariantCast(requireNonNullTransform(fun.apply(result()), "fun"));
      } catch (Throwable e) {
        throw throwProcessingException(e);
      }
    }

    @Override
    public Try<T> filter(Predicate<? super T> fun) {
      requireNonNullArg(fun, "fun");
      try {
        return fun.test(result) ? this : failure(new IllegalStateException("Illegal result value: " + result));
      } catch (Throwable e) {
        throw throwProcessingException(e);
      }
    }

    @Override
    public void forEach(Consumer<T> fun) {
      requireNonNullArg(fun, "fun");
      fun.accept(result);
    }

    @Override
    public <E extends Throwable> Try<T> mapException(Class<E> exceptionType, Function<? super E, ? extends Throwable> fun) {
      requireNonNullArg(exceptionType, "exceptionType");
      requireNonNullArg(fun, "fun");
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> recover(Class<E> exceptionType, Function<? super E, ? extends T> fun) {
      requireNonNullArg(exceptionType, "exceptionType");
      requireNonNullArg(fun, "fun");
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> flatRecover(Class<E> exceptionType, Function<? super E, ? extends Try<? extends T>> fun) {
      requireNonNullArg(exceptionType, "exceptionType");
      requireNonNullArg(fun, "fun");
      return this;
    }

    @Override
    public <U, R> Try<R> fold(Try<U> that, BiFunction<? super T, ? super U, ? extends R> fun) {
      requireNonNullArg(that, "that");
      requireNonNullArg(fun, "fun", that);
      if (that.isFailure()) {
        return that.asFailure().passFailure();
      } else {
        try {
          return success(requireNonNullTransform(fun.apply(result, that.result()), "fun"));
        } catch (Throwable e) {
          throw throwProcessingException(e, that);
        }
      }
    }

    @Override
    public <U, R> Try<R> flatFold(Try<U> that, BiFunction<? super T, ? super U, ? extends Try<? extends R>> fun) {
      requireNonNullArg(that, "that");
      requireNonNullArg(fun, "fun", that);
      if (that.isFailure()) {
        return that.asFailure().passFailure();
      } else {
        try {
          return covariantCast(requireNonNullTransform(fun.apply(result, that.result()), "fun"));
        } catch (Throwable e) {
          throw throwProcessingException(e, that);
        }
      }
    }

    @Override
    public T get() {
      return result;
    }

    @Override
    public Throwable getException() throws IllegalTryStateException {
      throw new IllegalTryStateException(this, "getException()");
    }

    @Override
    public Try<T> rethrowIfUnchecked() throws RuntimeException, Error {
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> rethrowIfInstanceOf(Class<E> exceptionType) {
      requireNonNullArg(exceptionType, "exceptionType");
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> rethrowUnlessInstanceOf(Class<E> exceptionType) throws RuntimeException, Error {
      requireNonNullArg(exceptionType, "exceptionType");
      return this;
    }

    @Override
    public Try<T> rethrow() throws RuntimeException, Error {
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> rethrow(Class<E> exceptionType) {
      requireNonNullArg(exceptionType, "exceptionType");
      return this;
    }

    @Override
    public <E1 extends Throwable, E2 extends Throwable> Try<T> rethrow(Class<E1> exceptionType1, Class<E2> exceptionType2) {
      requireNonNullArg(exceptionType1, "exceptionType1");
      requireNonNullArg(exceptionType2, "exceptionType2");
      return this;
    }

    @Override
    public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Try<T> rethrow(Class<E1> exceptionType1, Class<E2> exceptionType2, Class<E3> exceptionType3) {
      requireNonNullArg(exceptionType1, "exceptionType1");
      requireNonNullArg(exceptionType2, "exceptionType2");
      requireNonNullArg(exceptionType3, "exceptionType3");
      return this;
    }

    @Override
    public Try<T> addException(@Nullable Throwable exception) {
      return exception == null ? this : failure(exception);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      //noinspection ObjectEquality
      if (o == null || getClass() != o.getClass()) return false;
      return result.equals(((Success<?>)o).result);
    }

    @Override
    public int hashCode() {
      return result.hashCode();
    }

    @Override
    public String toString() {
      return "Success[" + result + "]";
    }
  }

  public static final class Failure<T> extends Try<T> {

    private final Throwable exception;

    private Failure(Throwable exception) {
      assert exception != null;
      this.exception = exception;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public <R> Try<R> map(Function<? super T, ? extends R> fun) {
      requireNonNullArg(fun, "fun");
      return passFailure();
    }

    @Override
    public <R> Try<R> flatMap(Function<? super T, ? extends Try<? extends R>> fun) {
      requireNonNullArg(fun, "fun");
      return passFailure();
    }

    @Override
    public Try<T> filter(Predicate<? super T> fun) {
      requireNonNullArg(fun, "fun");
      return this;
    }

    @Override
    public void forEach(Consumer<T> fun) {
      requireNonNullArg(fun, "fun");
      throw new IllegalTryStateException(this, "forEach()" + this, exception);
    }

    @Override
    public <E extends Throwable> Try<T> mapException(Class<E> exceptionType, Function<? super E, ? extends Throwable> fun) {
      requireNonNullArg(exceptionType, "exceptionType");
      requireNonNullArg(fun, "fun");
      if (exceptionType.isInstance(exception)) {
        try {
          return failure(requireNonNullTransform(fun.apply(exceptionType.cast(exception)), "fun"));
        } catch (Throwable e) {
          throw throwProcessingException(e);
        }
      } else {
        return this;
      }
    }

    @Override
    public <E extends Throwable> Try<T> recover(Class<E> exceptionType, Function<? super E, ? extends T> fun) {
      requireNonNullArg(exceptionType, "exceptionType");
      requireNonNullArg(fun, "fun");
      if (exceptionType.isInstance(exception)) {
        try {
          return success(requireNonNullTransform(fun.apply(exceptionType.cast(exception)), "fun"));
        } catch (Throwable e) {
          throw throwProcessingException(e);
        }
      } else {
        return this;
      }
    }

    @Override
    public <E extends Throwable> Try<T> flatRecover(Class<E> exceptionType, Function<? super E, ? extends Try<? extends T>> fun) {
      requireNonNullArg(exceptionType, "exceptionType");
      requireNonNullArg(fun, "fun");
      if (exceptionType.isInstance(exception)) {
        try {
          return covariantCast(requireNonNullTransform(fun.apply(exceptionType.cast(exception)), "fun"));
        } catch (Throwable e) {
          throw throwProcessingException(e);
        }
      } else {
        return this;
      }
    }

    @Override
    public <U, R> Try<R> fold(Try<U> that, BiFunction<? super T, ? super U, ? extends R> fun) {
      return foldImpl(that, fun);
    }

    @Override
    public <U, R> Try<R> flatFold(Try<U> that, BiFunction<? super T, ? super U, ? extends Try<? extends R>> fun) {
      return foldImpl(that, fun);
    }

    private <U, R> Try<R> foldImpl(Try<U> that, Object fun) {
      requireNonNullArg(that, "that");
      requireNonNullArg(fun, "fun", that);
      if (that.isFailure()) {
        return addException(that.exception()).passFailure();
      } else {
        return this.passFailure();
      }
    }

    @Override
    public T get() throws IllegalTryStateException {
      throw new IllegalTryStateException(this, "get()", exception);
    }

    @Override
    public Throwable getException() {
      return exception;
    }

    @Override
    public Try<T> rethrowIfUnchecked() throws RuntimeException, Error {
      if (exception instanceof RuntimeException) throw (RuntimeException)exception;
      if (exception instanceof Error) throw (Error)exception;
      return this;
    }

    @Override
    public <E extends Throwable > Try<T> rethrowIfInstanceOf(Class<E> exceptionType) throws E, RuntimeException, Error {
      requireNonNullArg(exceptionType, "exceptionType");
      if (exceptionType.isInstance(exception)) throw exceptionType.cast(exception);
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> rethrowUnlessInstanceOf(Class<E> exceptionType) throws RuntimeException, Error {
      requireNonNullArg(exceptionType, "exceptionType");
      if (!exceptionType.isInstance(exception)) rethrow();
      return this;
    }

    @Override
    public Try<T> rethrow() throws RuntimeException, Error {
      rethrowIfUnchecked();
      throw wrapCheckedException(exception);
    }

    @Override
    public <E extends Throwable> Try<T> rethrow(Class<E> exceptionType) throws E, RuntimeException, Error {
      requireNonNullArg(exceptionType, "exceptionType");
      if (exception instanceof RuntimeException) throw (RuntimeException)exception;
      if (exception instanceof Error) throw (Error)exception;
      if (exceptionType.isInstance(exceptionType)) throw exceptionType.cast(exception);
      throw wrapCheckedException(exception);
    }

    @Override
    public <E1 extends Throwable, E2 extends Throwable> Try<T> rethrow(Class<E1> exceptionType1, Class<E2> exceptionType2) throws E1, E2, RuntimeException, Error {
      requireNonNullArg(exceptionType1, "exceptionType1");
      requireNonNullArg(exceptionType2, "exceptionType2");
      if (exception instanceof RuntimeException) throw (RuntimeException)exception;
      if (exception instanceof Error) throw (Error)exception;
      if (exceptionType1.isInstance(exceptionType1)) throw exceptionType1.cast(exception);
      if (exceptionType2.isInstance(exceptionType2)) throw exceptionType2.cast(exception);
      throw wrapCheckedException(exception);
    }

    @Override
    public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> Try<T> rethrow(Class<E1> exceptionType1, Class<E2> exceptionType2, Class<E3> exceptionType3) throws E1, E2, E3, RuntimeException, Error {
      requireNonNullArg(exceptionType1, "exceptionType1");
      requireNonNullArg(exceptionType2, "exceptionType2");
      requireNonNullArg(exceptionType3, "exceptionType3");
      if (exception instanceof RuntimeException) throw (RuntimeException)exception;
      if (exception instanceof Error) throw (Error)exception;
      if (exceptionType1.isInstance(exceptionType1)) throw exceptionType1.cast(exception);
      if (exceptionType2.isInstance(exceptionType2)) throw exceptionType2.cast(exception);
      if (exceptionType3.isInstance(exceptionType3)) throw exceptionType3.cast(exception);
      throw wrapCheckedException(exception);
    }

    @Override
    public Failure<T> addException(@Nullable Throwable exception) {
      if (exception != null) {
        this.exception.addSuppressed(rethrowFatalError(exception, this.exception));
      }
      return this.passFailure();
    }

    @SuppressWarnings("unchecked")
    private <R> Failure<R> passFailure() {
      // failures have no result, so we can safely ignore the type parameter and cross-cast as we please
      return (Failure<R>)this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      //noinspection ObjectEquality
      if (o == null || getClass() != o.getClass()) return false;
      return exception.equals(((Failure<?>)o).exception);
    }

    @Override
    public int hashCode() {
      return exception.hashCode();
    }

    @Override
    public String toString() {
      return "Failure[" + exception + "]";
    }
  }

  private static class FoldingCollector<T, U> implements Collector<Try<T>, FoldingCollector.State<T, U>, Try<U>> {

    private final Collector<T, ?, U> downstreamCollector;

    private FoldingCollector(Collector<T, ?, U> downstreamCollector) {
      this.downstreamCollector = downstreamCollector;
    }

    @Override
    public Supplier<State<T, U>> supplier() {
      return () -> new State<>(downstreamCollector);
    }
    @Override
    public BiConsumer<State<T, U>, Try<T>> accumulator() {
      return State::accumulate;
    }
    @Override
    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public BinaryOperator<State<T, U>> combiner() {
      return State::combine;
    }
    @Override
    public Function<State<T, U>, Try<U>> finisher() {
      return State::finish;
    }
    @Override
    public Set<Characteristics> characteristics() {
      return Collections.emptySet();
    }

    private final static class State<T, U> {
      final Collector<T, Object, U> downstreamCollector;
      Object downstreamState;
      @Nullable
      ArrayList<Throwable> exceptions = null;

      @SuppressWarnings("unchecked")
      private State(Collector<T, ?, U> downstreamCollector) {
        this.downstreamCollector = (Collector<T, Object, U>)downstreamCollector;
        this.downstreamState = downstreamCollector.supplier().get();
      }

      private void accumulate(Try<T> element) {
        if (element.isFailure()) {
          if (exceptions == null) {
            exceptions = new ArrayList<>();
          }
          exceptions.add(element.getException());
        } else if (!failing()) {
          downstreamCollector.accumulator().accept(downstreamState, element.get());
        }
      }

      private State combine(State<T, U> that) {
        if (that.exceptions != null) {
          if (exceptions == null) {
            exceptions = new ArrayList<>(that.exceptions);
          } else {
            exceptions.addAll(that.exceptions);
          }
        }
        if (!failing()) {
          downstreamState = downstreamCollector.combiner().apply(downstreamState, that.downstreamState);
        }
        return this;
      }

      private Try<U> finish() {
        if (failing()) {
          assert exceptions != null;
          Throwable folded = null;
          for (Throwable exception : exceptions) {
            if (folded == null) {
              folded = exception;
            } else {
              folded.addSuppressed(exception);
            }
          }
          assert folded != null;
          return failure(folded);
        } else {
          return success(downstreamCollector.finisher().apply(downstreamState));
        }
      }

      private boolean failing() {
        assert exceptions == null || !exceptions.isEmpty();
        return exceptions != null;
      }
    }
  }

  private static final class AutoCloseableWrapper<T> implements AutoCloseable {
    private final T resource;
    private final ThrowingConsumer<? super T> closeFun;
    private AutoCloseableWrapper(T resource, ThrowingConsumer<? super T> closeFun) {
      this.resource = resource;
      this.closeFun = closeFun;
    }
    @Override
    public void close() throws Exception {
      closeFun.accept(resource);
    }
  }

}
