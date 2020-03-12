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

package ch.raffael.meldioc.library.base.scheduler

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Clock
import java.time.Duration
import java.time.Instant

class DefaultSchedulerSpec extends Specification {

  def "Upcoming tasks are ordered correctly in the queue"() {
    given: "A dummy scheduler"
    def conditions = new PollingConditions(timeout: 1, initialDelay: 0.001, factor: 1)
    def nanoOffset = 0
    def millis = 0
    def scheduler = DefaultScheduler.withExecutor({r -> r.run()})
        .clock(Mock(Clock) {
          instant() >> { Instant.ofEpochMilli(millis).plusNanos(nanoOffset) }
        }).build()
    and: "A bunch of tasks"
    List tasks = [].asSynchronized()
    def a = new TestTask(1, tasks)
    def b = new TestTask(2, tasks)
    def c = new TestTask(3, tasks)
    def d = new TestTask(4, tasks)

    when: "Enqueue the tasks"
    scheduler.at(Instant.ofEpochMilli(5)).run(b)
    scheduler.at(Instant.ofEpochMilli(3)).run(a)
    scheduler.at(Instant.ofEpochMilli(10)).run(d)
    scheduler.at(Instant.ofEpochMilli(5)).repeatAtRate(Duration.ofMillis(50)).run(c)

    and: "Realign scheduler"
    scheduler.realign()
    then: "At first, nothing is run"
    conditions.eventually {
      tasks == []
    }

    when: "Time at 3 ms -9ns & realign scheduler"
    nanoOffset = -9
    millis = 3
    scheduler.realign()
    sleep(100)
    then: "The first task has been run"
    tasks == [1]

    when: "Time at 5 ms -11 ns & realign scheduler"
    nanoOffset = -11
    millis = 5
    scheduler.realign()
    then: "No other tasks have been run (early run tolerance)"
    tasks == [1]

    when: "Set nanoOffset to 0"
    nanoOffset = 0
    and: "Time at 5 ms & realign scheduler"
    millis = 5
    scheduler.realign()
    sleep(100)
    then: "The next two tasks have also been run"
    tasks == [1, 2, 3]

    when: "Forward to 11 ms & realign scheduler"
    millis = 11
    scheduler.realign()
    sleep(100)
    then: "All tasks have been run"
    tasks == [1, 2, 3, 4]

    when: "Forward to 55 (initial 5 + rate 50) ms & realign scheduler"
    millis = 55
    scheduler.realign()
    sleep(100)
    then: "The repeating task (3) has been run again (early run by 1 ms)"
    tasks == [1, 2, 3, 4, 3]

    cleanup:
    scheduler?.shutdown()
  }

  static class TestTask implements Scheduler.Task {
    final Object id
    final Collection recorder
    TestTask(Object id, Collection recorder) {
      this.id = id
      this.recorder = recorder
    }
    @Override
    void run() {
      println "Running task: $id"
      recorder?.add(id)
    }

    @Override
    String toString() {
      return "${super.toString()}[$id]"
    }
  }
}
