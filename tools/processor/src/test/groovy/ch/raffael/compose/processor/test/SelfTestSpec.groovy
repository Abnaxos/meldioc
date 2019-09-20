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

import ch.raffael.compose.processor.test.meta.Bad
import ch.raffael.compose.processor.test.meta.Good
import spock.lang.Specification

import static ch.raffael.compose.processor.test.tools.ProcessorTestCase.compile

class SelfTestSpec extends Specification {

  @Good
  def "Self test: hello world all good"() {
    when:
    def c = compile('selfTest/good')

    then:
    c.allGood
    with(c.marker('class')) {
      file == 'HelloWorld.java'
      line == 29
      col == 8
    }
    with(c.marker('method')) {
      file == 'HelloWorld.java'
      line == 32
      col == 22
    }
    with(c.marker('param')) {
      file == 'HelloWorld.java'
      line == 32
      col == 53
    }
  }

  @Bad
  def "Self test: hello world with error"() {
    when:
    def c = compile('selfTest/bad')

    then:
    with(c.message()) {
      message.startsWith 'missing method body'
      pos.file == 'HelloWorld.java'
    }
    c.allGood
  }
}
