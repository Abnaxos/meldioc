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

package ch.raffael.meldioc.usecases.plugins.host;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Setup;
import ch.raffael.meldioc.library.base.ConfigFeature;
import ch.raffael.meldioc.library.base.lifecycle.Lifecycle;
import ch.raffael.meldioc.library.base.lifecycle.StartupActions;
import ch.raffael.meldioc.library.base.threading.SameThreadWorkExecutorFeature;
import ch.raffael.meldioc.usecases.plugins.spi.HostContext;
import ch.raffael.meldioc.usecases.plugins.spi.Messages;
import ch.raffael.meldioc.usecases.plugins.spi.PluginActivator;
import ch.raffael.meldioc.util.Classes;
import io.vavr.collection.List;
import org.slf4j.Logger;

import static ch.raffael.meldioc.logging.Logging.logger;

@Configuration
abstract class DefaultHostContext implements HostContext {

  private static final Logger LOG = logger();

  private final Messages messages = new Messages();

  @Feature.Mount
  abstract Lifecycle.Feature lifecycleFeature();

  @Feature.Mount
  abstract SameThreadWorkExecutorFeature threadingFeature();

  @Feature.Mount
  abstract ConfigFeature.Default configFeature();

  @ExtensionPoint
  Messages messages() {
    return messages;
  }

  @Setup
  void setup(StartupActions startup) {
    LOG.info("{} setup", Classes.outermost(getClass()).getSimpleName());
    startup.add(() -> LOG.info("{} starting up", Classes.outermost(getClass()).getSimpleName()));
    startup.add(() -> shutdownController().onPrepare(
        () -> LOG.info("{} shutting down", Classes.outermost(getClass()).getSimpleName())));
  }

  @Setup
  void loadPluginsGenerated() {
    var injectedSetups = List.of(new PluginLoader(getClass().getClassLoader()));
    var extensionPoints = new PluginActivator.ExtensionPoints() {
      @Override
      public Messages messages() {
        return DefaultHostContext.this.messages();
      }

      @Override
      public StartupActions startupActions() {
        return lifecycleFeature().startupActionsEP();
      }
    };
    for (var setup : injectedSetups) {
      setup.activate(this, configFeature().config(), extensionPoints);
    }
  }
}
