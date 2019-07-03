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

package ch.raffael.compose.usecases.http.hello;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.Feature;
import ch.raffael.compose.Feature.Mount;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Setup;
import ch.raffael.compose.core.shutdown.ShutdownFeature;
import ch.raffael.compose.core.threading.ThreadingFeature;
import ch.raffael.compose.features.http.Handler;
import ch.raffael.compose.features.http.Servlets;
import ch.raffael.compose.features.http.jetty.DefaultJettyHttpFeature;
import ch.raffael.compose.features.http.spi.HttpRequestContextFeature;
import com.typesafe.config.Config;
import io.vavr.CheckedFunction1;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * TODO javadoc
 */
@Configuration
abstract class DefaultHelloAppContext implements HelloAppContext, HttpRequestContextFeature<HelloRequestContext> {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHelloAppContext.class);

  @Mount
  abstract ShutdownFeature.WithThreadingWorker shutdownFeature();

//  @Mount
//  abstract JavaThreadPoolFeature.WithShutdown threadingFeature();

  @Mount
  abstract ThreadingFeature.WithSystemForkJoinPool systemForkJoinFeature();

  @Mount
  abstract MyJettyFeature httpServerFeature();

  @Setup
  void contributeServlets(Servlets<? extends HelloRequestContext> servlets) {
    Handler.IgnoringCtx helloHandler = (req, res) -> HelloApp.sayHello(req, res, greeting());
    servlets.handle("/hello/*").with(helloHandler);
    servlets.handle("/hello").with(helloHandler);
    servlets.filter("/*").through(HelloApp::logRequest);
    servlets.filter("/*").through(HelloApp::logRequestId);
    // WARNING: this is dangerous. It works with the shutdown coordinator,
    // but other components may not be ready yet. This is called very early.
    // Shutdown coordinator is a component that supports this.
    shutdownController().onFinalize(() -> LOG.info("This is my shutdown hook"));
  }

  @Parameter
  String greeting() {
    return "Hello";
  }

  void start() throws Exception {
    httpServerFeature().jettyServer();
  }

  void shutdown() {
    shutdownFeature().shutdownController().performShutdown().await();
  }

  @Parameter(Parameter.ALL)
  abstract Config allConfig();

  @Override
  @Provision
  public CheckedFunction1<HttpServletRequest, HelloRequestContext> httpRequestContextFactory() {
    return (r) -> DefaultHelloRequestContextShell.builder()
        .config(allConfig())
        .mountParent(this)
        .build();
  }

  /**
   * We need this to expose the jetty server to this package.
   */
  @Feature
  static abstract class MyJettyFeature extends DefaultJettyHttpFeature.SharedJettyThreading<HelloRequestContext> {
    @Override
    @Provision(shared = true)
    protected Server jettyServer() throws Exception {
      return super.jettyServer();
    }
  }

}
