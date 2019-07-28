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

package ch.raffael.compose.http.undertow;

import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Feature;
import ch.raffael.compose.Feature.DependsOn;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.shutdown.ShutdownFeature;
import ch.raffael.compose.logging.Logging;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.undertow.Undertow;
import org.slf4j.Logger;

/**
 * Default Undertow server.
 */
@Feature
public abstract class DefaultUndertowServerFeature<C> {

  private static final Logger LOG = Logging.logger();

  private final UndertowBuilderConfiguration.Holder<C> undertowBuilder =
      UndertowBuilderConfiguration.Holder.create();
  private final HttpRouting.Default<C> httpRouter = new HttpRouting.Default<>();

  @Parameter("undertow")
  protected Config undertowConfig() {
    return ConfigFactory.empty();
  }

  @Provision(shared = true)
  protected Undertow undertow() {
    return undertowBuilder.configureAndStart(httpRouter.definitions())  ;
  }

  @ExtensionPoint
  protected HttpRouting<C> httpRouter() {
    return httpRouter.api();
  }

  @ExtensionPoint
  protected UndertowBuilderConfiguration<C> undertowBuilderConfiguration() {
    var config = undertowBuilder.acceptor();
    preConfigure(config);
    return config;
  }

  protected void preConfigure(UndertowBuilderConfiguration<C> config) {
    config.postStart(u -> LOG.info("Undertow started: {}", u.getListenerInfo()));
  }

  @Feature
  public static abstract class WithShutdown<C> extends DefaultUndertowServerFeature<C> implements @DependsOn ShutdownFeature {
    private static final Logger LOG = Logging.logger();
    @Override
    protected void preConfigure(UndertowBuilderConfiguration<C> config) {
      super.preConfigure(config);
      config.postConstruct(u -> shutdownController().onPrepare(() -> {
        LOG.info("Stopping Undertow: {}", u.getListenerInfo());
        u.stop();
      }));
    }
  }

}
