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

package ch.raffael.compose.processor.test

import ch.raffael.compose.model.messages.Message
import ch.raffael.compose.processor.test.meta.Fine
import ch.raffael.compose.processor.test.meta.Flawed
import spock.lang.Specification

import static ch.raffael.compose.processor.test.tools.ProcessorTestCase.compile


class MountAttributeSpec extends Specification {

  @Fine
  def "Mixed mount method/attribute"() {
    when:
    def c = compile('c/mountAttribute/fine/mixed')

    then:
    c.allFine
    and:
    with(c.context()) {
      a() != null
      a() == a()
      b() != null
      b() != b()
    }
  }

  @Fine
  def "Mount attribute only"() {
    when:
    def c = compile('c/mountAttribute/fine/allAttr')

    then:
    c.allFine
    and:
    with(c.context()) {
      a() != null
      a() == a()
      b() != null
      b() != b()
    }
  }

  @Fine
  def "Specifying a class that extends a class with type parameters but has no type parameters itself is fine"() {
    when:
    def c = compile('c/mountAttribute/fine/generics')

    then:
    c.allFine
    and:
    c.context().t() != null
  }

  @Flawed
  def "Specifying a class with type parameters in the `mount` attribute is a compiler error"() {
    when:
    def c = compile('c/mountAttribute/flawed/generics')

    then:
    with(c.findMessage {it.id == Message.Id.MountAttributeClassMustNotBeParametrized}) {
      pos.line == c.marker('generic-mount').line
      message.contains("'FeatureT.Generic'")
    }
    c.allFine
  }
}
