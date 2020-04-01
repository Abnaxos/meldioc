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
import io.vavr.control.Option;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ch.raffael.meldioc.logging.Logging.logger;
import static io.vavr.API.*;

public class DefaultMBeanRegistry implements MBeanRegistry {

  private static final Logger LOG = logger();

  private final IdentityHashMap<Object, Registration> registrations = new IdentityHashMap<>();
  private final MBeanServer server;
  private final DomainMappings domainMappings;
  private final ReadWriteLock shutdownLock = new ReentrantReadWriteLock();
  private boolean shutdown = false;

  protected DefaultMBeanRegistry(MBeanServer server, DomainMappings domainMappings) {
    this.server = server;
    this.domainMappings = domainMappings;
  }

  public static Handle create(MBeanServer server, DomainMappings domainMappings) {
    return new Handle(new DefaultMBeanRegistry(server, domainMappings));
  }

  @Override
  public RegistrationBuilder registrationBuilder() {
    checkNotShutdown();
    return new MyRegistrationBuilder();
  }

  @Override
  public Option<ObjectName> nameOf(Object object) {
    synchronized (registrations) {
      return Option(registrations.get(object)).flatMap(r -> Option(r.objectName));
    }
  }

  @Override
  public boolean unregister(Object object) throws MBeanRegistrationException {
    Registration reg = claimRegistration(object, false);
    if (reg == null) {
      return false;
    }
    reg.lock.lock();
    try {
      if (reg.objectName == null) {
        return false;
      }
      try {
        LOG.debug("Unregistering MBean: {}", reg);
        server.unregisterMBean(reg.objectName);
        reg.objectName = null;
      } catch (InstanceNotFoundException e) {
        LOG.warn("Instance of registered MBean not found: {}", reg, e);
        reg.objectName = null;
        return false;
      }
      return true;
    } finally {
      reg.lock.unlock();
      releaseRegistration(reg);
    }
  }

  protected void shutdown() {
    shutdownLock.writeLock().lock();
    try {
      if (shutdown) {
        return;
      }
      synchronized (registrations) {
        while (!registrations.isEmpty()) {
          var reg = registrations.entrySet().iterator().next().getValue();
          try {
            unregister(reg.key);
          } catch (Exception e) {
            LOG.error("Error unregistering {}", reg);
          }
        }
      }
    } finally {
      shutdown = true;
      shutdownLock.writeLock().lock();
    }
  }

  private void checkNotShutdown() {
    shutdownLock.readLock().lock();
    try {
      if (shutdown) {
        throw new IllegalStateException("Shutdown");
      }
    } finally {
      shutdownLock.readLock().unlock();
    }
  }

  @Override
  public MBeanRegistry withDefaultDomain(String name) {
    return new DefaultMBeanRegistry(server, domainMappings.withDefaultDomain(name));
  }

  @Override
  public MBeanRegistry withFixedDomain(String name) {
    return new DefaultMBeanRegistry(server, DomainMappings.of(name));
  }

  @Nullable
  private Registration claimRegistration(Object key, boolean create) {
    synchronized (registrations) {
      var reg = registrations.get(key);
      if (reg == null && create) {
        reg = new Registration(key);
        registrations.put(key, reg);
      }
      if (reg != null) {
        reg.claimedCount++;
      }
      return reg;
    }
  }

  private void releaseRegistration(Registration reg) {
    synchronized (registrations) {
      reg.claimedCount--;
      if (reg.claimedCount <= 0 && reg.objectName == null) {
        registrations.remove(reg.key);
      }
    }
  }

  public static class Handle {
    private final DefaultMBeanRegistry registry;

    protected Handle(DefaultMBeanRegistry registry) {
      this.registry = registry;
    }

    public DefaultMBeanRegistry registry() {
      return registry;
    }

    public void shutdown() {
      registry.shutdown();
    }

    @Override
    public String toString() {
      return "Handle<" + registry.toString() + ">";
    }
  }

  private class MyRegistrationBuilder extends AbstractRegistrationBuilder<Registration> {
    public MyRegistrationBuilder() {
      super(LOG, domainMappings);
    }

    @Override
    protected Registration preRegister(Object key) {
      checkNotShutdown();
      Objects.requireNonNull(key, "key");
      Registration reg = claimRegistration(key, true);
      assert reg != null;
      reg.lock.lock();
      boolean success = false;
      try {
        if (reg.objectName != null) {
          throw new IllegalStateException("MBean registration already present for " + key + " as " + reg.objectName);
        }
        success = true;
        return reg;
      } finally {
        if (!success) {
          reg.lock.unlock();
          releaseRegistration(reg);
        }
      }
    }

    @Override
    protected void postRegister(Registration reg) {
      assert reg.lock.isHeldByCurrentThread();
      reg.lock.unlock();
      releaseRegistration(reg);
    }

    @Override
    protected void doRegister(Registration reg, Object mbean, ObjectName objectName) throws Exception {
      assert reg.lock.isHeldByCurrentThread();
      shutdownLock.readLock().lock();
      try {
        checkNotShutdown();
        server.registerMBean(mbean, objectName);
        reg.objectName = objectName;
      } finally {
        shutdownLock.readLock().unlock();
      }
    }
  }

  private static final class Registration {
    private final Object key;
    private final ReentrantLock lock = new ReentrantLock();
    // access this only in synchronized on registrations map!
    private int claimedCount = 0;
    // access this only when locked on lock!
    @Nullable
    private ObjectName objectName;

    private Registration(Object key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return objectName + " -> " + key;
    }
  }
}
