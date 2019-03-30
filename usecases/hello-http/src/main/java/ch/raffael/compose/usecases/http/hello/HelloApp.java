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

import ch.raffael.compose.modules.http.Handler;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * TODO javadoc
 */
public class HelloApp {

  private static final Logger LOG = LoggerFactory.getLogger(HelloApp.class);

  public static void main(String[] args) throws Exception {
    var config = ConfigFactory.load().resolve();
    HelloAppAssembly ctx = AmendedHelloAssemblyShell.builder()
        .config(config)
        .buildAssembly();
    Runtime.getRuntime().addShutdownHook(new Thread(ctx::shutdown, "Shutdown"));
    ctx.shutdownCoordinator().onFinalize(() -> LOG.info("This is my shutdown hook"));
    ctx.start();
    ctx.httpModule().jettyServer().await();
    LOG.info("Hello application ready, JVM uptime {}", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()));
  }

  static void sayHello(HttpServletRequest request, HttpServletResponse response) throws IOException {
    byte[] bytes = ("Hello " + Optional.ofNullable(request.getPathInfo())
        .filter(p -> !p.isEmpty())
        .orElse("whoever you are") + "!").getBytes(StandardCharsets.UTF_8);
    response.setContentType("text/plain;charset=" + StandardCharsets.UTF_8.name());
    response.setContentLength(bytes.length);
    try (var out = response.getOutputStream()) {
      out.write(bytes);
    }
  }

  static void logRequest(HttpServletRequest request, HttpServletResponse response, Handler next) throws Exception {
    var start = System.nanoTime();
    LOG.info("Request to {} from {} by {}", request.getRequestURI(), request.getRemoteHost(), request.getHeader("User-Agent"));
    try {
      next.handle(request, response);
    } finally {
      LOG.info("Request handling time: {}", Duration.ofNanos(System.nanoTime() - start));
    }
  }

}

//final class AppContextAssemblyManual extends HelloAppAssembly {
//
//  private final Config $config;
//
//  private final $Mount_ShutdownModule$WithThreadingWorker $mount_shutdownModule = new $Mount_ShutdownModule$WithThreadingWorker();
//  private final $Mount_JettyHttpModule $mount_httpModule = new $Mount_JettyHttpModule();
//  private final $Mount_JavaThreadPoolModule$WithShutdown $mount_threadingModule = new $Mount_JavaThreadPoolModule$WithShutdown();
//
//  AppContextAssemblyManual(Config config) throws CompositionException {
//    $config = config;
//    try {
////      super.contributeServlets($mount_httpModule.servletsExtensionPoint());
//    } catch (RuntimeException | Error e) {
//      throw e;
//    } catch (Exception e) {
//      if (e instanceof CompositionException) {
//        throw (CompositionException) e;
//      } else {
//        throw new CompositionException(e);
//      }
//    }
//  }
//
//  @Module.Mount
//  @Override
//  ShutdownModule.WithThreadingWorker shutdownModule() {
//    return $mount_shutdownModule;
//  }
//
//  @Module.Mount
//  @Override
//  JavaThreadPoolModule.WithShutdown threadingModule() {
//    return $mount_threadingModule;
//  }
//
//  @Module.Mount
//  @Override
//  JettyHttpModule httpModule() {
//    return $mount_httpModule;
//  }
//
//  @Provision(shared = true)
//  @Override
//  public ExecutorService workExecutor() {
//    return $mount_threadingModule.workExecutor();
//  }
//
//  @Provision
//  @Override
//  public ShutdownCoordinator shutdownCoordinator() {
//    return $mount_shutdownModule.shutdownCoordinator();
//  }
//
//  private class $Mount_JettyHttpModule extends JettyHttpModule {
//
//    private final $Provision<Future<Server>> $provision_jettyServer = $Provision.shared(
//        JettyHttpModule.class, "jettyServer", super::jettyServer);
//
//    @Override
//    public ShutdownCoordinator shutdownCoordinator() {
//      return AppContextAssemblyManual.this.shutdownCoordinator();
//    }
//
//    @Override
//    public ExecutorService workExecutor() {
//      return AppContextAssemblyManual.this.workExecutor();
//    }
//
////    @Override
////    protected Servlets servletsExtensionPoint() {
////      return super.servletsExtensionPoint();
////    }
//
//    @Provision(shared = true)
//    @Override
//    public Future<Server> jettyServer() {
//      return $provision_jettyServer.get();
//    }
//  }
//
//  private class $Mount_JavaThreadPoolModule$WithShutdown extends JavaThreadPoolModule.WithShutdown {
//    private final $Provision<ExecutorService> $provision_workExecutor = $Provision.shared(
//        WithShutdown.class, "workExecutor", super::workExecutor);
//
//    @Override
//    public ShutdownCoordinator shutdownCoordinator() {
//      return AppContextAssemblyManual.this.shutdownCoordinator();
//    }
//
//    @Override
//    protected int corePoolSize() {
//      if ($config.hasPath("workers.corePoolSize")) {
//        return $config.getInt("workers.corePoolSize");
//      } else {
//        return super.corePoolSize();
//      }
//    }
//
//    @Override
//    public ExecutorService workExecutor() {
//      return $provision_workExecutor.get();
//    }
//  }
//
//  private class $Mount_ShutdownModule$WithThreadingWorker extends WithThreadingWorker {
//    private final $Provision<ShutdownCoordinator> $provision_shutdownCoordinator = $Provision.shared(
//        WithThreadingWorker.class, "shutdownCoordinator", super::shutdownCoordinator);
//
//    @Override
//    public ExecutorService workExecutor() {
//      return AppContextAssemblyManual.this.workExecutor();
//    }
//
//    @Override
//    public Future<Void> performShutdown() {
//      return super.performShutdown();
//    }
//
//    @Provision(shared = true)
//    @Override
//    public ShutdownCoordinator shutdownCoordinator() {
//      return $provision_shutdownCoordinator.get();
//    }
//  }
//}
