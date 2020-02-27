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

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Executor that runs all tasks in the submitting thread.
 *
 * <p>It does, however, honor the contract of executor services. Several
 * tasks can be run concurrently (when submitted from multiple threads), it
 * doesn't accept any new work on shutdown, {@link #awaitTermination(long,
 * TimeUnit) awaitTermination()} waits until all tasks are done.
 */
public class SameThreadExecutorService implements ExecutorService {

  private final AtomicReference<State> state = new AtomicReference<>(new State(0, false));
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  @Override
  public void execute(Runnable command) {
    beginExecution();
    try {
      command.run();
    } finally {
      endExecution();
    }
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    beginExecution();
    try {
      return ImmediateFuture.ofCallable(task);
    } finally {
      endExecution();
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, @Nullable T result) {
    beginExecution();
    try {
      return submit(() -> {
        task.run();
        return result;
      });
    } finally {
      endExecution();
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    beginExecution();
    try {
      return ImmediateFuture.runRunnable(task);
    } finally {
      endExecution();
    }
  }

  private void beginExecution() {
    state.updateAndGet(State::beginExecution);
  }

  private void endExecution() {
    state.updateAndGet(State::endExecution);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
    return tasks.stream().map(this::submit).collect(Collectors.toList());
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    Duration timeoutDuration;
    switch (unit) {
      case NANOSECONDS:
        timeoutDuration = Duration.ofNanos(timeout);
        break;
      case MICROSECONDS:
        timeoutDuration = Duration.ofNanos(unit.toNanos(timeout));
        break;
      case MILLISECONDS:
        //noinspection DuplicateBranchesInSwitch
        timeoutDuration = Duration.ofMillis(timeout);
        break;
      case SECONDS:
        timeoutDuration = Duration.ofSeconds(timeout);
        break;
      case MINUTES:
        timeoutDuration = Duration.ofMinutes(timeout);
        break;
      case HOURS:
        timeoutDuration = Duration.ofHours(timeout);
        break;
      case DAYS:
        timeoutDuration = Duration.ofDays(timeout);
        break;
      default:
        timeoutDuration = Duration.ofMillis(timeout);
        break;
    }
    Instant until = Instant.now().plus(timeoutDuration);
    return tasks.stream()
        .map(t -> Instant.now().compareTo(until) <= 0 ? submit(t) : ImmediateFuture.<T>cancelled())
        .collect(Collectors.toList());
  }

  @Override
  @Nullable
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
    return invokeAll(tasks).stream()
        .map(f -> (ImmediateFuture<T>)f)
        .filter(f -> f.result().isSuccess())
        .findFirst()
        .map(f -> f.result().get())
        .orElse(null);
  }

  @Override
  @Nullable
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    return invokeAll(tasks, timeout, unit).stream()
        .filter(f -> f instanceof ImmediateFuture && !f.isCancelled())
        .map(f -> (ImmediateFuture<T>)f)
        .filter(f -> f.result().isSuccess())
        .findFirst()
        .map(f -> f.result().get())
        .orElse(null);
  }

  @Override
  public void shutdown() {
    state.updateAndGet(State::shutdown);
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown() {
    return state.get().shutdown;
  }

  @Override
  public boolean isTerminated() {
    return shutdownLatch.getCount() == 0;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return shutdownLatch.await(timeout, unit);
  }

  private class State {
    private final int executing;
    private final boolean shutdown;
    private State(int executing, boolean shutdown) {
      this.executing = executing;
      this.shutdown = shutdown;
    }
    private State beginExecution() {
      if (shutdown) {
        throw new IllegalStateException(SameThreadExecutorService.this + " is shutting down");
      } else {
        return new State(executing + 1, false);
      }
    }
    private State endExecution() {
      return new State(executing - 1, shutdown).checkShutdownComplete();
    }
    private State shutdown() {
      return (shutdown ? this : new State(executing, true)).checkShutdownComplete();
    }
    private State checkShutdownComplete() {
      if (shutdown && executing == 0) {
        shutdownLatch.countDown();
      }
      return this;
    }
  }

}
