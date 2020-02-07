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

package ch.raffael.compose.base.jmx.std


import ch.raffael.compose.base.jmx.util.DomainMappings
import ch.raffael.compose.base.jmx.util.ObjectNames
import spock.lang.Specification

import javax.management.InstanceNotFoundException
import javax.management.MBeanServer
import javax.management.ObjectName
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicInteger

class DefaultJmxRegistrarSpec extends Specification {

  static final AtomicInteger COUNTER = new AtomicInteger()
  public static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer()

  String domain = ObjectNames.packageName(DefaultJmxRegistrarSpec) + COUNTER.getAndIncrement()
  DomainMappings domainMappings = DomainMappings.of(domain)
  DefaultJmxRegistrar registrar = new DefaultJmxRegistrar(SERVER, domainMappings)

  def cleanup() {
    registrar.shutdown()
  }

  def "Beans can be registered and unregistered by the managed object"() {
    given: "A managed object"
    def m1 = new MyManaged()

    when: "Register one of the managed objects"
    registrar.register(MyManagedMBean.FACTORY, m1)
    and: "Lookup the info of that MBean"
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged"))

    then: "The bean is registered in the MBean server"
    notThrown InstanceNotFoundException

    when: "Unregister the MBean"
    def ret = registrar.unregister(m1)
    and: "Try to lookup the MBean info again"
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged"))

    then: "The bean could not be found"
    thrown InstanceNotFoundException
    and: "The registrar returned true on unregister"
    ret

    when: "Try to unregister an MBean that was never registered"
    ret = registrar.unregister(new MyManaged())

    then: "The registrar returned false on unregister"
    !ret
  }

  def "All beans will be unregistered on registrar shutdown"() {
    given: "Tow managed objects"
    def m1 = new MyManaged()
    def m2 = new MyManaged()

    when: "Register the MBeans with two different names"
    registrar.registrationBuilder()
        .name('a')
        .register(MyManagedMBean.FACTORY, m1)
    registrar.registrationBuilder()
        .name('b')
        .register(MyManagedMBean.FACTORY, m2)
    and: "Lookup these beans"
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged,name=a"))
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged,name=b"))

    then: "The bean lookup was successful"
    notThrown InstanceNotFoundException

    when: "Shutdown the registrar"
    registrar.shutdown()
    and: "Lookup the first bean"
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged,name=a"))

    then: "The bean could not be found"
    thrown InstanceNotFoundException

    when: "Lookup the second bean"
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged,name=b"))

    then: "The bean could not be found"
    thrown InstanceNotFoundException
  }

  def "An object cannot be registered twice"() {
    given: "A managed object"
    def m = new MyManaged()

    when: "Register the object once and look it up"
    registrar.register(MyManagedMBean.FACTORY, m)
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged"))
    then: "No exception is thrown"
    notThrown()

    when: "Attempt to register the object again"
    registrar.register(MyManagedMBean.FACTORY, m)

    then: "An IllegalStateException is thrown"
    def e = thrown IllegalStateException
    e.message.startsWith('MBean registration already present')

    when: "Unregister the object"
    registrar.unregister(m)
    and: "Try to look it up"
    SERVER.getMBeanInfo(new ObjectName("$domain:type=MyManaged"))

    then: "The bean is now unregistered normally"
    thrown InstanceNotFoundException

    when: "Register the object again"
    registrar.register(MyManagedMBean.FACTORY, m)
    then: "Now it succeeds again"
    notThrown()
  }

}
