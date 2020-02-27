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

package ch.raffael.meldioc.util.concurrent;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Future} implementation that's run immediately in the
 * constructing thread. Instances of this are always completed.
 */
public final class ImmediateFuture<T> implements Future<T> {

  private static final Cancelled<?> CANCELLED = new Cancelled<>();

  private final Try<T> result;

  private ImmediateFuture(Try<T> result) {
    this.result = result;
  }

  public static <T> ImmediateFuture<T> of(Try<T> result) {
    return new ImmediateFuture<>(result);
  }

  public static <T> ImmediateFuture<T> of(CheckedFunction0<T> supplier) {
    return new ImmediateFuture<>(Try.of(supplier));
  }

  public static ImmediateFuture<Void> run(CheckedRunnable runnable) {
    return new ImmediateFuture<>(Try.run(runnable));
  }

  public static ImmediateFuture<Void> runRunnable(Runnable runnable) {
    return new ImmediateFuture<>(Try.runRunnable(runnable));
  }

  public static <T> ImmediateFuture<T> ofCallable(Callable<T> callable) {
    return new ImmediateFuture<>(Try.ofCallable(callable));
  }

  @SuppressWarnings("unchecked")
  public static <T> Cancelled<T> cancelled() {
    return (Cancelled<T>) CANCELLED;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public T get() throws ExecutionException {
    if (result.isFailure()) {
      throw new ExecutionException(result.getCause());
    } else {
      return result.get();
    }
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws ExecutionException {
    return get();
  }

  public Try<T> result() {
    return result;
  }

  public static final class Cancelled<T> implements Future<T> {

    private Cancelled() {
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return true;
    }

    @Override
    public boolean isDone() {
      return true;
    }

    @Override
    public T get() {
      throw new CancellationException();
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
      return get();
    }
  }

}
