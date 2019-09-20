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

package ch.raffael.compose.core.jmx;

import io.vavr.collection.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.function.Consumer;

/**
 * TODO JavaDoc
 */
public interface RegistrationBuilder extends ObjectNameBuilder {

  RegistrationBuilder type(String type);

  RegistrationBuilder type(Class<?> type, boolean verbatim);

  default RegistrationBuilder type(Class<?> type) {
    ObjectNameBuilder.super.type(type);
    return this;
  }

  RegistrationBuilder clearType();

  RegistrationBuilder name(String name);

  RegistrationBuilder property(String name, String value);

  RegistrationBuilder properties(Map<String, String> properties);

  RegistrationBuilder properties(java.util.Map<String, String> properties);

  RegistrationBuilder domain(String domain);

  RegistrationBuilder onError(Consumer<? super Throwable> errorHandler);

  <T> T register(JmxRegistrar.MBeanFactory<? super T> factory, T managed);

  <T> T register(T mbean);

  ObjectName toObjectName(Object object) throws MalformedObjectNameException;
}
