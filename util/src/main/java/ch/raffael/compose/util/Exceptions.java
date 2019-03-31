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

import io.vavr.control.Option;
import io.vavr.control.Try;


/**
 * TODO javadoc
 */
public final class Exceptions {

  private Exceptions() {
  }

  public static boolean isFatal(Throwable exception) {
    return exception instanceof InterruptedException
        || exception instanceof VirtualMachineError
        || exception instanceof LinkageError;
  }

  public static <E extends Throwable> E rethrowIfFatal(E exception) {
    if (isFatal(exception)) {
      if (exception instanceof InterruptedException) {
        throw InterruptedRuntimeException.rethrow((InterruptedException) exception);
      } else {
        throw (Error) exception;
      }
    }
    return exception;
  }

  public static <E extends Throwable, T extends Throwable> E rethrowIfChecked(E exception, Class<T> t) throws T {
    if (t.isInstance(exception)) {
      throw t.cast(exception);
    } else {
      return exception;
    }
  }

  public static <E extends Throwable, T1 extends Throwable, T2 extends Throwable>
  E rethrowIfChecked(E exception, Class<T1> t1, Class<T2> t2) throws T1, T2 {
    if (t1.isInstance(exception)) {
      throw t1.cast(exception);
    } else if (t2.isInstance(exception)) {
      throw t2.cast(exception);
    } else {
      return exception;
    }
  }

  public static <E extends Throwable, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable>
  E rethrowIfChecked(E exception, Class<T1> t1, Class<T2> t2, Class<T3> t3) throws T1, T2, T3 {
    if (t1.isInstance(exception)) {
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
    if (exception instanceof RuntimeException) {
      throw (RuntimeException) exception;
    } else if (exception instanceof Error) {
      throw (Error)exception;
    } else {
      return exception;
    }
  }

  public static IllegalFlow forceRethrow(Throwable exception) {
    throw IllegalFlow.unexpectedException(rethrowIfUnchecked(exception));
  }

  public static <T extends Throwable> IllegalFlow forceRethrow(Throwable exception, Class<T> t) throws T {
    throw forceRethrow(rethrowIfChecked(exception, t));
  }

  public static <T1 extends Throwable, T2 extends Throwable>
  IllegalFlow forceRethrow(Throwable exception, Class<T1> t1, Class<T2> t2) throws T1, T2 {
    throw forceRethrow(rethrowIfChecked(exception, t1, t2));
  }

  public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable>
  IllegalFlow forceRethrow(Throwable exception, Class<T1> t1, Class<T2> t2, Class<T3> t3) throws T1, T2, T3 {
    throw forceRethrow(rethrowIfChecked(exception, t1, t2, t3));
  }

  public static Throwable accumulate(Option<Throwable> current, Throwable exception) {
    return current.map(e -> {
      e.addSuppressed(exception);
      return e;
    }).getOrElse(exception);
  }

  public static <T> Try<T> accumulate(Try<T> t, Throwable exception) {
    if (t.isFailure()) {
      t.getCause().addSuppressed(exception);
      return t;
    } else {
      return Try.failure(exception);
    }
  }

}
