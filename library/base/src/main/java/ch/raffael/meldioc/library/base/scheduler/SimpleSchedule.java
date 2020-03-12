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

import io.vavr.control.Option;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public class SimpleSchedule implements Scheduler.Schedule {

  private final Option<Function<? super Clock, ? extends Instant>> initial;
  private final Option<Duration> every;
  private final RepeatMode repeatMode;

  private SimpleSchedule(Option<Function<? super Clock, ? extends Instant>> initial, Option<Duration> every, RepeatMode repeatMode) {
    this.repeatMode = repeatMode;
    every.forEach(SimpleSchedule::requirePositive);
    this.initial = initial;
    this.every = every;
  }

  public static void requirePositive(Duration delay) {
    if (delay.compareTo(Duration.ZERO) < 0) {
      throw new IllegalArgumentException("Not a positive duration: " + delay);
    }
  }

  @Override
  public Option<Instant> findNextExecution(Clock clock, Instant nominalExecution, Option<Instant> actualExecution) {
    if (actualExecution.isEmpty()) {
      return initial.<Instant>map(f -> f.apply(clock)).orElse(() -> some(clock.instant()));
    } else if (every.isEmpty()) {
      return none();
    }
    return some(repeatMode.nextExecutionBase(nominalExecution).plus(every.get()));
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

  public static class Fluent {
    private final Scheduler scheduler;
    private Option<Function<? super Clock, ? extends Instant>> initial = none();
    private Option<Duration> delay = none();
    private RepeatMode repeatMode = RepeatMode.RATE;

    public Fluent(Scheduler scheduler) {
      this.scheduler = scheduler;
    }

    public Fluent initial(Instant initial) {
      this.initial = some(__ -> initial);
      return this;
    }

    public Fluent initial(Duration initialDelay) {
      this.initial = some(c -> c.instant().plus(initialDelay));
      return this;
    }

    public Fluent repeatAtRate(Duration rate) {
      return repeat(rate, RepeatMode.RATE);
    }

    public Fluent repeatWithDelay(Duration delay) {
      return repeat(delay, RepeatMode.DELAY);
    }

    private Fluent repeat(Duration delay, RepeatMode mode) {
      requirePositive(delay);
      this.delay = some(delay);
      repeatMode = mode;
      return this;
    }

    public void run(Scheduler.Task task) {
      scheduler.schedule(new SimpleSchedule(initial, delay, repeatMode), task);
    }
  }
}
