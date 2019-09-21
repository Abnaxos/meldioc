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
import ch.raffael.compose.processor.test.meta.Bad
import ch.raffael.compose.processor.test.meta.Good
import spock.lang.Specification

import static ch.raffael.compose.processor.test.tools.ProcessorTestCase.compile

class MountsSpec extends Specification {

  @Good
  def "Provisions in a mount that are not provided by the mounting configuration stay local to that mount"() {
    when:
    def c = compile('c/mounts/good/localProvisions/direct')

    then:
    c.allGood
    and:
    def ctx = c.shellBuilder().build()
    c.loadClass('.Mounted').isInstance(ctx.mounted())
    c.loadClass('c.ProvisionA').isInstance(ctx.mounted().mounted())
    c.loadClass('c.ProvisionB').isInstance(ctx.mounted2().mounted())
  }

  /**
   * Situation:
   *
   * <ul>
   *   <li>A mounted feature provides provisionA <em>locally</em>
   *   <li>Another mounted feature depends on provisionA <em>locally</em>
   * </ul>
   *
   * <p>This should be a compiler error as there are ways to resolve that
   * without exposing the feature globally. We <em>could</em> also automatically
   * use that provided local provision in the dependent feature, but that's
   * too obscure. It can be done, but it must be done explicitly.
   */
  @Bad
  def "Local provisions provided by one mount are forwarded to another mount depending on them"() {
    when:
    def c = compile('c/mounts/bad/localProvisions/indirect')

    then:
    with(c.message()) {
      id == Message.Id.MountedAbstractProvisionHasNoImplementationCandidate
      pos == c.marker('no-impl')
    }
    c.allGood
  }
}
