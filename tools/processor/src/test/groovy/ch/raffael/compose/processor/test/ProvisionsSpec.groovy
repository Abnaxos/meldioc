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
import ch.raffael.compose.processor.test.meta.Captious
import ch.raffael.compose.processor.test.meta.Fine
import ch.raffael.compose.processor.test.meta.Flawed
import ch.raffael.compose.processor.test.meta.Issue
import spock.lang.PendingFeature
import spock.lang.Specification

import static ch.raffael.compose.processor.test.tools.ProcessorTestCase.compile

class ProvisionsSpec extends Specification {

  @Fine
  def "Directly implemented provisions work fine including shared/non-shared"() {
    when:
    def c = compile('c/provisions/fine/implemented')

    then:
    c.allFine
    and:
    def s = c.context()
    s.a() != null
    s.a() != s.a()
    and:
    s.b() != null
    s.b() == s.b()
  }

  @Fine
  def "Mounted implemented provisions work fine including shared/non-shared"() {
    when:
    def c = compile('c/provisions/fine/mounted')

    then:
    c.allFine
    and:
    def s = c.context()
    s.a() != null
    s.a() == s.a()
    and:
    s.b() != null
    s.b() != s.b()
  }

  @Fine
  def "An unshared provision overriding a shared specifying `override=true` is fine"() {
    when:
    def c = compile('c/provisions/fine/unsharedOverridesShared')

    then:
    c.allFine
  }

  @Flawed
  def "An unshared provision overriding a shared one without `override=true` is a compiler error"() {
    when:
    def c = compile('c/provisions/flawed/unsharedOverridesShared')

    then:
    with (c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('problematic-override')
    }
  }

  @Flawed
  def "A duplicate provision from two different mounts is a compiler error"() {
    when:
    def c = compile('c/provisions/flawed/conflictingProvisionFromMounts')

    then:
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('conflicting-provisions')
    }
    c.allFine
  }

  @Flawed
  @Issue(4)
  @PendingFeature
  def "If an abstract provision from a module can't be forwarded anywhere, it is an error"() {
    when:
    def c = compile('c/provisions/flawed/abstractMountedProvisionNotImplementable')

    then:
    // TODO (2019-08-11) expect some error here
    c.allFine // we're actually getting a compiler error in the generated shell code here
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
  @Captious
  def "An unshared configuration provision redirecting to a mounted shared provision is actually shared"() {
    when:
    def c = compile('c/provisions/captious/unsharedOverrideDelegatedToSharedMounted')

    then:
    def s = c.context()
    s.a() == s.a()
  }
}
