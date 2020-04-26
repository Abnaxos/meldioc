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

import ch.raffael.meldioc.util.Exceptions;
import ch.raffael.meldioc.util.Strings;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.vavr.control.Option.none;

public final class DefaultScheduler implements Scheduler {

  public static final Duration DEFAULT_READJUST_DURATION = Duration.ofSeconds(10);
  public static final Duration DEFAULT_LATE_RUN_TOLERANCE = Duration.ofMillis(5);
  public static final Duration DEFAULT_EARLY_RUN_TOLERANCE = Duration.ofNanos(10);
  public static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

  private static int NANOS_PER_MILLI = (int) TimeUnit.MILLISECONDS.toNanos(1);
  private final Duration FOREVER = Duration.ofMillis(Long.MAX_VALUE);

  private static final Logger LOG = LoggerFactory.getLogger(DefaultScheduler.class);

  private final Executor executor;
  private final Clock clock;
  private final Duration earlyRunTolerance;
  private final Duration lateRunTolerance;
  private final Duration readjustDuration;

  private final Object sync = new Object();
  @GuardedBy("sync")
  @Nullable
  private ScheduledTask<?> queueHead = null;
  private volatile boolean shutdownFlag = false;
  @GuardedBy("sync")
  @Nullable
  private volatile Thread thread;

  private DefaultScheduler(Executor executor, Clock clock,
                           Duration earlyRunTolerance, Duration lateRunTolerance,
                           Duration readjustDuration) {
    this.executor = executor;
    this.clock = clock;
    this.readjustDuration = readjustDuration;
    this.earlyRunTolerance = earlyRunTolerance;
    this.lateRunTolerance = lateRunTolerance;
  }

  public static Builder withExecutor(Executor executor) {
    return new Builder(executor);
  }

  @Override
  public <T> Handle schedule(Schedule<T> schedule, Task task) {
    return enqueue(new ScheduledTask<>(schedule, task));
  }

  public boolean shutdown() {
    return shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
  }

  public void realign() {
    synchronized (sync) {
      sync.notifyAll();
    }
  }

  public boolean shutdown(Duration timeout) {
    Thread t;
    synchronized (sync) {
      t = thread;
      shutdownFlag = true;
      sync.notifyAll();
    }
    if (t != null) {
      var interrupted = false;
      try {
        while (t.isAlive()) {
          try {
            t.join(timeout.toMillis(), timeout.toNanosPart() % NANOS_PER_MILLI);
          } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for scheduler thread to exit");
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return t == null || !t.isAlive();
  }

  private void start() {
    synchronized (sync) {
      if (shutdownFlag) {
        throw new IllegalStateException("Scheduler has been shut down");
      }
      var t = thread;
      if (t == null || !t.isAlive()) {
        t = new Thread(this::run, getClass().getName());
        t.start();
        thread = t;
      }
    }
  }

  private <T> ScheduledTask<T> enqueue(ScheduledTask<T> task) {
    synchronized (sync) {
      var state = task.state.getOrNull();
      if (state != null) {
        if (!shutdownFlag) {
          if (queueHead == null) {
            queueHead = task;
          } else {
            ScheduledTask<?> prev = null;
            ScheduledTask<?> current = queueHead;
            while (current != null && current.state.map(Tuple2::_1).getOrElse(Instant.MIN).compareTo(state._1()) <= 0) {
              prev = current;
              current = prev.next;
            }
            task.next = current;
            if (prev == null) {
              queueHead = task;
            } else {
              prev.next = task;
            }
          }
          sync.notifyAll();
        }
      }
    }
    return task;
  }

  private void remove(ScheduledTask<?> task) {
    synchronized (sync) {
      ScheduledTask<?> prev = null;
      ScheduledTask<?> current = queueHead;
      while (current != null) {
        if (current.equals(task)) {
          if (prev != null) {
            prev.next = current.next;
          } else {
            queueHead = current;
          }
          task.next = null;
          return;
        }
        prev = current;
        current = prev.next;
      }
    }
  }

  @SuppressWarnings("UnnecessaryLabelOnContinueStatement")
  private void run() {
    try {
      mainLoop: while (true) {
        Duration wait = FOREVER;
        ScheduledTask<?> runnable = null;
        synchronized (sync) {
          if (shutdownFlag) {
            break;
          }
          LOG.trace("Adjusting: {}", queueHead);
          if (queueHead != null) {
            var now = now();
            var state = queueHead.state.getOrNull();
            if (state == null) {
              LOG.warn("Non-runnable task found in queue: {}", queueHead.task);
              queueHead = queueHead.next;
              continue mainLoop;
            }
            wait = Duration.between(now, state._1());
            if (wait.compareTo(earlyRunTolerance) < 0) {
              runnable = queueHead;
              queueHead = queueHead.next;
            } else {
              if (readjustDuration.compareTo(wait) < 0) {
                wait = readjustDuration;
              }
            }
          }
          if (runnable == null) {
            try {
              sync.wait(wait.toMillis(), wait.toNanosPart() % NANOS_PER_MILLI);
            } catch (InterruptedException e) {
              interrupted(e);
            }
            continue mainLoop;
          }
        }
        if (Thread.interrupted()) {
          interrupted(null);
        }
        executor.execute(runnable::run);
      }
      LOG.info("Scheduler exiting normally");
    } catch (Throwable e) {
      LOG.error("Scheduler exiting with error", e);
    } finally {
      synchronized (sync) {
        queueHead = null;
        thread = null;
      }
    }
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

    @GuardedBy("sync")
    @Nullable
    private ScheduledTask<?> next = null;
    @Nullable
    private Schedule<T> schedule;
    private volatile Option<Tuple2<Instant, T>> state;

    private ScheduledTask(Schedule<T> schedule, Task task) {
      this.schedule = schedule;
      this.task = task;
      state = schedule.initialExecution(clock);
    }

    @Override
    public void cancel() {
      synchronized (sync) {
        schedule = null;
        state = none();
        remove(this);
      }
    }

    @Override
    public <U> Handle reschedule(Schedule<U> schedule) {
      synchronized (sync) {
        cancel();
        return schedule(schedule, task);
      }
    }

    private void run() {
      var nominalExecution = state.map(Tuple2::_1).getOrNull();
      if (nominalExecution != null && !shutdownFlag) {
        var actualExecution = now();
        var late = Duration.between(nominalExecution, actualExecution);
        var doRun = true;
        if (late.compareTo(lateRunTolerance) > 0) {
          doRun = onLateRun(task, nominalExecution, late);
        }
        try {
          if (doRun) {
            task.run();
          }
        } catch (Throwable e) {
          try {
            LOG.error("Task {} failed ", task, e);
          } catch (Throwable e2) {
            Exceptions.rethrowIfFatal(e2, e);
            e.addSuppressed(e2);
            LOG.error("A scheduled task failed, logging the error also failed", e);
          }
          Exceptions.rethrowIfFatal(e);
        }
        reschedule(actualExecution);
      }
    }

    protected boolean onLateRun(Task task, Instant nominalExecution, Duration difference) {
      LOG.warn("Task {} running late by {}", task, difference);
      return true;
    }

    private void reschedule(Instant actual) {
      synchronized (sync) {
        var next = state;
        if (schedule == null || next.isEmpty()) {
          state = none();
          return;
        }
        int skips = -1;
        do {
          skips++;
          next = schedule.nextExecution(clock, actual, skips, next.get()._2());
        } while (next.map(s -> s._1().isBefore(now())).getOrElse(false));
        if (skips > 0) {
          LOG.warn("Skipping {} nominal executions of task {}, next run at {}", skips, task, next);
        }
        state = next;
        enqueue(this);
      }
    }

    @Override
    public String toString() {
      return "DefaultScheduler.ScheduledTask{" +
          "task=" + task +
          ",state=" + state +
          ",schedule=" + schedule +
          '}';
    }
  }

  public static final class Builder {
    private final Executor executor;
    private Clock clock = Clock.systemDefaultZone();
    private Duration earlyRunTolerance = DEFAULT_EARLY_RUN_TOLERANCE;
    private Duration lateRunTolerance = DEFAULT_LATE_RUN_TOLERANCE;
    private Duration readjustDuration = DEFAULT_READJUST_DURATION;

    private Builder(Executor executor) {
      this.executor = executor;
    }

    public Builder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    public Builder earlyRunTolerance(Duration earlyRunTolerance) {
      checkGreaterThanZero("early run tolerance", readjustDuration);
      this.earlyRunTolerance = earlyRunTolerance;
      return this;
    }

    public Builder lateRunTolerance(Duration lateRunTolerance) {
      checkGreaterThanZero("late run tolerance", readjustDuration);
      this.lateRunTolerance = lateRunTolerance;
      return this;
    }

    public Builder readjustDuration(Duration readjustDuration) {
      checkGreaterThanZero("readjust duration", readjustDuration);
      this.readjustDuration = readjustDuration;
      return this;
    }
    
    public DefaultScheduler build() {
      var scheduler = new DefaultScheduler(executor, clock, earlyRunTolerance, lateRunTolerance, readjustDuration);
      scheduler.start();
      return scheduler;
    }

    private void checkGreaterThanZero(String description, Duration readjustDuration) {
      if (readjustDuration.isNegative() || readjustDuration.isZero()) {
        throw new IllegalArgumentException(Strings.capitalize(description) + " must be grater than zero: "
            + readjustDuration);
      }
    }
  }
}
