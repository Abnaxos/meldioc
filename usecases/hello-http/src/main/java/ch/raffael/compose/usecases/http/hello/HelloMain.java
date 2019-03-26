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

import ch.raffael.compose.Compose;
import ch.raffael.compose.Context;
import ch.raffael.compose.Mount;
import ch.raffael.compose.modules.http.HttpModule;
import ch.raffael.compose.modules.http.Routing;
import ch.raffael.compose.modules.http.jetty.JettyHttpModule;
import ch.raffael.compose.core.shutdown.ShutdownModule;
import ch.raffael.compose.core.threading.JavaThreadPoolModule;
import ch.raffael.compose.core.threading.ThreadingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 2019-03-23
 */
public class HelloMain {

  private static final Logger LOG = LoggerFactory.getLogger(HelloMain.class);

  public static void main(String[] args) throws Exception {
    var ctx = new AppContextAssemblyManual();
    Runtime.getRuntime().addShutdownHook(new Thread(ctx::shutdown, "Shutdown"));
    ctx.start();
    ctx.httpModule().jettyServer().await();
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//    //ctx.shutdown();
//    System.exit(0);
  }

  @Context
  public static abstract class AppContext implements ThreadingModule, ShutdownModule, HttpModule {

    @Mount
    abstract ShutdownModule.WithThreadingWorker shutdownModule();

    @Mount
    abstract JavaThreadPoolModule.WithShutdown threadingModule();

    @Mount
    abstract JettyHttpModule httpModule();

    @Compose
    void routing(Routing routing) {
      routing.map("/hello/*").to(RawHelloHandler::new);
      routing.map("/hello").to(RawHelloHandler::new);
      shutdownCoordinator().onFinalize(() -> LOG.info("This is my shutdown hook"));
    }

    void start() {
      httpModule().jettyServer();
    }

    void shutdown() {
      shutdownModule().performShutdown().await();
    }

  }

}
