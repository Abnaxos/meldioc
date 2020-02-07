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

package ch.raffael.compose.library.base.lifecycle;

import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Feature;
import ch.raffael.compose.Feature.DependsOn;
import ch.raffael.compose.Provision;
import ch.raffael.compose.library.base.threading.ThreadingFeature;
import ch.raffael.compose.util.Exceptions;
import ch.raffael.compose.util.IllegalFlow;
import io.vavr.CheckedRunnable;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ch.raffael.compose.logging.Logging.logger;
import static io.vavr.API.*;

@Feature
public abstract class LifecycleFeature implements ShutdownFeature {

  private static final Logger LOG = logger();

  private final AtomicReference<CountDownLatch> startupLatch = new AtomicReference<>(null);
  protected final StartupActions.Default startupActions = new StartupActions.Default();

  protected LifecycleFeature() {
  }

  @ExtensionPoint
  protected StartupActions startupActionsEP() {
    return startupActions;
  }

  @Provision(shared = true)
  @Override
  public abstract ShutdownController shutdownController();

  @Provision
  public ShutdownController.Actuator shutdownControllerHandle() {
    return ((ExecutorShutdownController) shutdownController()).actuator();
  }

  public Seq<Throwable> start() throws InterruptedException {
    try {
      return start(0);
    } catch (TimeoutException e) {
      throw IllegalFlow.unexpectedException(e);
    }
  }

  public Seq<Throwable> start(long timeoutSeconds) throws TimeoutException, InterruptedException {
    return start(timeoutSeconds, TimeUnit.SECONDS);
  }

  public Seq<Throwable> start(long timeout, TimeUnit timeoutUnit) throws TimeoutException, InterruptedException {
    return start(executor(), timeout, timeoutUnit);
  }

  protected abstract Executor executor();

  private Seq<Throwable> start(Executor executor, long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
    if (!startupLatch.compareAndSet(null, new CountDownLatch(1))) {
      throw new IllegalStateException("Startup already initiated");
    }
    Seq<Throwable> errors;
    try {
      errors = shutdownControllerHandle().getPreventingShutdown(() -> innerStart(executor, timeout, timeoutUnit)).get();
    } catch (Throwable e) {
      LOG.error("Startup failure, initiating shutdown", e);
      try {
        shutdownControllerHandle().performShutdown();
      } catch (Throwable e2) {
        Exceptions.rethrowIfFatal(e2, e);
        e.addSuppressed(e);
      }
      throw Exceptions.alwaysRethrow(e, InterruptedException.class, TimeoutException.class);
    }
    if (!errors.isEmpty()) {
      shutdownControllerHandle().performShutdown();
    }
    return errors;
  }

  @Nonnull
  private Try<Seq<Throwable>> innerStart(Executor executor, long timeout, TimeUnit timeoutUnit) {
    var errors = new AtomicReference<Seq<Throwable>>(Seq());
    var counter = new AtomicInteger(0);
    startupActions.startupActions().forEach(a -> {
      counter.incrementAndGet();
      executor.execute(() -> {
        outerStartupAction(counter, errors, a);
      });
    });
    try {
      if (timeout <= 0) {
        startupLatch.get().await();
      } else {
        startupLatch.get().await(timeout, timeoutUnit);
      }
      if (startupLatch.get().getCount() > 0) {
        return Failure(new TimeoutException("Timeout awaiting startup completion ("
            + Duration.of(timeout, timeoutUnit.toChronoUnit()) + ")"));
      }
      return Success(errors.get());
    } catch (InterruptedException e) {
      return Failure(e);
    }
  }

  private void outerStartupAction(AtomicInteger counter, AtomicReference<Seq<Throwable>> errors, CheckedRunnable a) {
    try {
      if (!errors.get().isEmpty()) {
        LOG.debug("Skipping startup action {} because startup is being aborted", a);
      }
      shutdownControllerHandle().runPreventingShutdown(() -> innerStartupAction(errors, a));
    } finally {
      if (counter.decrementAndGet() == 0) {
        startupLatch.get().countDown();
      }
    }
  }

  private void innerStartupAction(AtomicReference<Seq<Throwable>> errors, CheckedRunnable a) {
    try {
      a.run();
    } catch (Throwable e) {
      try {
        LOG.error("Error in startup action {}", a, e);
        errors.updateAndGet(errs -> errs.append(e));
        shutdownControllerHandle().announceShutdown();
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
    public ShutdownController shutdownController() {
      return new ExecutorShutdownController(this::workExecutor);
    }

    @Override
    protected Executor executor() {
      return workExecutor();
    }
  }

  @Feature
  public abstract static class SameThread extends LifecycleFeature {
    public SameThread() {
    }

    @Provision(shared = true)
    @Override
    public ShutdownController shutdownController() {
      return new ExecutorShutdownController(() -> Runnable::run);
    }

    @Override
    protected Executor executor() {
      return Runnable::run;
    }
  }
}
