/*
 *  Copyright (c) 2020 Raffael Herzog
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

package ch.raffael.meldioc.library.base.lifecycle;

import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.library.base.ShutdownHooks;
import ch.raffael.meldioc.library.base.threading.ThreadingFeature;
import io.vavr.CheckedRunnable;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;


/**
 * A helper class to manage context lifecycle with features supporting
 * applications.
 */
public class Lifecycle {

  private static final ShutdownController.Actuator NULL_SHUTDOWN_ACTUATOR = new ShutdownController.Actuator() {
    @Override
    public ShutdownController controller() {
      throw new UnsupportedOperationException();
    }
    @Override
    public <T> T getPreventingShutdown(Supplier<T> supplier) {
      return supplier.get();
    }
    @Override
    public void announceShutdown() {
    }
    @Override
    public Seq<Throwable> performShutdown() {
      return List.empty();
    }
  };

  private final Supplier<? extends Traversable<? extends CheckedRunnable>> startupActions;
  private final Supplier<? extends Executor> executor;
  private final Supplier<? extends ShutdownController.Actuator> shutdownActuator;
  private final Instant createTimestamp;

  private boolean asApplication = false;
  private Consumer<StartupSuccess> onSuccess = __ -> {};
  private Consumer<StartupError> onError = __ -> {};
  private Option<Integer> exitOnError = none();

  protected Lifecycle(Supplier<? extends Traversable<? extends CheckedRunnable>> startupActions,
                      Supplier<? extends Executor> executor,
                      Supplier<? extends ShutdownController.Actuator> shutdownActuator,
                      Instant createTimestamp) {
    this.startupActions = startupActions;
    this.executor = executor;
    this.shutdownActuator = shutdownActuator;
    this.createTimestamp = createTimestamp;
  }

  public static Lifecycle of(Supplier<? extends Executor> executor,
                             Supplier<? extends Traversable<? extends CheckedRunnable>> startupActions,
                             Supplier<? extends ShutdownController.Actuator> shutdownActuator) {
    return new Lifecycle(startupActions, executor, shutdownActuator, Instant.now());
  }

  public static Lifecycle of(Supplier<? extends Executor> executor,
                             StartupActions.Feature startupActionsFeature,
                             ShutdownFeature.WithActuator shutdownFeature) {
    return of(executor, startupActionsFeature.startupActionsEP()::startupActions, shutdownFeature::shutdownActuator);
  }

  public static Lifecycle of(ThreadingFeature threadingFeature,
                             StartupActions.Feature startupActionsFeature,
                             ShutdownFeature.WithActuator shutdownFeature) {
    return of(threadingFeature::workExecutor,
        startupActionsFeature.startupActionsEP()::startupActions, shutdownFeature::shutdownActuator);
  }

  public static Lifecycle of(Feature lifecycleFeature) {
    return of(lifecycleFeature::workExecutor, lifecycleFeature.startupActionsEP()::startupActions,
        lifecycleFeature::shutdownActuator);
  }

  public static ShutdownController.Actuator nullShutdownActuator() {
    return NULL_SHUTDOWN_ACTUATOR;
  }

  public Lifecycle onSuccess(Consumer<? super StartupSuccess> onSuccess) {
    this.onSuccess = this.onSuccess.andThen(onSuccess);
    return this;
  }

  public Lifecycle onError(Consumer<? super StartupError> onError) {
    this.onError = this.onError.andThen(onError);
    return this;
  }

  public Lifecycle log(Logger log) {
    onSuccess(r -> {
      if (log.isInfoEnabled()) {
        log.info("Startup completed successfully in {}", r.timingInfoString());
      }
    });
    onError(err -> {
      if (!err.errors().isEmpty() && log.isErrorEnabled()) {
        log.error("Startup completed with errors in {}: {}", err.timingInfoString(), err.errors());
      }
      err.failure().forEach(e -> {
        if (log.isErrorEnabled()) {
          log.error("Startup failed after {}", err.timingInfoString(), e);
        }
      });
    });
    return this;
  }

  public Lifecycle asApplication(Logger log) {
    return asApplication(some(log));
  }

  public Lifecycle asApplication(Option<? extends Logger> log) {
    asApplication = true;
    log.forEach(this::log);
    shutdownHook();
    exitOnError();
    return this;
  }

  public Lifecycle shutdownHook() {
    ShutdownHooks.shutdownHooks().add(shutdownActuator);
    return this;
  }

  public Lifecycle exitOnError() {
    return exitOnError(1);
  }

  public Lifecycle exitOnError(int exitCode) {
    exitOnError = some(exitCode);
    return this;
  }

  public StartupResult start() {
    return start(0);
  }

  public StartupResult start(long timeoutSeconds) {
    return start(timeoutSeconds, TimeUnit.SECONDS);
  }

  public StartupResult start(long timeout, TimeUnit timeoutUnit) {
    Seq<Throwable> errors;
    try {
      errors = new Startup(startupActions.get(), executor.get(), shutdownActuator.get()).start(timeout, timeoutUnit);
    } catch (TimeoutException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return doOnError(new StartupError(e));
    }
    if (errors.isEmpty()) {
      var success = new StartupSuccess();
      onSuccess.accept(success);
      return success;
    } else {
      return doOnError(new StartupError(errors));
    }
  }

  public Seq<Throwable> shutdown() throws InterruptedException {
    return shutdownActuator.get().performShutdown();
  }

  @SuppressWarnings("CallToSystemExit")
  private StartupError doOnError(StartupError error) {
    try {
      onError.accept(error);
      return error;
    } finally {
      if (exitOnError.isDefined()) {
        System.exit(exitOnError.get());
      }
    }
  }

  public abstract class StartupResult {
    private final boolean success;
    private final Seq<Throwable> errors;
    private final Option<Throwable> failure;
    private final Duration duration = Duration.between(createTimestamp, Instant.now());

    private StartupResult(boolean success, Seq<Throwable> errors, Option<Throwable> failure) {
      this.success = success;
      this.errors = errors;
      this.failure = failure;
    }

    public boolean success() {
      return success;
    }

    public Seq<Throwable> errors() {
      return errors;
    }

    public Option<Throwable> failure() {
      return failure;
    }

    public Duration duration() {
      return duration;
    }

    public Duration jvmUptime() {
      return Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
    }

    public String timingInfoString() {
      return timingInfoString(asApplication);
    }

    public String timingInfoString(boolean jvmUptime) {
      return jvmUptime ? duration() + " (JVM uptime " + jvmUptime() + ")" : duration().toString();
    }
  }

  public final class StartupSuccess extends StartupResult {
    private StartupSuccess() {
      super(true, List.empty(), none());
    }
  }

  public final class StartupError extends StartupResult {
    private StartupError(Seq<Throwable> errors) {
      super(false, errors, none());
    }
    private StartupError(Throwable failure) {
      super(false, List.empty(), some(failure));
    }
  }

  @ch.raffael.meldioc.Feature
  public static abstract class Feature extends StartupActions.Feature
      implements ThreadingFeature, ShutdownFeature.WithActuator {

    @Provision(singleton = true)
    @Override
    public ShutdownController.Actuator shutdownActuator() {
      return new ExecutorShutdownController(this::workExecutor).actuator();
    }
  }

  @Deprecated(forRemoval = true)
  public static class LegacyLifecycle extends Lifecycle {

    protected LegacyLifecycle(Supplier<? extends Traversable<? extends CheckedRunnable>> startupActions,
                              Supplier<? extends Executor> executor,
                              Supplier<? extends ShutdownController.Actuator> shutdownActuator,
                              Instant createTimestamp) {
      super(startupActions, executor, shutdownActuator, createTimestamp);
    }
  }
}
