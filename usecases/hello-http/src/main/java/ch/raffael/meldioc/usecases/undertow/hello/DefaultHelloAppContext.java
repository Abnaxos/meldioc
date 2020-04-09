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

package ch.raffael.meldioc.usecases.undertow.hello;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.Feature.Mount;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import ch.raffael.meldioc.library.base.jmx.registry.MBeanRegistryFeature;
import ch.raffael.meldioc.library.base.lifecycle.Lifecycle;
import ch.raffael.meldioc.library.base.lifecycle.StartupActions;
import ch.raffael.meldioc.library.codec.GsonObjectCodecFeature;
import ch.raffael.meldioc.library.http.server.undertow.StandardHttpServerParams;
import ch.raffael.meldioc.library.http.server.undertow.UndertowBlueprint;
import ch.raffael.meldioc.library.http.server.undertow.UndertowServerFeature;
import ch.raffael.meldioc.library.http.server.undertow.handler.HttpMethodHandler;
import ch.raffael.meldioc.library.http.server.undertow.handler.RequestLoggingHandler;
import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition;
import ch.raffael.meldioc.logging.Logging;
import ch.raffael.meldioc.usecases.undertow.hello.security.HelloIdentityManager;
import ch.raffael.meldioc.usecases.undertow.hello.security.HelloRole;
import com.typesafe.config.Config;
import org.slf4j.Logger;

@Configuration
abstract class DefaultHelloAppContext implements HelloAppContext {

  private static final Logger LOG = Logging.logger();

  @Mount
  abstract Lifecycle.Feature lifecycleFeature();

  @Mount
  abstract GsonObjectCodecFeature.Default gsonObjectCodecFeature();

  @Mount
  abstract UndertowServerFeature.WithSharedWorkersAndShutdown<HelloRequestContext> undertowServerFeature();

  @Mount
  abstract MBeanRegistryFeature.WithShutdown mbeanRegistryFeature();

  @Setup
  void startup(StartupActions startupActions) {
    startupActions.add(() -> undertowServerFeature().start());
    //startupActions.add(() -> {
    //  throw new Exception("Fail");
    //});
  }

  @Setup
  void jmx(MBeanRegistryFeature.Configuration registryConfig, StartupActions startupActions) {
    registryConfig.defaultDomain(getClass().getPackageName());
    startupActions.add(() -> mbeanRegistryFeature().mbeanRegistry()
        .register(new HelloMXBean.Impl()));
  }

  @Parameter
  String greeting() {
    return "Hello";
  }

  @Provision
  HelloRequests helloRequests() {
    return new HelloRequests(greeting());
  }

  private RoutingDefinition<HelloRequestContext> mergedRouting() {
    var paramHello = new RoutingDefinition<HelloRequestContext>() {{
      get().producePlainText().nonBlocking()
          .apply(query("name").asString(), n -> helloRequests().text(n));
    }};
    var pathHello = new RoutingDefinition<HelloRequestContext>() {{
      path().captureString().route(name ->
          get().producePlainText()
              .apply(name, n -> helloRequests().text(n)));
    }};
    var restHello = new RoutingDefinition<HelloRequestContext>() {{
        post().accept(RestHelloRequest.class).produce(RestHelloResponse.class)
            .apply(p -> helloRequests().json(p));
    }};
    return new RoutingDefinition<>() {{
      objectCodec(objectCodecFactory());
      path("hello").route(() -> {
        restrict(HelloRole.class, HelloRole.USER);
        merge(paramHello);
        merge(pathHello);
      });
      path("rest/hello").route(() -> {
        restrict(HelloRole.class, HelloRole.ADMIN);
        merge(restHello);
      });
      path("long").route(() -> {
        get().producePlainText().apply(() -> helloRequests().longText());
      });
      path("throw").route(() -> {
        handle(HttpMethodHandler.Method.values()).apply(() -> {
          throw new Exception("This method always fails; making the message log for zipping: "
              + helloRequests().longText());
        });
      });
    }};
  }

  @Setup
  void setupUndertow(UndertowBlueprint<HelloRequestContext> config) {
    config.requestContextFactory(
        __ -> DefaultHelloRequestContextShell.builder().mountParent(this).config(allConfig()).build())
        .handler(n -> RequestLoggingHandler.info(LOG, n))
        .basicSecurity(new HelloIdentityManager())
        .routing(this::mergedRouting)
        .http(httpServerAddress(), httpServerPort());
  }

  @Parameter(Parameter.ALL)
  abstract Config allConfig();

  @Parameter(StandardHttpServerParams.PORT)
  abstract int httpServerPort();

  @Parameter(StandardHttpServerParams.ADDRESS)
  String httpServerAddress() {
    return StandardHttpServerParams.ADR_ALL;
  }
}
