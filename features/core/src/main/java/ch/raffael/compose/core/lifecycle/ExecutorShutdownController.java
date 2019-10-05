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

package ch.raffael.compose.core.lifecycle;

import io.vavr.CheckedRunnable;
import io.vavr.Lazy;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static io.vavr.API.*;

/**
 * A shutdown controller that allows to parallelize the shutdown.
 *
 * <p>The <em>prepare</em> and <em>perform</em> callbacks are called in
 * parallel, but not overlapping (i.e. <em>peform</em> callbacks will only
 * be called after all <em>prepare</em> callbacks are finished.
 *
 * <p>The <em>finalize</em> callback will be called serially the thread
 * initiating the shutdown (finalizing the shutdown usually includes to
 * destroying the work thread pool).
 */
public class ExecutorShutdownController implements ShutdownController {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutorShutdownController.class);

  private final Lazy<Executor> executor;

  private Seq<CheckedRunnable> prepareCallbacks = Seq();
  private Seq<CheckedRunnable> performCallbacks = Seq();
  private Seq<CheckedRunnable> finalizeCallbacks = Seq();

  private final Object sync = new Object();
  private volatile State state = State.DORMANT;
  private int preventDepth = 0;
  private final CompletableFuture<Seq<Throwable>> shutdownFuture = new CompletableFuture<>();

  public ExecutorShutdownController(Supplier<? extends Executor> executor) {
    this.executor = Lazy.of(executor);
  }

  @Override
  public void onPrepare(CheckedRunnable callback) {
    synchronized (sync) {
      state.checkStateBefore(State.PREPARING);
      prepareCallbacks = prepareCallbacks.prepend(callback);
    }
  }

  @Override
  public void onPerform(CheckedRunnable callback) {
    synchronized (sync) {
      state.checkStateBefore(State.PERFORMING);
      performCallbacks = performCallbacks.prepend(callback);
    }
  }

  @Override
  public void onFinalize(CheckedRunnable callback) {
    synchronized (sync) {
      state.checkStateBefore(State.FINALIZING);
      finalizeCallbacks = finalizeCallbacks.prepend(callback);
    }
  }

  @Override
  public <T> T getPreventingShutdown(Supplier<T> supplier) {
    synchronized (sync) {
      state.checkStateBeforeOrEqual(State.DORMANT);
      preventDepth++;
    }
    try {
      return supplier.get();
    } finally {
      synchronized (sync) {
        preventDepth--;
        sync.notifyAll();
      }
    }
  }

  public State state() {
    return state;
  }

  public void announceShutdown() {
    synchronized (sync) {
      if (state.isBefore(State.ANNOUNCED)) {
        state = State.ANNOUNCED;
      }
    }
  }

  public CompletableFuture<Seq<Throwable>> initiateShutdown() {
    var doShutdown = false;
    synchronized (sync) {
      if (state.isBefore(State.INITIATED)) {
        state = State.INITIATED;
      }
      boolean preventLogged = false;
      while (preventDepth > 0) {
        if (preventLogged) {
          LOG.trace("Awaiting {} more shutdown preventing actions", preventDepth);
        } else {
          LOG.debug("Awaiting {} shutdown preventing actions", preventDepth);
          preventLogged = true;
        }
        try {
          sync.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return CompletableFuture.failedFuture(e);
        }
      }
      if (state == State.INITIATED) {
        // set the PREPARING state right now, not in doShutdown()
        state = State.PREPARING;
        doShutdown = true;
      }
    }
    if (doShutdown) {
      try {
        shutdownFuture.complete(doShutdown());
      } catch (Exception e) {
        shutdownFuture.completeExceptionally(e);
      }
    }
    return shutdownFuture;
  }

  protected void onShutdownComplete(Seq<Throwable> exceptions) {
    if (exceptions.isEmpty()) {
      LOG.info("Shutdown completed successfully");
    } else {
      LOG.error("Shutdown completed with errors: {}", exceptions);
    }
  }

  protected void onDoShutdown() {
    LOG.info("Performing shutdown");
  }

  private Seq<Throwable> doShutdown() throws InterruptedException {
    try {
      onDoShutdown();
      Seq<Throwable> exceptions = Seq();
      Seq<CheckedRunnable> callbacks;
      synchronized (sync) {
        // state PREPARING was set in initiateShutdown()
        callbacks = prepareCallbacks;
      }
      exceptions = runCallbacks(executor.get(), exceptions, "prepare", callbacks);
      synchronized (sync) {
        state = State.PERFORMING;
        callbacks = performCallbacks;
      }
      exceptions = runCallbacks(executor.get(), exceptions, "perform", callbacks);
      synchronized (sync) {
        state = State.FINALIZING;
        callbacks = finalizeCallbacks;
      }
      exceptions = runCallbacks(Runnable::run, exceptions, "finalize", callbacks);
      onShutdownComplete(exceptions);
      return exceptions;
    } finally {
      state = State.COMPLETE;
    }
  }

  private Seq<Throwable> runCallbacks(Executor executor, Seq<Throwable> exceptions, String phase,
                                      Seq<CheckedRunnable> callbacks)
      throws InterruptedException {
    if (!callbacks.isEmpty()) {
      CountDownLatch latch = new CountDownLatch(callbacks.size());
      var exceptionsRef = new AtomicReference<>(exceptions);
      callbacks.forEach(cb -> executor.execute(() -> {
        try {
          cb.run();
        } catch (Throwable e) {
          LOG.error("Shutdown {} callback failed: {}", phase, cb, e);
          exceptionsRef.updateAndGet(s -> s.append(e));
        } finally {
          latch.countDown();
        }
      }));
      latch.await();
      return exceptionsRef.get();
    } else {
      return exceptions;
    }
  }
}
