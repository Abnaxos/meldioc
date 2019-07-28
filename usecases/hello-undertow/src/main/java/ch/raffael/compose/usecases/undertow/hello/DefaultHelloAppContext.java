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

package ch.raffael.compose.usecases.undertow.hello;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Feature.Mount;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Setup;
import ch.raffael.compose.core.shutdown.ShutdownFeature;
import ch.raffael.compose.core.threading.JavaThreadPoolFeature;
import ch.raffael.compose.core.threading.ThreadingFeature;
import ch.raffael.compose.http.undertow.HttpRouter;
import ch.raffael.compose.http.undertow.routing.RoutingDefinition;
import com.typesafe.config.Config;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * TODO javadoc
 */
@Configuration
abstract class DefaultHelloAppContext implements HelloAppContext {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHelloAppContext.class);

  private final HttpRouter.Default<HelloRequestContext> httpRouterEp = new HttpRouter.Default<>();

  @Mount
  abstract ShutdownFeature.WithThreadingWorker shutdownFeature();

  @Mount
  abstract JavaThreadPoolFeature.WithShutdown threadingFeature();

  @Mount
  abstract ThreadingFeature.WithSystemForkJoinPool systemForkJoinFeature();

  @Provision(shared = true)
  Supplier<HelloRequestContext> helloRequestContext() {
    return () -> DefaultHelloRequestContextShell.builder()
        .config(allConfig())
        .mountParent(this)
        .build();
  }

  @Parameter

  String greeting() {
    return "Hello";
  }

  void start() {
    undertowServer();
  }

  void shutdown() {
    shutdownFeature().shutdownController().performShutdown().await();
  }

  @Setup
  void routing(HttpRouter<? extends HelloRequestContext> router) {
    router.route(new RoutingDefinition<HelloRequestContext>() {{
      path("/hello").route(() -> {
        get().producePlainText()
            .with(query("name").asString().orElse("whoever you are"), (ctx, n) -> "Hello " + n);
        path().captureString().route(valName ->
            get().producePlainText()
                .with(valName, (ctx, name) -> "Hello " + name));
      });
    }});
  }

  @Parameter(Parameter.ALL)
  abstract Config allConfig();

  @Parameter("http-server.port")
  abstract int httpServerPort();

  @Parameter("http-server.address")
  String httpServerAddress() {
    return "0.0.0.0";
  }

  @Provision(shared = true)
  Undertow undertowServer() {
    var server = Undertow.builder()
        .addHttpListener(httpServerPort(), httpServerAddress())
        .setHandler(RoutingDefinition.createHandlerTree(httpRouterEp.definitions()))
        .build();
    server.start();
    shutdownController().onPrepare(server::stop);
    return server;
  }

  @ExtensionPoint
  HttpRouter<HelloRequestContext> routerEp() {
    return httpRouterEp.api();
  }

}
