/*
 *  Copyright (c) 2021 Raffael Herzog
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

package ch.raffael.meldioc.library.base.jmx.registry;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Feature.DependsOn;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.library.base.jmx.registry.std.DefaultMBeanRegistry;
import ch.raffael.meldioc.library.base.jmx.registry.util.DomainMappings;
import ch.raffael.meldioc.library.base.lifecycle.ShutdownFeature;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;


@Feature
public interface MBeanRegistryFeature {

  @Provision(singleton = true)
  default MBeanServer registryMBeanServer() {
    return ManagementFactory.getPlatformMBeanServer();
  }

  @Provision
  MBeanRegistry mbeanRegistry();

  @Feature
  class Default implements MBeanRegistryFeature {

    private final Configuration configuration = new Configuration();

    @ExtensionPoint
    protected Configuration configuration() {
      return configuration;
    }

    @Provision(singleton = true)
    @Override
    public DefaultMBeanRegistry mbeanRegistry() {
      var handle = DefaultMBeanRegistry.create(registryMBeanServer(), configuration.toMappings());
      postCreate(handle);
      return handle.registry();
    }

    protected void postCreate(DefaultMBeanRegistry.Handle registryHandle) {
    }
  }

  @Feature
  abstract class WithShutdown extends Default implements @DependsOn ShutdownFeature {
    @Override
    protected void postCreate(DefaultMBeanRegistry.Handle registryHandle) {
      super.postCreate(registryHandle);
      shutdownController().onPerform(registryHandle::shutdown);
    }
  }

  @ExtensionPoint
  class Configuration {
    private Option<String> defaultDomain = none();
    private Map<String, String> mappings = LinkedHashMap.empty();

    public Configuration defaultDomain(String defaultDomain) {
      this.defaultDomain = some(defaultDomain);
      return this;
    }

    public Configuration mapping(String baseName, String domain) {
      mappings = mappings.put(baseName, domain);
      return this;
    }

    public DomainMappings toMappings() {
      return DomainMappings.builder()
          .defaultDomain(defaultDomain.getOrElseThrow(() -> new IllegalStateException("No default domain set")))
          .mappings(mappings)
          .build();
    }
  }
}
