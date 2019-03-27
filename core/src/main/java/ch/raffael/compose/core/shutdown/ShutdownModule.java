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

import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.threading.ThreadingModule;
import ch.raffael.compose.util.Exceptions;
import io.vavr.CheckedRunnable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Traversable;
import io.vavr.concurrent.Future;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

/**
 * @since 2019-03-18
 */
@Module
public interface ShutdownModule {

  @Provision
  ShutdownCoordinator shutdownCoordinator();

  @Module
  class Basic implements ShutdownModule {
    protected final DefaultShutdownCoordinator shutdownCallbacks = new DefaultShutdownCoordinator();

    @Provision(shared = true)
    public ShutdownCoordinator shutdownCoordinator() {
      return shutdownCallbacks;
    }
  }

  @Module
  abstract class WithExecutor extends Basic {

    private static final Logger LOG = LoggerFactory.getLogger(WithExecutor.class);

    protected abstract Executor shutdownCallbackExecutor();
    private final Object shutdownLock = new Object();
    @Nullable
    private volatile Future<Void> shutdownFuture = null;

    public Future<Void> performShutdown() {
      Future<Void> shutdown;
      if ((shutdown = shutdownFuture) == null) {
        synchronized (shutdownLock) {
          if ((shutdown = shutdownFuture) == null) {
            shutdown = shutdownFuture = Future.of(() -> {
              doShutdown();
              return null;
            });
          }
        }
      }
      assert shutdown != null;
      return shutdown;
    }

    private void doShutdown() throws InterruptedException {
      onInitiateShutdown();
      Executor executor = shutdownCallbackExecutor();
      runCallbacks(executor, "prepare", shutdownCallbacks.onPrepareCallbacks());
      runCallbacks(executor, "perform", shutdownCallbacks.onPerformCallbacks());
      shutdownCallbacks.onFinalizeCallbacks().forEach((callback) -> Try.run(callback)
          .onFailure(e -> LOG.error("Shutdown finalize callback failed", e)));
      onShutdownComplete();
    }

    protected void onShutdownComplete() {
      LOG.info("Shutdown complete");
    }

    protected void onInitiateShutdown() {
      LOG.info("Initiating shutdown");
    }

    private void runCallbacks(Executor executor, String phase, Traversable<CheckedRunnable> callbacks) throws InterruptedException {
      ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
      var futures = callbacks.map(c -> Tuple.of(completionService.submit(() -> {
        try {
          c.run();
          return null;
        } catch (Throwable e) {
          throw Exceptions.forceRethrow(e, Exception.class);
        }
      }), c)).toMap(Tuple2::_1, Tuple2::_2).toJavaMap();
      while (!futures.isEmpty()) {
        var done = completionService.take();
        CheckedRunnable runnable = futures.remove(done);
        if (done.isCancelled()) {
          LOG.warn("Shutdown {} callback cancelled: ", phase, runnable);
        } else {
          try {
            done.get();
          } catch (ExecutionException e) {
            LOG.error("Shutdown {} callback failed", phase, runnable, e);
          }
        }
      }
    }
  }

  @Module
  abstract class WithThreadingWorker extends WithExecutor implements @Module.DependsOn ThreadingModule {
    @Override
    protected Executor shutdownCallbackExecutor() {
      return workExecutor();
    }
  }

}
