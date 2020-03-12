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

public interface Scheduler {

  Handle schedule(Schedule schedule, Task task);

  default SimpleSchedule.Fluent repeatAtRate(Duration rate) {
    return new SimpleSchedule.Fluent(this).repeatAtRate(rate);
  }

  default SimpleSchedule.Fluent repeatWithDelay(Duration delay) {
    return new SimpleSchedule.Fluent(this).repeatWithDelay(delay);
  }

  default SimpleSchedule.Fluent in(Duration initial) {
    return new SimpleSchedule.Fluent(this).initial(initial);
  }

  default SimpleSchedule.Fluent at(Instant initial) {
    return new SimpleSchedule.Fluent(this).initial(initial);
  }

  @FunctionalInterface
  interface Task {
    void run();
  }

  interface Handle {
    void cancel();
    void reschedule(Schedule schedule);
  }

  interface Schedule {
    Option<Instant> findNextExecution(Clock clock, Instant nominalExecution, Option<Instant> actualExecution);
  }
}
