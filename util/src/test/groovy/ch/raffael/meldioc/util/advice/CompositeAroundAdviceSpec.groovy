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

package ch.raffael.meldioc.util.advice

import spock.lang.Specification

class CompositeAroundAdviceSpec extends Specification {

  def "Before advices are called in order they were added, after advices in reverse order"() {
    given: "A list of calls"
    def calls = []
    and: "A composite advice with two before/after components"
    def advice = CompositeAroundAdvice.builder()
        .before({return {calls.add '+a'}}).after({return {calls.add '-a'}})
        .before({return {calls.add '+b'}}).after({return {calls.add '-b'}})
        .advice()

    when: "before advice"
    def closeable = advice.before()

    then: "The two before advices have been called in forward order"
    calls == ['+a', '+b']

    when: "after advice"
    calls = []
    closeable.close()

    then: "The two after advices have been called in reverse order"
    calls ==  ['-b', '-a']
  }

  def "When a before advice throws, an AdviceException is thrown immediately but all after advices are called"() {
    given: "A list of calls"
    def calls = []
    and: "A composite advice with two before/after components"
    def advice = CompositeAroundAdvice.builder()
        .before({return {throw new RuntimeException('+a')}}).after({return {calls.add '-a'}})
        .before({return {calls.add '+b'}}).after({return {calls.add '-b'}})
        .advice()

    when:
    advice.before().withCloseable {}

    then: "An AdviceException is thrown"
    def e = thrown AdviceException
    e.cause.class == RuntimeException
    e.cause.message == '+a'
    and: "The second before advice was skipped, but both after advices were called"
    calls == ['-b', '-a']
  }

  def "When multiple after advices throw, later exceptions are added as suppressed to the first one"() {
    given: "A composite advice with two before/after components"
    def advice = CompositeAroundAdvice.builder()
        .after({return {throw new RuntimeException('-a')}})
        .after({return {throw new RuntimeException('-b')}})
        .after({return {throw new RuntimeException('-c')}})
        .advice()

    when:
    advice.before().withCloseable {}

    then: "The first after exception is wrapped into an advice exception"
    def e = thrown AdviceException
    e.cause.message == '-c'
    and: "More after exceptions are attached as suppressed"
    e.suppressed.collect {it.message} as List == ['-b', '-a']
  }

  def "When both the before advice and multiple after advices throw, the after AdviceException is added as suppressed to the before AdviceException"() {
    given: "A composite advice with two before/after components"
    def advice = CompositeAroundAdvice.builder()
        .before({return {throw new RuntimeException('+a')}})
        .after({return {throw new RuntimeException('-a')}})
        .after({return {throw new RuntimeException('-b')}})
        .after({return {throw new RuntimeException('-c')}})
        .advice()

    when:
    advice.before().withCloseable {}

    then: "The before exception is wrapped into an advice exception"
    def before = thrown AdviceException
    before.cause.message == '+a'
    and: "The first after exception is wrapped into an advice exception and added as suppressed to the before exception"
    before.suppressed.size() == 1
    def after = before.suppressed[0]
    after.class == AdviceException
    after.cause.message == '-c'
    and: "More after exceptions are attached as suppressed to the first after exception"
    after.suppressed.collect {it.message} as List == ['-b', '-a']
  }
}
