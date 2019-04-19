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

package ch.raffael.compose.core.shutdown;

import io.vavr.CheckedRunnable;
import io.vavr.Lazy;
import io.vavr.collection.Seq;
import io.vavr.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

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

  private final Object shutdownLock = new Object();
  private final DefaultShutdownCallbackRegistry callbackRegistry = new DefaultShutdownCallbackRegistry();
  private final Lazy<Executor> executor;

  @Nullable
  private volatile Future<Void> shutdownFuture = null;

  public ExecutorShutdownController(Supplier<? extends Executor> executor) {
    this.executor = Lazy.of(executor);
  }

  @Override
  public void onPrepare(CheckedRunnable callback) {
    callbackRegistry.onPrepare(callback);
  }

  @Override
  public void onPerform(CheckedRunnable callback) {
    callbackRegistry.onPerform(callback);
  }

  @Override
  public void onFinalize(CheckedRunnable callback) {
    callbackRegistry.onFinalize(callback);
  }

  public Future<Void> performShutdown() {
    Future<Void> shutdown;
    if ((shutdown = shutdownFuture) == null) {
      synchronized (shutdownLock) {
        if ((shutdown = shutdownFuture) == null) {
          shutdown = shutdownFuture = Future.of(Runnable::run, () -> {
            doShutdown();
            return null;
          });
        } else {
          LOG.warn("Shutdown already in progress", new Exception("Stack trace"));
        }
      }
    }
    assert shutdown != null;
    return shutdown;
  }

  private void doShutdown() {
    onInitiateShutdown();
    runCallbacks(executor.get(), "prepare", callbackRegistry.onPrepareCallbacks());
    runCallbacks(executor.get(), "perform", callbackRegistry.onPerformCallbacks());
    callbackRegistry.onFinalizeCallbacks().forEach(c -> runCallback("finalize", c));
    onShutdownComplete();
  }

  protected void onShutdownComplete() {
    LOG.info("Shutdown complete");
  }

  protected void onInitiateShutdown() {
    LOG.info("Initiating shutdown");
  }

  private void runCallbacks(Executor executor, String phase, Seq<CheckedRunnable> callbacks) {
    Future.sequence(callbacks.map(c -> Future.of(executor, () -> runCallback(phase, c)))).await();
  }

  private Void runCallback(String phase, CheckedRunnable callback) {
    try {
      callback.run();
    } catch (Throwable e) {
      LOG.error("Shutdown {} callback failed: {}", phase, callback, e);
    }
    return null;
  }

}
