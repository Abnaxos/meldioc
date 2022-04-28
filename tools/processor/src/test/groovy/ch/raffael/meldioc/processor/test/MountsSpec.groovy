/*
 *  Copyright (c) 2022 Raffael Herzog
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
import spock.lang.Unroll

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

  def "A mounted 'local' provision is forwarded to the mounting configuration that doesn't declare it as if it was declared"() {
    when:
    def c = compile('c/mounts/noLocalProvisions/localOnly/basic')

    then:
    c.allGood
    and:
    c.loadClass('.ContextShell$$Dispatcher').getDeclaredMethod('a')
  }

  def "When a mounted provision causes a method collision in the configurations, it's reported as an error"() {
    when:
    def c = compile('c/mounts/declaredMethodCollisions')

    then:
    with(c.message()) {
      id == Message.Id.MountedProvisionOverridesMethod
      pos == c.marker('override-b-inherited')
    }
    with(c.message()) {
      id == Message.Id.MountedProvisionOverridesMethod
      pos == c.marker('override-a')
    }
    and:
    with(c.message()) {
      id == Message.Id.MountedProvisionOverridesMethod
      pos == c.marker('mount-same-name-as-provision')
    }
    and:
    c.allGood
  }

  def "Local provisions will be shared, so an conflict will be reported if 2 mounted features provide the same for each of the mount methods"() {
    when:
    def c = compile('c/mounts/noLocalProvisions/conflict')

    then:
    with(c.message()) {
      id == Message.Id.ConflictingProvisions
      pos == c.marker("mount")
      message.contains("'mountedProvision'")
    }
    with(c.message()) {
      id == Message.Id.ConflictingProvisions
      pos == c.marker("mount2")
      message.contains("'mountedProvision'")
    }
    and:
    c.allGood
  }

  /**
   * Situation:
   *
   * <ul>
   *   <li>A mounted feature provides provisionA <em>locally</em>
   *   <li>Another mounted feature depends on provisionA <em>locally</em>
   * </ul>
   *
   * Since the mounted provisionA is inherited in the configuration, the
   * feature depending on provisionA uses that implementation.
   */
  def "Local provisions are inherited to the configuration and therefore shared"() {
    when:
    def c = compile('c/mounts/noLocalProvisions/sharing')

    then:
    c.allGood
    and:
    def ctx = c.shellBuilder().build()
    ctx.a().is ctx.forwardToA()
    ctx.getClass().getDeclaredMethod('a').getReturnType() == c.loadClass('c.ProvisionA')
  }

  def "A duplicate provision from two different mounts is a compiler error"() {
    when:
    def c = compile('c/mounts/conflictingProvision')

    then: "Error about conflicting provisions where override is not overridden"
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('without-override-inherited')
    }
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('without-override-mounted-1')
    }
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('without-override-mounted-2')
    }
    and:
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('with-override-mounted-1')
    }
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('with-override-mounted-2')
    }
    with(c.message())  {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('with-override-declared')
    }
    and: "No more errors (specifically, the context with explicit override is fine)"
    c.allGood
  }

  def "If an abstract mounted provision isn't implemented anywhere, it is an error, except; all provisions from injected mounts are considered implemented"() {
    when:
    def c = compile('c/mounts/abstractMountedProvisionNotImplementable')

    then:
    with(c.findMessage {it.id == Message.Id.UnresolvedProvision}) {
      pos == c.marker('mount-a')
    }
    and:
    c.allGood
  }

  def "When the implementation of a provision is covariant with all abstract provision, the implementation type wins"() {
    when:
    def c = compile('c/mounts/types/covariant')

    then:
    c.allGood
    and: "The implied provision uses the actual type"
    c.loadDispatcherClass('.Context').getDeclaredMethod('a').getReturnType().name.endsWith '.ProvisionA2'
    and: "The declared provision uses the declared type" // TODO (2022-04-26) make that the covariant type
    c.loadDispatcherClass('.ContextWithDeclared').getDeclaredMethod('a').getReturnType().name.endsWith '.ProvisionA'
  }

  @Unroll
  def "Incompatible provision types are reported as error (#mode)"() {
    given:
    def validMarkers = ['mounted-conflict', 'declared-conflict']
    when:
    def c = compile("c/mounts/types/incompatible/$mode")

    then: "Verify test: check markers"
    c.markers.keySet().findAll {!validMarkers.contains(it)}.empty
    !c.markers.empty

    then: "The errors were reported as expected"
    def mounted = c.marker('mounted-conflict')
    def mountedMsg = c.findMessage({it.pos == mounted})
    def declared = c.marker('declared-conflict')
    def declaredMsg = c.findMessage({it.pos == declared})
    !mounted || mountedMsg.id == Message.Id.IncompatibleProvisionTypes
    !declared || declaredMsg.id == Message.Id.IncompatibleProvisionTypes

    where:
    mode << ['implied', 'declaredOk', 'declaredConflict', 'implemented']
  }

  @Issue(63)
  def "Inconsistent throws clause causes a compiler error (#mode)"() {
    when:
    def c = compile("c/mounts/exceptions/$mode")

    then:
    with(c.findMessage {it.id == Message.Id.IncompatibleProvisionThrows}) {
      pos == c.marker('throws-conflict')
    }
    c.allGood

    where:
    mode << ['conflictWithInherited', 'conflictWith2Inherited', 'conflictWithLocalImpl', /*'conflictWith2Mounts'*/]
  }

  def "Implied provisions copy the exceptions from their implementation"() {
    when:
    def c = compile('c/mounts/exceptions/impliedCopiesThrown')

    then: "No compiler errors"
    c.allGood
    and:
    c.loadDispatcherClass('.Context').getDeclaredMethod('a')
        .getExceptionTypes() as List == [IOException]
  }
}
