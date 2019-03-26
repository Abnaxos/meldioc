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

import ch.raffael.compose.runtime.CompositionException;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Mount;
import ch.raffael.compose.core.internal.generated.$Provider;
import ch.raffael.compose.modules.http.Routing;
import ch.raffael.compose.modules.http.jetty.JettyHttpModule;
import ch.raffael.compose.core.shutdown.ShutdownCoordinator;
import ch.raffael.compose.core.shutdown.ShutdownModule;
import ch.raffael.compose.core.threading.JavaThreadPoolModule;
import io.vavr.concurrent.Future;
import org.eclipse.jetty.server.Server;

import java.util.concurrent.ExecutorService;

/**
 * @since 2019-03-23
 */
final class AppContextAssemblyManual extends HelloMain.AppContext {

  private final $ShutdownModule$WithThreadingWorker $shutdownModule = new $ShutdownModule$WithThreadingWorker();

  private final $JettyHttpModule $httpModule = new $JettyHttpModule();

  private final JavaThreadPoolModule.WithShutdown $threadingModule = new $JavaThreadPoolModule$WithShutdown();

  AppContextAssemblyManual() throws CompositionException {
    try {
      super.routing($httpModule.routingExtensionPoint());
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Exception e) {
      if (e instanceof CompositionException) {
        throw (CompositionException) e;
      } else {
        throw new CompositionException(e);
      }
    }
  }

  @Mount
  @Override
  ShutdownModule.WithThreadingWorker shutdownModule() {
    return $shutdownModule;
  }

  @Mount
  @Override
  JavaThreadPoolModule.WithShutdown threadingModule() {
    return $threadingModule;
  }

  @Mount
  @Override
  JettyHttpModule httpModule() {
    return $httpModule;
  }

  @Provision(shared = true)
  @Override
  public ExecutorService workExecutor() {
    return $threadingModule.workExecutor();
  }

  @Provision
  @Override
  public ShutdownCoordinator shutdownCoordinator() {
    return $shutdownModule.shutdownCoordinator();
  }

  private class $JettyHttpModule extends JettyHttpModule {

    private final $Provider<Future<Server>> $jettyServer = $Provider.shared(
        JettyHttpModule.class, "jettyServer", super::jettyServer);

    @Override
    public ShutdownCoordinator shutdownCoordinator() {
      return AppContextAssemblyManual.this.shutdownCoordinator();
    }

    @Override
    public ExecutorService workExecutor() {
      return AppContextAssemblyManual.this.workExecutor();
    }

    @Override
    protected Routing routingExtensionPoint() {
      return super.routingExtensionPoint();
    }

    @Provision(shared = true)
    @Override
    public Future<Server> jettyServer() {
      return $jettyServer.get();
    }
  }

  private class $JavaThreadPoolModule$WithShutdown extends JavaThreadPoolModule.WithShutdown {
    private final $Provider<ExecutorService> $workExecutor = $Provider.shared(
        WithShutdown.class, "workExecutor", super::workExecutor);

    @Override
    public ShutdownCoordinator shutdownCoordinator() {
      return AppContextAssemblyManual.this.shutdownCoordinator();
    }

    @Override
    public ExecutorService workExecutor() {
      return $workExecutor.get();
    }
  }

  private class $ShutdownModule$WithThreadingWorker extends WithThreadingWorker {
    private final $Provider<ShutdownCoordinator> $shutdownCoordinator = $Provider.shared(
        WithThreadingWorker.class, "shutdownCoordinator", super::shutdownCoordinator);

    @Override
    public ExecutorService workExecutor() {
      return AppContextAssemblyManual.this.workExecutor();
    }

    @Override
    public Future<Void> performShutdown() {
      return super.performShutdown();
    }

    @Provision(shared = true)
    @Override
    public ShutdownCoordinator shutdownCoordinator() {
      return $shutdownCoordinator.get();
    }
  }
}
