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
import spock.lang.Specification

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile

class ProvisionsSpec extends Specification {

  def "Directly implemented provisions work fine including non-/singleton"() {
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

  def "A non-singleton provision overriding a singleton one without `override=true` is a compiler error"() {
    when:
    def c = compile('c/provisions/nonSingletonOverridesSingleton')

    then: "ErrNonSingletonFeatureA has a compiler error"
    with (c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('problematic-override')
    }
    and: "No more errors (specifically, NonSingletonFeatureA specifying override=true is good)"
    c.allGood
  }

  def "Overriding a provision as non-singleton when at least one of the declaring interfaces declares it as singleton is an error"() {
    when:
    def c = compile('c/provisions/inheritance/interfaceSingletonWins')

    then: "Compiler errors in ErrContextConflict and ErrContextConflictReverse"
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('singleton-conflict')
    }
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('singleton-conflict-reverse')
    }
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('merged-conflict')
    }
    and: "No more errors (specifically, BothNonSingletonContext is good)"
    c.allGood
  }

  def "Inheriting an non-singleton provision from a class and implementing an interface with the same singleton provision is an error"() {
    when:
    def c = compile('c/provisions/inheritance/classSingletonWins')

    then: "Conflicting provisions when inherited non-singleton implements singleton"
    with(c.message()) {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('conflicting-provisions')
    }
    and: "No more compiler errors (specifically, inherited singleton implementing non-singleton is fine)"
    c.allGood
  }

  def "Non-/singleton conflicts involving provisions implemented as default methods in interfaces"() {
    when:
    def c = compile('c/provisions/inheritance/interfaceDefaultMethods')

    then: "Inheriting non-singleton from class, singleton from interface is a conflict"
    with(c.message()) {
      id == Message.Id.ConflictingProvisions
      pos == c.marker('conflict')
    }
    then: "Inheriting singleton from class, non-singleton from interface results in a singleton provision (Compiler error in implicit downgrade)"
    with(c.message()) {
      id == Message.Id.ProvisionOverrideMissing
      pos == c.marker('problematic-downgrade')
    }
    then: "No further errors, specificially, ContextUsingIfaceDefault is using the default implementation from the interface"
    c.allGood
  }

  def "Methods that are not provisions in feature interfaces cause a warning"() {
    when:
    def c = compile('c/provisions/iface')

    then:
    with(c.message()) {
      id == Message.Id.FeatureInterfacesShouldDeclareProvisionsOnly
      pos == c.marker('inherited-non-provision')
      message.contains('(method provisionB inherited from NonProvisionInterface)')
    }
    with(c.message()) {
      id == Message.Id.FeatureInterfacesShouldDeclareProvisionsOnly
      pos == c.marker('non-provision')
      !message.contains('inherited from')
    }
    c.allGood
  }
}
