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

import ch.raffael.meldioc.library.base.scheduler.Scheduler.Schedule;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public class SimpleSchedule implements Schedule<Instant> {

  private final Option<Function<? super Clock, ? extends Instant>> initial;
  private final Option<Duration> every;
  private final RepeatMode repeatMode;

  private SimpleSchedule(Option<Function<? super Clock, ? extends Instant>> initial, Option<Duration> every, RepeatMode repeatMode) {
    this.repeatMode = repeatMode;
    every.forEach(SimpleSchedule::requirePositive);
    this.initial = initial;
    this.every = every;
  }

  public static Builder.Standalone builder() {
    return new Builder.Standalone();
  }

  public static Builder.Bound with(Scheduler scheduler) {
    return new Builder.Bound(scheduler);
  }

  public static void requirePositive(Duration delay) {
    if (delay.compareTo(Duration.ZERO) < 0) {
      throw new IllegalArgumentException("Not a positive duration: " + delay);
    }
  }

  @Override
  public Option<Tuple2<Instant, Instant>> initialExecution(Clock clock) {
    return initial.<Instant>map(f -> f.apply(clock))
        .orElse(() -> some(clock.instant()))
        .map(t -> Tuple.of(t, t));
  }

  @Override
  public Option<Tuple2<Instant, Instant>> nextExecution(Clock clock, Instant actualExecution, int skips, Instant state) {
    return every.map(e -> repeatMode.nextExecutionBase(state).plus(e)).map(t -> Tuple.of(t, t));
  }

  private enum RepeatMode {
    RATE(UnaryOperator.identity()), DELAY(__ -> Instant.now());

    private final UnaryOperator<Instant> nextExecutionOp;

    RepeatMode(UnaryOperator<Instant> nextExecutionOp) {
      this.nextExecutionOp = nextExecutionOp;
    }

    Instant nextExecutionBase(Instant nominalExecution) {
      return nextExecutionOp.apply(nominalExecution);
    }
  }

  public static class Builder<SELF> {
    private Option<Function<? super Clock, ? extends Instant>> initial = none();
    private Option<Duration> delay = none();
    private RepeatMode repeatMode = RepeatMode.RATE;

    public Builder() {
    }

    public SELF initial(Instant initial) {
      this.initial = some(__ -> initial);
      return self();
    }

    public SELF initial(Duration initialDelay) {
      this.initial = some(c -> c.instant().plus(initialDelay));
      return self();
    }

    public SELF repeatAtRate(Duration rate) {
      return repeat(rate, RepeatMode.RATE);
    }

    public SELF repeatWithDelay(Duration delay) {
      return repeat(delay, RepeatMode.DELAY);
    }

    private SELF repeat(Duration delay, RepeatMode mode) {
      requirePositive(delay);
      this.delay = some(delay);
      repeatMode = mode;
      return self();
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
      return (SELF) this;
    }

    protected SimpleSchedule build() {
      return new SimpleSchedule(initial, delay, repeatMode);
    }

    public static class Bound extends Builder<Bound> {
      private final Scheduler scheduler;
      public Bound(Scheduler scheduler) {
        this.scheduler = scheduler;
      }
      public void schedule(Scheduler.Task task) {
        scheduler.schedule(build(), task);
      }
    }

    public static class Standalone extends Builder<Standalone> {
      public Standalone() {
      }
      @Override
      public SimpleSchedule build() {
        return super.build();
      }
    }
  }
}
