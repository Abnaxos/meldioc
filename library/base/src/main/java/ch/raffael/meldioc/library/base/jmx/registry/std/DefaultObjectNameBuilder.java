/*
 *  Copyright (c) 2020 Raffael Herzog
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

package ch.raffael.meldioc.library.base.jmx.registry.std;

import ch.raffael.meldioc.library.base.jmx.registry.ObjectNameBuilder;
import ch.raffael.meldioc.library.base.jmx.registry.util.ObjectNames;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Default implementation of {@link ObjectNameBuilder}.
 */
public class DefaultObjectNameBuilder implements ObjectNameBuilder {

  private Option<String> type = none();
  private Option<String> name = none();
  private Option<String> domain = none();
  private Map<String, String> properties = LinkedHashMap.empty();

  public DefaultObjectNameBuilder() {
  }

  @Override
  public DefaultObjectNameBuilder type(String type) {
    this.type = some(type);
    return this;
  }

  @Override
  public DefaultObjectNameBuilder type(Class<?> type, boolean verbatim) {
    this.type = some(ObjectNames.typeName(type, verbatim));
    return this;
  }

  public Option<String> type() {
    return type;
  }

  public Option<String> name() {
    return name;
  }

  @Override
  public DefaultObjectNameBuilder name(String name) {
    this.name = some(name);
    return this;
  }

  @Override
  public DefaultObjectNameBuilder property(String key, String value) {
    switch (key) {
      case TYPE_PROPERTY:
        type(value);
        break;
      case NAME_PROPERTY:
        name(value);
        break;
      default:
        properties = properties.put(key, value);
    }
    return this;
  }

  @Override
  public DefaultObjectNameBuilder properties(Map<String, String> properties) {
    properties.forEach(this::property);
    return this;
  }

  @Override
  public DefaultObjectNameBuilder properties(java.util.Map<String, String> properties) {
    properties.forEach(this::property);
    return this;
  }

  public Map<String, String> properties() {
    return properties;
  }

  @Override
  public DefaultObjectNameBuilder domain(String domain) {
    this.domain = some(domain);
    return this;
  }

  public Option<String> domain() {
    return domain;
  }

  @Override
  public ObjectName toObjectName() throws MalformedObjectNameException {
    if (type.isEmpty() && name.isEmpty() && properties.isEmpty()) {
      throw new IllegalStateException("No properties specified");
    }
    var objName = new StringBuilder();
    objName.append(ObjectNames.quoteIfNecessary(domain
        .getOrElseThrow(() -> new IllegalStateException("No domain specified"))));
    objName.append(":");
    objName.append(type.map(t -> TYPE_PROPERTY + "=" + ObjectNames.quoteIfNecessary(t) + ",").getOrElse(""));
    objName.append(name.map(n -> NAME_PROPERTY + "=" + ObjectNames.quoteIfNecessary(n) + ",").getOrElse(""));
    properties.forEach((k, v) -> {
      objName.append(k).append('=').append(ObjectNames.quoteIfNecessary(v)).append(",");
    });
    if (objName.charAt(objName.length()-1) == ',') {
      objName.delete(objName.length() - 1, objName.length());
    }
    return new ObjectName(objName.toString());
  }
}
