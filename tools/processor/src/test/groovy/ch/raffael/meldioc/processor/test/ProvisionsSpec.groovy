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

package ch.raffael.meldioc.processor.test

import ch.raffael.meldioc.model.messages.Message
import spock.lang.Specification

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile

class ProvisionsSpec extends Specification {

  def "Directly implemented provisions work fine including shared/non-shared"() {
    when:
    def c = compile('c/provisions/implemented')

    then:
    c.allGood
    and:
    def s = c.context()
    s.a() != null
    s.a() != s.a()
    and:
    s.b() != null
    s.b() == s.b()
  }

  def "An unshared provision overriding a shared one without `override=true` is a compiler error"() {
    when:
    def c = compile('c/provisions/unsharedOverridesShared')

    then: "ErrNonSharedFeatureA has a compiler error"
    with (c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('problematic-override')
    }
    and: "No more errors (specifically, NonSharedFeatureA specifying override=true is good)"
    c.allGood
  }

  def "Overriding a provision as unshared when at least one of the declaring interfaces declares it as shared is an error"() {
    when:
    def c = compile('c/provisions/inheritance/interfaceSharedWins')

    then: "Compiler errors in ErrContextConflict and ErrContextConflictReverse"
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('shared-conflict')
    }
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('shared-conflict-reverse')
    }
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('merged-conflict')
    }
    and: "No more errors (specifically, BothUnsharedContext is good)"
    c.allGood
  }

  def "Inheriting an unshared provision from a class and implementing an interface with the same shared provision is an error"() {
    when:
    def c = compile('c/provisions/inheritance/classSharedWins')

    then: "Conflicting provisions when inherited unshared implements shared"
    with(c.message()) {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('conflicting-provisions')
    }
    and: "No more compiler errors (specifically, inherited shared implementing unshared is fine)"
    c.allGood
  }

  /**
   * In the overridden mounted class in the shell, the provision is shared.
   * By redirecting to the mount, this shared instance will always be
   * retrieved, even if the provision is declared non-shared. It's not an
   * error to do so because in the interface the context implements, the
   * provision is unshared.
   *
   * <p>[discussion] It may be appropriate to emit a warning in such cases.
   */
  def "An unshared configuration provision redirecting to a mounted shared provision is actually shared"() {
    when:
    def c = compile('c/provisions/_edge/unsharedOverrideDelegatedToSharedMounted')

    then:
    def s = c.context()
    s.a() == s.a()
  }
}
