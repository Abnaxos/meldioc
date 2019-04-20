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
import ch.raffael.compose.Setup;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Module;
import ch.raffael.compose.Module.Mount;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.shutdown.ShutdownModule;
import ch.raffael.compose.core.threading.ThreadingModule;
import ch.raffael.compose.modules.http.Handler;
import ch.raffael.compose.modules.http.Servlets;
import ch.raffael.compose.modules.http.jetty.DefaultJettyHttpModule;
import ch.raffael.compose.modules.http.spi.HttpRequestContextModule;
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
abstract class DefaultHelloAppContext implements HelloAppContext, HttpRequestContextModule<HelloRequestContext> {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHelloAppContext.class);

  @Mount
  abstract ShutdownModule.WithThreadingWorker shutdownModule();

//  @Mount
//  abstract JavaThreadPoolModule.WithShutdown threadingModule();

  @Mount
  abstract ThreadingModule.WithSystemForkJoinPool systemForkJoinModule();

  @Mount
  abstract MyJettyModule httpModule();

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
    httpModule().jettyServer();
  }

  void shutdown() {
    shutdownModule().shutdownController().performShutdown().await();
  }

  @Parameter(path = Parameter.ALL)
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
  @Module
  static abstract class MyJettyModule extends DefaultJettyHttpModule.SharedJettyThreading<HelloRequestContext> {
    @Override
    @Provision(shared = true)
    protected Server jettyServer() throws Exception {
      return super.jettyServer();
    }
  }

}