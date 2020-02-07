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

package ch.raffael.compose.library.base.jmx;

import io.vavr.control.Option;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.ObjectName;

/**
 * The JMX registrar registers MBeans to a given MBean server applying
 * certain rules.
 */
public interface JmxRegistrar {

  RegistrationBuilder registrationBuilder();

  default <T> T register(MBeanFactory<? super T> factory, T managed) {
    return registrationBuilder().register(factory, managed);
  }

  default <T> T register(T mbean) {
    return registrationBuilder().register(mbean);
  }

  Option<ObjectName> nameOf(Object object);

  boolean unregister(Object object) throws MBeanRegistrationException, InstanceNotFoundException;

  JmxRegistrar withDefaultDomain(String name);

  JmxRegistrar withFixedDomain(String name);

  @FunctionalInterface
  interface MBeanFactory<T> {
    Object mbeanFor(T managed) throws Exception;
  }
}
