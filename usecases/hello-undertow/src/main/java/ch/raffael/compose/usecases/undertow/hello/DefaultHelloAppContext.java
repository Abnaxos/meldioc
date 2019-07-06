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
import ch.raffael.compose.Feature;
import ch.raffael.compose.Feature.Mount;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Setup;
import ch.raffael.compose.core.shutdown.ShutdownFeature;
import ch.raffael.compose.core.threading.JavaThreadPoolFeature;
import ch.raffael.compose.core.threading.ThreadingFeature;
import ch.raffael.compose.http.undertow.DefaultUndertowServerFeature;
import ch.raffael.compose.http.undertow.HttpRouter;
import ch.raffael.compose.http.undertow.UndertowBuilder;
import ch.raffael.compose.http.undertow.routing.RoutingDefinition;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO javadoc
 */
@Configuration
abstract class DefaultHelloAppContext implements HelloAppContext {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHelloAppContext.class);

  @Mount
  abstract ShutdownFeature.WithThreadingWorker shutdownFeature();

  @Mount
  abstract JavaThreadPoolFeature.WithShutdown threadingFeature();

  @Mount
  abstract ThreadingFeature.WithSystemForkJoinPool systemForkJoinFeature();

  @Mount
  abstract MyUndertowServerFeature undertowFeature();

  @Parameter
  String greeting() {
    return "Hello";
  }

  void start() throws Exception {
    //httpFeature().jettyServer();
  }

  void shutdown() {
    shutdownFeature().shutdownController().performShutdown().await();
  }

  @Setup
  void routing(HttpRouter<? extends HelloRequestContext> router) {
    router.route(new RoutingDefinition<HelloRequestContext>() {{
      path("/hello", () -> {
        get((ctx) -> "Hello whoever you are");
        path(template("/{name}").asString(), (valName) ->
            get(valName, (name, ctx) -> "Hello " + name));
      });
    }});
  }

  @Parameter(Parameter.ALL)
  abstract Config allConfig();

  @Feature
  static abstract class MyUndertowServerFeature extends DefaultUndertowServerFeature<HelloRequestContext> {
    @Override
    protected UndertowBuilder<HelloRequestContext> undertowBuilder() {
      // TODO FIXME (2019-07-06) not implemented
      return null;
    }
  }

}
