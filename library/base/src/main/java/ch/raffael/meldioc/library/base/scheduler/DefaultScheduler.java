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
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

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

  private final String name;
  // WARNING: shutdownLock is sometimes acquired within adminLock of ScheduledTask
  // NEVER acquire first shutdownLock, then adminLock, as this might cause dead-locks
  private final Object shutdownLock = new Object();
  private final ScheduledThreadPoolExecutor scheduler;
  private final Executor executor;
  private final Clock clock;
  private final long earlyRunTolerance;
  private final long lateRunTolerance;
  private final long driftCompensationRate;

  private DefaultScheduler(Executor executor, Builder builder) {
    this.name = builder.name.getOrElse(() -> getClass().getSimpleName());
    this.scheduler = new ScheduledThreadPoolExecutor(1,
        new CountingThreadFactory(CountingThreadFactory.dashedNameBuilder(name)));
    try {
      this.executor = executor;
      this.clock = builder.clock.map(c -> c.withZone(ZoneOffset.UTC)).getOrElse(Clock.systemUTC());
      this.earlyRunTolerance = max(builder.earlyRunTolerance.toNanos(), 0);
      this.lateRunTolerance = max(builder.lateRunTolerance.toNanos(), 0);
      this.driftCompensationRate = max(builder.driftCompensationRate.toNanos(), 0);
      scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
      scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      scheduler.setMaximumPoolSize(1);
      scheduler.setRemoveOnCancelPolicy(true);
    } catch (Throwable e) {
      try {
        scheduler.shutdown();
      } catch (Throwable e2) {
        e.addSuppressed(e2);
      }
      throw Exceptions.alwaysRethrow(e);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> Handle schedule(Schedule<T> schedule, Task task) {
    return new ScheduledTask<>(task, schedule);
  }

  public boolean shutdown() {
    return shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
  }

  public boolean shutdown(Duration timeout) {
    long timeoutNanos = timeout.toNanos();
    synchronized (shutdownLock) {
      if (!scheduler.isShutdown()) {
        LOG.info("Scheduler {} shutting down", name);
      }
      scheduler.shutdown();
    }
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

    private final Runnable adminRunnable = this::admin;
    private final Runnable taskRunnable = this::runTask;

    private final Object adminLock = new Object();
    @Nullable
    private Tuple3<Instant, T, ScheduledFuture<?>> state;
    private int totalSkips = 0;
    private int totalRuns = 0;
    private int lateRuns = 0;

    private final Object threadLock = new Object();
    @Nullable
    private Thread thread = null;

    private volatile boolean cancelled = false;

    private ScheduledTask(Task task, Schedule<T> schedule) {
      this.task = task;
      this.schedule = schedule;
      scheduleNext();
    }

    @Override
    public void cancel() {
      cancelled = true;
      synchronized (adminLock) {
        if (state != null) {
          state._3.cancel(false);
        }
      }
    }

    @Override
    public void forceCancel() {
      cancel();
      synchronized (threadLock) {
        if (thread != null) {
          thread.interrupt();
        }
      }
    }

    public boolean active() {
      synchronized (adminLock) {
        return state != null && !cancelled();
      }
    }

    public boolean running() {
      synchronized (threadLock) {
        return thread != null;
      }
    }

    public boolean cancelled() {
      return cancelled || scheduler.isShutdown();
    }

    public int runCount() {
      synchronized (adminLock) {
        return totalRuns;
      }
    }

    private void admin() {
      try {
        Runnable runnable = null;
        synchronized (adminLock) {
          if (state == null || state._3.isCancelled()) {
            return;
          }
          var wait = Duration.between(now(), state._1).toNanos();
          if (cancelled()) {
            return;
          }
          if (wait <= earlyRunTolerance) {
            runnable = taskRunnable;
          } else {
            doSchedule(state::update3, wait);
          }
        }
        if (runnable != null) {
          executor.execute(runnable);
        }
      } catch (Throwable e) {
        Exceptions.rethrowIfFatal(e);
        LOG.error("Internal error in scheduler", e);
      }
    }

    private void runTask() {
      try {
        var actualTime = now();
        Instant scheduledStart;
        synchronized (adminLock) {
          if (state == null || state._3.isCancelled()) { // TODO FIXME (2020-07-21)
            return;
          }
          scheduledStart = state._1;
          totalRuns++;
          var late = Duration.between(scheduledStart, actualTime);
          if (late.toNanos() >= lateRunTolerance) {
            lateRuns++;
            onLateRun(task, scheduledStart, late);
          }
        }
        try {
          synchronized (threadLock) {
            thread = Thread.currentThread();
          }
          if (cancelled()) {
            return;
          }
          task.run();
        } finally {
          synchronized (threadLock) {
            thread = null;
          }
        }
      } finally {
        try {
          scheduleNext();
        } catch (Throwable e) {
          LOG.error("Error scheduling next execution of task {}", task, e);
        }
      }
    }

    private void scheduleNext() {
      int skips = 0;
      Tuple2<Instant, T> next = null;
      synchronized (adminLock) {
        if (state != null) {
          next = Tuple.of(state._1, state._2);
        }
      }
      while (true) {
        next = (next == null
                ? schedule.initialExecution(clock)
                : schedule.nextExecution(clock, next._1, skips, next._2))
            .getOrNull();
        if (next != null && next._1.isBefore(now().minus(lateRunTolerance, ChronoUnit.NANOS)) && !cancelled()) {
          skips++;
        } else {
          break;
        }
      }
      if (skips > 0) {
        LOG.warn("Skipping {} nominal executions of task {}, next run at {} (total skips/runs {}/{})",
            skips, task, next == null ? "<never>" : next._1, totalSkips, totalRuns);
      }
      synchronized (adminLock) {
        totalSkips += skips;
        if (next != null) {
          doSchedule(next::append, Duration.between(now(), next._1).toNanos());
        } else {
          state = null;
        }
      }
    }

    private void doSchedule(Function<ScheduledFuture<?>, Tuple3<Instant, T, ScheduledFuture<?>>> stateUpdater, long nanos) {
      assert Thread.holdsLock(adminLock);
      synchronized (shutdownLock) {
        if (state != null) {
          // just to be sure
          state._3.cancel(false);
        }
        if (cancelled()) {
          return;
        }
        state = stateUpdater.apply(scheduler.schedule(adminRunnable,
            driftCompensationRate <= 0 ? nanos : min(nanos, driftCompensationRate), NANOSECONDS));
      }
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
