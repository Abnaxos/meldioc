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

package ch.raffael.compose.core.shutdown

import io.vavr.CheckedRunnable
import spock.lang.Specification

import java.util.concurrent.Executor

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
    sctl.performShutdown()

    then: "The handlers were called in the correct order, i.e. prepare -> perform -> finalize, each in reverse order"
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
    sctl.performShutdown()

    then: "All handlers were called in the correct order"
    calls == [prepare2, prepare1, perform2, perform1, final2, final1]
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
