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

import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Feature;
import ch.raffael.compose.Feature.DependsOn;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.threading.ThreadingFeature;
import ch.raffael.compose.util.Exceptions;
import io.vavr.CheckedRunnable;
import io.vavr.collection.Seq;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ch.raffael.compose.logging.Logging.logger;
import static io.vavr.API.*;

@Feature
public abstract class LifecycleFeature implements ShutdownFeature {

  private static final Logger LOG = logger();

  private final AtomicBoolean startupInitiated = new AtomicBoolean(false);
  private final CompletableFuture<Seq<Throwable>> startupFuture = new CompletableFuture<>();
  protected final StartupActions.Default startupActions = new StartupActions.Default();

  protected LifecycleFeature() {
  }

  @ExtensionPoint
  protected StartupActions startupActionsEP() {
    return startupActions;
  }

  @Provision(shared = true)
  @Override
  public abstract ExecutorShutdownController shutdownController();

  public abstract CompletableFuture<Seq<Throwable>> start();

  protected CompletableFuture<Seq<Throwable>> start(Executor executor) {
    if (!startupInitiated.compareAndSet(false, true)) {
      return startupFuture;
    }
    try {
      var errors = new AtomicReference<Seq<Throwable>>(Seq());
      var counter = new AtomicInteger(0);
      startupActions.startupActions().forEach(a -> {
        counter.incrementAndGet();
        executor.execute(() -> {
          outerStartupAction(counter, errors, a);
        });
      });
    } catch (Exception e) {
      startupFuture.completeExceptionally(e);
    }
    return startupFuture;
  }

  private void outerStartupAction(AtomicInteger counter, AtomicReference<Seq<Throwable>> errors, CheckedRunnable a) {
    try {
      if (!errors.get().isEmpty()) {
        LOG.debug("Skipping startup action {} because startup is being aborted", a);
      }
      shutdownController().runPreventingShutdown(() -> innerStartupAction(errors, a));
    } finally {
      if (counter.decrementAndGet() == 0) {
        startupFuture.complete(errors.get());
      }
    }
  }

  private void innerStartupAction(AtomicReference<Seq<Throwable>> errors, CheckedRunnable a) {
    try {
      a.run();
    } catch (Throwable e) {
      try {
        errors.updateAndGet(errs -> errs.append(e));
      } catch (Throwable e2) {
        Exceptions.rethrowIfFatal(e2, e);
      }
      Exceptions.rethrowIfFatal(e);
    }
  }

  @Feature
  public abstract static class WithThreading extends LifecycleFeature implements @DependsOn ThreadingFeature {
    public WithThreading() {
    }

    @Provision(shared = true)
    @Override
    public ExecutorShutdownController shutdownController() {
      return new ExecutorShutdownController(this::workExecutor);
    }

    @Override
    public CompletableFuture<Seq<Throwable>> start() {
      return start(workExecutor());
    }
  }

  @Feature
  public abstract static class SameThread extends LifecycleFeature {
    public SameThread() {
    }

    @Provision(shared = true)
    @Override
    public ExecutorShutdownController shutdownController() {
      return new ExecutorShutdownController(() -> Runnable::run);
    }

    @Override
    public CompletableFuture<Seq<Throwable>> start() {
      return start(Runnable::run);
    }
  }
}
