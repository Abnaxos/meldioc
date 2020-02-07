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

package ch.raffael.compose.library.base.jmx.std

import ch.raffael.compose.library.base.jmx.JmxRegistrar
import ch.raffael.compose.library.base.jmx.util.DomainMappings
import spock.lang.Specification

import javax.management.ObjectName

import static ch.raffael.compose.logging.Logging.logger

class RegistrationBuilderSpec extends Specification {

  static final String DOMAIN = 'my.domain'

  List<Tuple2<Object, ObjectName>> registrations = []
  AbstractRegistrationBuilder builder = new AbstractRegistrationBuilder<Object>(logger(), DomainMappings.of(DOMAIN)) {
    @Override
    protected Object preRegister(Object key) {
      return key
    }
    @Override
    protected void postRegister(Object state) {
    }
    @Override
    protected void doRegister(Object state, Object mbean, ObjectName objectName) throws Exception {
      registrations.add(new Tuple2(mbean, objectName))
    }
  }


  def "Standard registration with direct MBean"() {
    given: "An MBean"
    def mbean = new MyManagedMBean.Impl()

    when: "Register the MBean"
    builder.register(mbean)

    then: "The bean was registered"
    registrations.size() == 1
    and: "The object name matches our expectations"
    with(registrations[0].second) {
      domain == DOMAIN
      keyPropertyList.size() == 1
      keyPropertyList['type'] == 'MyManaged'
    }
  }

  def "Standard registration with managed and MBeanFactory"() {
    given: "A managed object and an MBeanFactory"
    def managed = new MyManaged()
    def factory = Mock(JmxRegistrar.MBeanFactory)

    when: "Register the MBean"
    builder.register(factory, managed)

    then: "The bean was registered"
    registrations.size() == 1
    and: "The object name matches our expectations"
    with(registrations[0]) {
      with(second) {
        domain == DOMAIN
        keyPropertyList.size() == 1
        keyPropertyList['type'] == 'MyManaged'
      }
    }
    and: "Our MBeanFactory was used"
    registrations[0].first instanceof MyManagedMBean
    1 * factory.mbeanFor({it.is managed}) >> {MyManaged m -> MyManagedMBean.FACTORY.mbeanFor(m)}
    0 * factory._
  }
}
