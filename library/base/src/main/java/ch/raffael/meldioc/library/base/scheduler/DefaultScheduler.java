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

package ch.raffael.meldioc.library.base.scheduler;

import ch.raffael.meldioc.library.base.threading.CountingThreadFactory;
import ch.raffael.meldioc.util.Exceptions;
import ch.raffael.meldioc.util.Strings;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Option;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static ch.raffael.meldioc.logging.Logging.logger;
import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public final class DefaultScheduler implements Scheduler {

  private static final Logger LOG = logger();

  public static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

  private final LinkedList<ScheduledTask<?>> tasks = new LinkedList<>();

  private final ScheduledExecutorService scheduler;
  private final Executor executor;
  private final Clock clock;
  private final long earlyRunTolerance;
  private final long lateRunTolerance;
  private final long driftCompensationRate;

  private DefaultScheduler(Executor executor, Builder builder) {
    this.scheduler = createExecutorService(builder);
    this.executor = executor;
    this.clock = builder.clock.map(c -> c.withZone(ZoneOffset.UTC)).getOrElse(Clock.systemUTC());
    this.earlyRunTolerance = max(builder.earlyRunTolerance.toNanos(), 0);
    this.lateRunTolerance = max(builder.lateRunTolerance.toNanos(), 0);
    this.driftCompensationRate = max(builder.driftCompensationRate.toNanos(), 0);
  }

  public static Builder builder() {
    return new Builder();
  }

  protected ScheduledExecutorService createExecutorService(Builder builder) {
    var executorService = new ScheduledThreadPoolExecutor(1,
        new CountingThreadFactory(builder.name.orElse(() -> some(getClass().getSimpleName()))
            .map(n -> n.replace("%", "%%") + "-%d").get()));
    executorService.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    executorService.setMaximumPoolSize(1);
    executorService.setRemoveOnCancelPolicy(true);
    var latch = new CountDownLatch(1);
    executorService.schedule(latch::countDown, 0, NANOSECONDS);
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for scheduler warm up");
      Thread.currentThread().interrupt();
    }
    return executorService;
  }

  @Override
  public <T> Handle schedule(Schedule<T> schedule, Task task) {
    var st = new ScheduledTask<>(task, schedule);
    st.scheduleNext();
    return st;
  }

  public boolean shutdown() {
    return shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
  }

  public boolean shutdown(Duration timeout) {
    long timeoutNanos = timeout.toNanos();
    scheduler.shutdownNow();
    try {
      scheduler.awaitTermination(timeoutNanos, NANOSECONDS);
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for scheduler thread to exit");
      Thread.currentThread().interrupt();
    }
    return scheduler.isTerminated();
  }

  protected boolean onLateRun(Task task, Instant nominalExecution, Duration difference) {
    LOG.warn("Task {} running late by {}", task, difference);
    return true;
  }

  private Instant now() {
    return clock.instant();
  }

  @SuppressWarnings("unused")
  private void interrupted(@Nullable InterruptedException e) {
    LOG.warn("Ignoring interrupt");
  }

  private final class ScheduledTask<T> implements Handle {

    private final Task task;
    private final Schedule<T> schedule;

    private final Object lock = new Object();
    private final Runnable adminRunnable = this::admin;
    private final Runnable taskRunnable = this::runTask;

    @Nullable
    private Tuple3<Instant, T, ScheduledFuture<?>> state;

    private int totalSkips = 0;
    private int totalRuns = 0;

    private ScheduledTask(Task task, Schedule<T> schedule) {
      this.task = task;
      this.schedule = schedule;
    }

    @Override
    public void cancel() {
      synchronized (lock) {
        if (state != null) {
          state._3.cancel(false);
        }
      }
    }

    @Override
    public <U> Handle reschedule(Schedule<U> schedule) {
      synchronized (lock) {
        cancel();
        return schedule(schedule, task);
      }
    }

    private void admin() {
      try {
        synchronized (lock) {
          if (state == null) {
            return;
          }
          var wait = Duration.between(now(), state._1).toNanos();
          if (wait <= earlyRunTolerance) {
            executor.execute(taskRunnable);
          } else {
            state = state.update3(scheduler.schedule(adminRunnable, calculateWaitNanos(wait), NANOSECONDS));
          }
        }
      } catch (Throwable e) {
        Exceptions.rethrowIfFatal(e);
        LOG.error("Internal error in scheduler", e);
      }
    }

    private void runTask() {
      var actualTime = now();
      Throwable thrown = null;
      try {
        synchronized (lock) {
          if (state == null || state._3.isCancelled()) {
            return;
          }
          totalRuns++;
          var late = Duration.between(state._1, actualTime);
          if (late.toNanos() >= lateRunTolerance) {
            onLateRun(task, state._1, late);
          }
        }
        task.run();
      } catch (Throwable e) {
        if (Exceptions.isFatal(e)) {
          synchronized (lock) {
            // always throws:
            Exceptions.rethrowIfFatal(e);
          }
        }
        thrown = e;
      }
      postRunTask(thrown);
    }

    private void postRunTask(@Nullable Throwable thrown) {
      synchronized (lock) {
        scheduleNext();
      }
    }

    private void scheduleNext() {
      synchronized (lock) {
        int skips = 0;
        var next = state == null ? null : Tuple.of(state._1, state._2);
        while(true) {
          next = (next == null
                   ? schedule.initialExecution(clock)
                   : schedule.nextExecution(clock, next._1, skips, next._2))
              .getOrElse((Tuple2<Instant, T>) null);
          if (next != null && next._1.isBefore(now().minus(lateRunTolerance, ChronoUnit.NANOS))) {
            skips++;
          } else {
            totalSkips += skips;
            break;
          }
        }
        if (skips > 0) {
          LOG.warn("Skipping {} nominal executions of task {}, next run at {}",
              skips, task, next == null ? "<never>" : next._1);
        }
        if (next != null) {
          state = next.append(scheduler.schedule(
              adminRunnable, calculateWaitNanos(Duration.between(now(), next._1).toNanos()), NANOSECONDS));
        } else {
          state = null;
        }
      }
    }

    private long calculateWaitNanos(long nanos) {
      return driftCompensationRate <= 0 ? nanos : min(nanos, driftCompensationRate);
    }

    @Override
    public String toString() {
      return "ScheduledTask{"
          + "state=" + state
          + ",schedule=" + schedule
          + ",task=" + task
          + '}';
    }
  }

  public static final class Builder {
    public static final Duration DEFAULT_LATE_RUN_TOLERANCE = Duration.ofMillis(10);
    public static final Duration DEFAULT_EARLY_RUN_TOLERANCE = Duration.ofNanos(10);
    public static final Duration DEFAULT_DRIFT_COMPENSATION_RATE = Duration.ofHours(1);

    private Option<Clock> clock = none();
    private Duration earlyRunTolerance = DEFAULT_EARLY_RUN_TOLERANCE;
    private Duration lateRunTolerance = DEFAULT_LATE_RUN_TOLERANCE;
    private Duration driftCompensationRate = DEFAULT_DRIFT_COMPENSATION_RATE;
    private Option<String> name = none();

    protected Builder() {
    }

    public Builder clock(Clock clock) {
      this.clock = some(requireNonNull(clock, "clock"));
      return this;
    }

    public Builder earlyRunTolerance(Duration earlyRunTolerance) {
      checkGreaterThanZero("early run tolerance", earlyRunTolerance);
      this.earlyRunTolerance = requireNonNull(earlyRunTolerance, "earlyRunTolerance");
      return this;
    }

    public Builder lateRunTolerance(Duration lateRunTolerance) {
      checkGreaterThanZero("late run tolerance", lateRunTolerance);
      this.lateRunTolerance = requireNonNull(lateRunTolerance, "lateRunTolerance");
      return this;
    }

    public Builder driftCompensationRate(Duration driftCompensationRate) {
      this.driftCompensationRate = requireNonNull(driftCompensationRate, "driftCompensationRate");
      return this;
    }

    public Builder name(String name) {
      this.name = some(name);
      return this;
    }

    public DefaultScheduler build(Executor executor) {
      return new DefaultScheduler(executor, this);
    }

    private void checkGreaterThanZero(String description, Duration readjustDuration) {
      if (readjustDuration.isNegative() || readjustDuration.isZero()) {
        throw new IllegalArgumentException(Strings.capitalize(description) + " must be grater than zero: "
            + readjustDuration);
      }
    }
  }
}
