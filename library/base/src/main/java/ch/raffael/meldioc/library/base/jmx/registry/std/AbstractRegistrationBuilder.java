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

import ch.raffael.meldioc.library.base.jmx.registry.MBeanRegistry;
import ch.raffael.meldioc.library.base.jmx.registry.RegistrationBuilder;
import ch.raffael.meldioc.library.base.jmx.registry.util.DomainMappings;
import ch.raffael.meldioc.library.base.jmx.registry.util.ObjectNames;
import ch.raffael.meldioc.util.Exceptions;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.slf4j.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.function.Consumer;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Default implementation of {@link RegistrationBuilder}.
 */
public abstract class AbstractRegistrationBuilder<S> implements RegistrationBuilder {

  private final Logger logger;
  private final DefaultObjectNameBuilder nameBuilder = new DefaultObjectNameBuilder();
  private final DomainMappings mappings;
  private Option<Consumer<? super Throwable>> errorHandler = none();
  private boolean verbatimType = false;
  private boolean skipTypeProperty = false;

  public AbstractRegistrationBuilder(Logger logger, DomainMappings mappings)
  {
    this.logger = logger;
    this.mappings = mappings;
  }

  @Override
  public RegistrationBuilder type(String type) {
    nameBuilder.type(type);
    return this;
  }

  @Override
  public RegistrationBuilder type(Class<?> type, boolean verbatim) {
    nameBuilder.type(type, verbatim);
    return this;
  }

  @Override
  public RegistrationBuilder clearType() {
    skipTypeProperty = true;
    return this;
  }

  @Override
  public RegistrationBuilder name(String name) {
    nameBuilder.name(name);
    return this;
  }

  @Override
  public RegistrationBuilder property(String key, String value) {
    nameBuilder.property(key, value);
    return this;
  }

  @Override
  public RegistrationBuilder properties(Map<String, String> properties) {
    nameBuilder.properties(properties);
    return this;
  }

  @Override
  public RegistrationBuilder properties(java.util.Map<String, String> properties) {
    nameBuilder.properties(properties);
    return this;
  }

  @Override
  public RegistrationBuilder domain(String domain) {
    nameBuilder.domain(domain);
    return this;
  }

  @Override
  public RegistrationBuilder onError(Consumer<? super Throwable> errorHandler) {
    this.errorHandler = some(errorHandler);
    return this;
  }

  @Override
  public <T> T register(MBeanRegistry.MBeanFactory<? super T> factory, T managed) {
    var state = preRegister(managed);
    try {
      var mbean = factory.mbeanFor(managed);
      var objectName = toObjectName(managed);
      logger.debug("Registering MBean {} for {} as {}", mbean, managed, objectName);
      doRegister(state, mbean, objectName);
    } catch (Exception e) {
      logger.error("Error registering MBean for {}", managed, e);
      if (errorHandler.isDefined()) {
        try {
          errorHandler.get().accept(e);
        } catch (Throwable e2) {
          Exceptions.rethrowIfFatal(e2, e);
          e.addSuppressed(e2);
        }
      }
    } finally {
      postRegister(state);
    }
    return managed;
  }

  @Override
  public <T> T register(T mbean) {
    var state = preRegister(mbean);
    try {
      ObjectName objectName = toObjectName(mbean);
      logger.debug("Registering MBean {} as {}", mbean, objectName);
      doRegister(state, mbean, objectName);
    } catch (Exception e) {
      logger.debug("Error registering MBean {}", mbean, e);
      if (errorHandler.isDefined()) {
        try {
          errorHandler.get().accept(e);
        } catch (Throwable e2) {
          Exceptions.rethrowIfFatal(e2, e);
          e.addSuppressed(e2);
        }
      }
    } finally {
      postRegister(state);
    }
    return mbean;
  }

  protected abstract S preRegister(Object key);

  protected abstract void postRegister(S state);

  protected abstract void doRegister(S state, Object mbean, ObjectName objectName) throws Exception;

  @Override
  public ObjectName toObjectName(Object object) throws MalformedObjectNameException {
    if (!skipTypeProperty) {
      nameBuilder.type(nameBuilder.type().getOrElse(ObjectNames.typeName(object.getClass(), verbatimType)));
    }
    nameBuilder.domain(nameBuilder.domain().getOrElse(mappings.domainFor(object.getClass())));
    return nameBuilder.toObjectName();
  }

  @Override
  public ObjectName toObjectName() throws MalformedObjectNameException {
    return nameBuilder.toObjectName();
  }
}
