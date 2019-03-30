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

import ch.raffael.compose.Assembly;
import ch.raffael.compose.Compose;
import ch.raffael.compose.Module.Mount;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.shutdown.ShutdownModule;
import ch.raffael.compose.core.threading.JavaThreadPoolModule;
import ch.raffael.compose.core.threading.ThreadingModule;
import ch.raffael.compose.modules.http.Servlets;
import ch.raffael.compose.modules.http.jetty.DefaultJettyHttpModule;
import io.vavr.concurrent.Future;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO javadoc
 */
@Assembly
abstract class HelloAppAssembly implements HelloAppContext {

  private static final Logger LOG = LoggerFactory.getLogger(HelloAppAssembly.class);

  @Mount
  abstract ShutdownModule.WithThreadingWorker shutdownModule();

//  @Mount
//  abstract JavaThreadPoolModule.WithShutdown threadingModule();

  @Mount
  abstract ThreadingModule.WithSystemForkJoinPool systemForkJoinModule();

  @Mount
  abstract MyJettyModule httpModule();

  @Compose
  void contributeServlets(Servlets servlets) {
    servlets.handle("/hello/*").with(HelloApp::sayHello);
    servlets.handle("/hello").with(HelloApp::sayHello);
    servlets.filter("/*").through(HelloApp::logRequest);
    // WARNING: this is dangerous. It works with the shutdown coordinator,
    // but other components may not be ready yet. This is called very early.
    // Shutdown coordinator is a component that supports this.
    shutdownController().onFinalize(() -> LOG.info("This is my shutdown hook"));
  }

  void start() {
    httpModule().jettyServer();
  }

  void shutdown() {
    shutdownModule().shutdownController().performShutdown().await();
  }

  /**
   * We need this to expose the jetty server to this package.
   */
  static abstract class MyJettyModule extends DefaultJettyHttpModule.SharedJettyThreading {
    @Override
    @Provision(shared = true)
    protected Future<Server> jettyServer() {
      return super.jettyServer();
    }
  }

}
