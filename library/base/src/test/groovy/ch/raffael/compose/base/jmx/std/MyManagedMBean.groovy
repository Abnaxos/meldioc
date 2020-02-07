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

import ch.raffael.compose.base.jmx.JmxRegistrar

import javax.management.MXBean
import javax.management.StandardMBean

interface MyManagedMBean {

  static final JmxRegistrar.MBeanFactory<MyManaged> FACTORY = {m -> new Impl(m)}

  int getObjectId()

  @MXBean
  class Impl extends StandardMBean implements MyManagedMBean {

    MyManaged myManaged

    Impl(MyManaged myManaged) {
      super(MyManagedMBean)
      this.myManaged = myManaged
    }

    @Override
    int getObjectId() {
      myManaged ? System.identityHashCode(myManaged) : 0
    }
  }
}
