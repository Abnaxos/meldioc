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

import ch.raffael.compose.library.base.ShutdownHooks;
import ch.raffael.compose.logging.Logging;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.vavr.API.*;

/**
 * A helper class to manage context lifecycle with features supporting
 * applications.
 */
public class Lifecycle<T extends ShutdownFeature> {

  static {
    Logging.init();
  }

  private final T context;
  private final Function<? super T, ? extends LifecycleFeature> lifecycle;
  private final Instant createTimestamp;

  private boolean asApplication = false;
  private Consumer<StartupSuccess> onSuccess = __ -> {};
  private Consumer<StartupError> onError = __ -> {};
  private Option<Integer> exitOnError = None();

  protected Lifecycle(T context, Function<? super T, ? extends LifecycleFeature> lifecycle, Instant createTimestamp) {
    this.context = context;
    this.lifecycle = lifecycle;
    this.createTimestamp = createTimestamp;
  }

  public static <T extends ShutdownFeature> LifecycleRequired<T> of(T context) {
    return new LifecycleRequired<>(context, Instant.now());
  }

  public Lifecycle<T> onSuccess(Consumer<? super StartupSuccess> onSuccess) {
    this.onSuccess = this.onSuccess.andThen(onSuccess);
    return this;
  }

  public Lifecycle<T> onError(Consumer<? super StartupError> onError) {
    this.onError = this.onError.andThen(onError);
    return this;
  }

  public Lifecycle<T> log(Logger log) {
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

  public Lifecycle<T> asApplication(Logger log) {
    return asApplication(Some(log));
  }

  public Lifecycle<T> asApplication(Option<? extends Logger> log) {
    asApplication = true;
    log.forEach(this::log);
    shutdownHook();
    exitOnError();
    return this;
  }

  public Lifecycle<T> shutdownHook() {
    ShutdownHooks.shutdownHooks().add(lifecycle.apply(context)::shutdownControllerHandle);
    return this;
  }

  public Lifecycle<T> exitOnError() {
    return exitOnError(1);
  }

  public Lifecycle<T> exitOnError(int exitCode) {
    exitOnError = Some(exitCode);
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
      errors = lifecycle.apply(context).start(timeout, timeoutUnit);
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

  public final static class LifecycleRequired<T extends ShutdownFeature> {
    private final T context;
    private final Instant createTimestamp;

    private LifecycleRequired(T context, Instant createTimestamp) {
      this.context = context;
      this.createTimestamp = createTimestamp;
    }

    public Lifecycle<T> lifecycle(Function<? super T, ? extends LifecycleFeature> lifecycle) {
      return new Lifecycle<>(context, lifecycle, createTimestamp);
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
      super(true, Seq(), None());
    }
  }

  public final class StartupError extends StartupResult {
    private StartupError(Seq<Throwable> errors) {
      super(false, errors, None());
    }
    private StartupError(Throwable failure) {
      super(false, Seq(), Some(failure));
    }
  }
}
