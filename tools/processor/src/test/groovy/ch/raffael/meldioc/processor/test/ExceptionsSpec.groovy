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

import ch.raffael.meldioc.model.messages.Message
import ch.raffael.meldioc.processor.test.meta.Issue
import spock.lang.Specification

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile

class ExceptionsSpec extends Specification {

  @Issue(63)
  def "Inconsistent throws clause in mounts causes a compiler error"() {
    when:
    def c = compile('c/exceptions/provision/single')

    then:
    with(c.findMessage {it.id == Message.Id.IncompatibleThrowsClause}) {
      pos == c.marker('single')
    }
    c.allGood

    when:
    c = compile('c/exceptions/provision/multi')

    then:
    with(c.findMessage {it.id == Message.Id.IncompatibleThrowsClause}) {
      pos == c.marker('multi')
    }
    c.allGood
  }

  @Issue(68)
  def "When an extension point provision throws an exception, it's added to the throws clause of the constructor"() {
    when:
    def c = compile('c/exceptions/extensionPoint')

    then: "No compiler errors"
    c.allGood
    and: "The builder method declares the exception to be thrown"
    c.loadClass('c.exceptions.extensionPoint.ErrThrowingExtensionPointShell$Builder')
        .getDeclaredMethod('build').exceptionTypes as Set == [IOException.class, InterruptedException.class] as Set
  }
}
