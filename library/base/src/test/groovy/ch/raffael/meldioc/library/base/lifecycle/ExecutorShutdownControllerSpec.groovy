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

package ch.raffael.meldioc.library.base.lifecycle

import ch.raffael.meldioc.library.base.lifecycle.ShutdownController.IllegalShutdownStateException
import io.vavr.CheckedRunnable
import io.vavr.collection.Seq
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

class ExecutorShutdownControllerSpec extends Specification {

  def "Shutdown handlers of all phases are called in the reverse order they were added"() {
    given: "An ExecutorShutdownController, a mock Executor, and two handlers for each shutdown phase"
    Executor executor = {r -> r.run()}
    def calls = []
    def sctl = new ExecutorShutdownController({executor})
    def prepare1 = newRunnable(calls, 'prepare 1')
    def perform1 = newRunnable(calls, 'perform 1')
    def final1 = newRunnable(calls, 'final 1')
    def prepare2 = newRunnable(calls, 'prepare 2')
    def perform2 = newRunnable(calls, 'perform 2')
    def final2 = newRunnable(calls, 'final 2')

    when: "Add a handler to each phase"
    sctl.onPrepare(prepare1)
    sctl.onPerform(perform1)
    sctl.onFinalize(final1)
    and: "Add another handler to each phase"
    sctl.onPrepare(prepare2)
    sctl.onPerform(perform2)
    sctl.onFinalize(final2)
    and: "Perform the shutdown"
    def errors = sctl.actuator().performShutdown()

    then: "The error list is empty"
    errors.empty
    and: "The handlers were called in the correct order, i.e. prepare -> perform -> finalize, each in reverse order"
    calls == [prepare2, prepare1, perform2, perform1, final2, final1]
  }

  def "All types of shutdown hooks may throw exceptions; these will be logged and shutdown continues"() {
    given: "An ExecutorShutdownController, a mock Executor, and a throwing handler shutdown phase"
    Executor executor = {r -> r.run()}
    def calls = []
    def sctl = new ExecutorShutdownController({executor})
    def prepare1 = newRunnable(calls, 'prepare 1', {throw new RuntimeException()})
    def perform1 = newRunnable(calls, 'perform 1', {throw new RuntimeException()})
    def final1 = newRunnable(calls, 'final 1', {throw new RuntimeException()})
    def prepare2 = newRunnable(calls, 'prepare 2', {throw new RuntimeException()})
    def perform2 = newRunnable(calls, 'perform 2', {throw new RuntimeException()})
    def final2 = newRunnable(calls, 'final 2', {throw new RuntimeException()})

    when: "Add a throwing handler to each phase"
    sctl.onPrepare(prepare1)
    sctl.onPerform(perform1)
    sctl.onFinalize(final1)
    and: "Add another two handlers to each phase"
    sctl.onPrepare(prepare2)
    sctl.onPerform(perform2)
    sctl.onFinalize(final2)
    and: "Perform the shutdown"
    def errors = sctl.actuator().performShutdown()

    then: "The exceptions have been added to the list"
    errors.size() == 6
    and: "All handlers were called in the correct order"
    calls == [prepare2, prepare1, perform2, perform1, final2, final1]
  }



  def "Concurrent scenario with actions preventing shutdown, multiple shutdown requests, late hook additions"() {
    given: "A shutdown controller and some latches"
    def executor = {r -> r.run()} as Executor
    def sctl = new ExecutorShutdownController({executor})
    def latch1 = new CountDownLatch(1)
    def initiateLatch = new CountDownLatch(2)
    def finishLatch = new CountDownLatch(1)
    List<Seq<Throwable>> futures = new CopyOnWriteArrayList<>()
    def polling = new PollingConditions(timeout: 0.1, delay: 0.01)

    when: "Start a worker preventing shutdown and two shutdown requests"
    def preventThread = Thread.start {
      sctl.actuator().runPreventingShutdown({
        latch1.countDown()
        finishLatch.await()
      })
    }
    latch1.await()
    def shutdownThread1 = Thread.start {
      initiateLatch.countDown()
      futures.add sctl.actuator().performShutdown()
    }
    def shutdownThread2 = Thread.start {
      initiateLatch.countDown()
      futures.add sctl.actuator().performShutdown()
    }
    initiateLatch.await()

    then: "The shutdown is initiated"
    polling.eventually {
      sctl.state() == ShutdownController.State.INITIATED
    }
    and: "The preventing thread is still alive"
    preventThread.isAlive()

    when: "Attempt to run another shutdown preventing action"
    sctl.actuator().runPreventingShutdown({})

    then: "An ShutdownStateException is thrown"
    thrown IllegalShutdownStateException

    when: "Add some PREPARE hook which still works, fails to add another PREPARE hook, but successfully adds a PERFORM hook"
    def hookCounter = new AtomicInteger(0)
    def secondHookCounter = new AtomicInteger(0)
    sctl.onPrepare({
      hookCounter.incrementAndGet()
      try {
        sctl.onPrepare({secondHookCounter.incrementAndGet()})
        assert false: 'ShutdownStateException expected'
      } catch (IllegalShutdownStateException e) {
        e.state() == ShutdownController.State.PERFORMING
      }
      sctl.onPerform({secondHookCounter.incrementAndGet()})
    })
    and: "Finish the task preventing shutdown"
    finishLatch.countDown()
    and: "Wait for all the threads to finish"
    preventThread.join()
    shutdownThread1.join()
    shutdownThread2.join()

    then: "We've now got 2 shutdown error lists, the same instance"
    futures.size() == 2
    futures[0].is futures[1]
    then: "Shutdown state is COMPLETE"
    sctl.state() == ShutdownController.State.COMPLETE
    and: "There were no errors"
    futures[0].empty
    and: "The shutdown hook was run once"
    hookCounter.get() == 1
    and: "Our shutdown hook added from within the shutdown hook was run once"
    secondHookCounter.get() == 1
  }

  private CheckedRunnable newRunnable(List calls, String name, Closure closure = {}) {
    return new CheckedRunnable() {
      @Override
      void run() {
        calls.add this
        println "Shutdown callback: $name"
        closure.call()
      }
      @Override
      String toString() {
        "Callback[$name]"
      }
    }
  }
}
