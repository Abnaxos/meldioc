/*
 *  Copyright (c) 2021 Raffael Herzog
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
import spock.lang.Specification

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile

class MountsSpec extends Specification {

  def "Mounted implemented provisions work fine including non-/singleton"() {
    when:
    def c = compile('c/mounts/basic')

    then:
    c.allGood
    and:
    def s = c.context()
    s.a() != null
    s.a() == s.a()
    and:
    s.b() != null
    s.b() != s.b()
  }

  def "Provisions in a mount that are not provided by the mounting configuration stay local to that mount"() {
    when:
    def c = compile('c/mounts/localProvisions/direct')

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
  def "Local provisions provided by one mount are forwarded to another mount depending on them"() {
    when:
    def c = compile('c/mounts/localProvisions/indirect')

    then:
    with(c.message()) {
      id == Message.Id.MountedAbstractProvisionHasNoImplementationCandidate
      pos == c.marker('no-impl')
    }
    c.allGood
  }

  def "A duplicate provision from two different mounts is a compiler error"() {
    when:
    def c = compile('c/mounts/conflictingProvision')

    then: "Compiler error in context without explicit override"
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('conflicting-provisions')
    }
    and: "No more errors (specifically, the context with explicit override is fine)"
    c.allGood
  }

  def "If an abstract provision from a module can't be forwarded anywhere, it is an error, except, if it's from an injected mount"() {
    when:
    def c = compile('c/mounts/abstractMountedProvisionNotImplementable')

    then:
    with(c.findMessage {it.id == Message.Id.MountedAbstractProvisionHasNoImplementationCandidate}) {
      pos == c.marker('mount-a')
    }
    c.allGood
  }
}
