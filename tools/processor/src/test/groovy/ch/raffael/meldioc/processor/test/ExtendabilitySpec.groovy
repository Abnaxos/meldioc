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

package ch.raffael.meldioc.processor.test

import ch.raffael.meldioc.processor.test.meta.Issue
import org.spockframework.runtime.SpockAssertionError
import spock.lang.FailsWith
import spock.lang.Specification

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile

class ExtendabilitySpec extends Specification {

  @FailsWith(SpockAssertionError)
  @Issue([3, 8])
  def "For mounts, classes must not be inner (non-static) and constructor must be accessible"() {
    when:
    def c = compile('c/extendability')

    then:
    with(c.message()) {
      // id == TODO (2020-04-11) ID
      pos == c.marker('nested-constructor-not-accessible')
    }
    with(c.message()) {
      // id == TODO (2020-04-11) ID
      pos == c.marker('inner')
    }
    with(c.message()) {
      // id == TODO (2020-04-11) ID
      pos == c.marker('subpackage-constructor-not-accessible')
    }
    with(c.message()) {
      // id == TODO (2020-04-11) ID
      pos == c.marker('private-constructor')
    }
    with(c.message()) {
      // id == TODO (2020-04-11) ID
      pos == c.marker('public-nested-of-private-not-accessible')
    }
    and:
    c.allGood
  }
}
