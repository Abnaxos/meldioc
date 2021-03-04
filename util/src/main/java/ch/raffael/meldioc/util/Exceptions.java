/*
 *  Copyright (c) 2021 Raffael Herzog
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

package ch.raffael.meldioc.util;

import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;


/**
 * Some utilities for dealing with exceptions. All these methods make sure
 * that {@link #isFatal(Throwable) fatal exceptions} are always rethrown
 * immediately.
 */
public final class Exceptions {

  private Exceptions() {
  }

  /**
   * {@link VirtualMachineError} and {@link LinkageError} are considered
   * fatal exceptions that no sane program should try to handle. The best
   * reaction to these exceptions is to crash miserably.
   */
  public static boolean isFatal(Throwable exception) {
    return exception instanceof VirtualMachineError
        || exception instanceof LinkageError;
  }

  public static <E extends Throwable> E rethrowIfFatal(E exception) {
    return rethrowIfFatal(exception, null);
  }

  @SuppressWarnings("ObjectEquality")
  public static <E extends Throwable> E rethrowIfFatal(E exception, @Nullable Throwable previous) {
    if (isFatal(exception)) {
      if (previous != null && previous != exception) {
        exception.addSuppressed(previous);
      }
      throw (Error) exception;
    }
    return exception;
  }

  public static <E extends Throwable, T extends Throwable> E rethrowIfChecked(E exception, Class<T> t) throws T {
    if (t.isInstance(rethrowIfFatal(exception))) {
      throw t.cast(exception);
    } else {
      return exception;
    }
  }

  public static <E extends Throwable, T1 extends Throwable, T2 extends Throwable>
  E rethrowIfChecked(E exception, Class<T1> t1, Class<T2> t2) throws T1, T2 {
    if (t1.isInstance(rethrowIfFatal(exception))) {
      throw t1.cast(exception);
    } else if (t2.isInstance(exception)) {
      throw t2.cast(exception);
    } else {
      return exception;
    }
  }

  public static <E extends Throwable, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable>
  E rethrowIfChecked(E exception, Class<T1> t1, Class<T2> t2, Class<T3> t3) throws T1, T2, T3 {
    if (t1.isInstance(rethrowIfFatal(exception))) {
      throw t1.cast(exception);
    } else if (t2.isInstance(exception)) {
      throw t2.cast(exception);
    } else if (t3.isInstance(exception)) {
      throw t3.cast(exception);
    } else {
      return exception;
    }
  }

  public static <E extends Throwable> E rethrowIfUnchecked(E exception) throws RuntimeException, Error{
    if (rethrowIfFatal(exception) instanceof RuntimeException) {
      throw (RuntimeException) exception;
    } else if (exception instanceof Error) {
      throw (Error)exception;
    } else {
      return exception;
    }
  }

  public static IllegalFlow alwaysRethrow(Throwable exception) {
    throw IllegalFlow.unexpectedException(rethrowIfUnchecked(exception));
  }

  public static <T extends Throwable> IllegalFlow alwaysRethrow(Throwable exception, Class<T> t) throws T {
    throw alwaysRethrow(rethrowIfChecked(exception, t));
  }

  public static <T1 extends Throwable, T2 extends Throwable>
  IllegalFlow alwaysRethrow(Throwable exception, Class<T1> t1, Class<T2> t2) throws T1, T2 {
    throw alwaysRethrow(rethrowIfChecked(exception, t1, t2));
  }

  public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable>
  IllegalFlow alwaysRethrow(Throwable exception, Class<T1> t1, Class<T2> t2, Class<T3> t3) throws T1, T2, T3 {
    throw alwaysRethrow(rethrowIfChecked(exception, t1, t2, t3));
  }

  public static <T extends Throwable> IllegalFlow alwaysRethrow(Throwable exception, Class<T> t, Function<? super Throwable, ? extends T> wrapper) throws T {
    rethrowIfChecked(exception, t);
    throw wrapper.apply(exception);
  }

  public static <T1 extends Throwable, T2 extends Throwable>
  IllegalFlow alwaysRethrow(Throwable exception, Class<T1> t1, Class<T2> t2, Function<? super Throwable, ? extends T1> wrapper) throws T1, T2 {
    rethrowIfChecked(exception, t1, t2);
    throw wrapper.apply(exception);
  }

  public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable>
  IllegalFlow alwaysRethrow(Throwable exception, Class<T1> t1, Class<T2> t2, Class<T3> t3, Function<? super Throwable, ? extends T1> wrapper) throws T1, T2, T3 {
    rethrowIfChecked(exception, t1, t2, t3);
    throw wrapper.apply(exception);
  }

  public static String toString(Throwable e) {
    return toString(e, true);
  }

  public static String toString(Throwable e, boolean stackTrace) {
    if (stackTrace) {
      StringWriter string = new StringWriter();
      PrintWriter print = new PrintWriter(string);
      e.printStackTrace(print);
      print.flush();
      return string.toString();
    } else {
      return e.toString();
    }
  }

  @Deprecated
  public static Throwable accumulate(Option<Throwable> current, Throwable exception) {
    return accumulate(current.getOrNull(), exception);
  }

  public static Throwable accumulate(@Nullable Throwable current, Throwable exception) {
    if (current == null) {
      return exception;
    }
    if (isFatal(exception) && !isFatal(current)) {
      exception.addSuppressed(current);
      return exception;
    } else {
      current.addSuppressed(exception);
      return current;
    }
  }

  public static <T> Try<T> accumulate(Try<T> t, Throwable exception) {
    if (t.isFailure()) {
      t.getCause().addSuppressed(exception);
      return t;
    } else {
      return Try.failure(exception);
    }
  }

  public static <E extends Exception> AutoCloser<E> autoClose(AutoCloser<E> autoCloser) {
    return autoCloser;
  }

  @FunctionalInterface
  public interface AutoCloser<E extends Exception> extends AutoCloseable {
    void close() throws E;
  }
}
