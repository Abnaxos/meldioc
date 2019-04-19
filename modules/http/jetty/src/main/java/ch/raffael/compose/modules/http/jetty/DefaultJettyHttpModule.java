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

package ch.raffael.compose.modules.http.jetty;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Module.DependsOn;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.shutdown.ShutdownModule;
import ch.raffael.compose.core.threading.ThreadingModule;
import ch.raffael.compose.modules.http.HttpModule;
import ch.raffael.compose.modules.http.Servlets;
import ch.raffael.compose.modules.http.spi.HttpRequestContextModule;
import ch.raffael.compose.util.Exceptions;
import io.vavr.Lazy;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the HTTP server module using Jetty.
 *
 * @param <C> The type of the request context.
 */
@Module
@Configuration.Prefix("httpServer")
public abstract class DefaultJettyHttpModule<C> implements HttpModule, @DependsOn HttpRequestContextModule<C>, @DependsOn ThreadingModule, @DependsOn ShutdownModule {

  public static final String REQUEST_CONTEXT_ATTR = DefaultJettyHttpModule.class.getName() + ".requestContext";

  @SuppressWarnings("unchecked")
  static <C> C getRequestContext(ServletRequest request) {
    return (C)request.getAttribute(REQUEST_CONTEXT_ATTR);
  }

  @ExtensionPoint.Provision
  protected Servlets.Default<C> servletsExtensionPoint() {
    var servlets = new Servlets.Default<C>();
    servlets.filter("/*").through((req, res, next) -> {
      try {
        C requestContext = httpRequestContextFactory().apply(req);
        req.setAttribute(REQUEST_CONTEXT_ATTR, requestContext);
        req = new HttpServletRequestWrapper(req) {
          @Override
          public void setAttribute(@Nullable String name, @Nullable Object o) {
            if (REQUEST_CONTEXT_ATTR.equals(name)) {
              throw new UnsupportedOperationException("Cannot set attribute " + REQUEST_CONTEXT_ATTR);
            }
            super.setAttribute(name, o);
          }
        };
        next.handle(req, res);
      } catch (Throwable throwable) {
        throw Exceptions.alwaysRethrow(throwable, ServletException.class, e -> new ServletException(e.toString(), e));
      }
    });
    return servlets;
  }

  @Configuration
  protected int port() {
    return 8080;
  }

  @Configuration
  protected String address() {
    return "0.0.0.0";
  }

  protected boolean sessionsEnabled() {
    return false;
  }

  protected boolean securityEnabled() {
    return false;
  }

  protected String contextPath() {
    return "/";
  }

  @Provision(shared = true)
  protected Server jettyServer() throws Exception {
    return startServer();
  }

  @Nonnull
  Server startServer() throws Exception {
    Server server = buildServer();
    server.start();
    return server;
  }

  @Nonnull
  Server buildServer() {
    var server = new Server(createThreadPool());
    var connector = new ServerConnector(server);
    connector.setHost(address());
    connector.setPort(port());
    server.addConnector(connector);
    shutdownController().onPrepare(server::stop);
    var context = new ServletContextHandler(server, contextPath(), sessionsEnabled(), securityEnabled());
    var ext = servletsExtensionPoint();
    ext.handlerMappings().forEach(m ->
        context.addServlet(new ServletHolder(m.name().getOrNull(), new ServletWrapper<>(m)), m.pathSpec()));
    ext.filterMappings().forEach(m ->
        context.addFilter(new FilterHolder(new FilterWrapper<>(m)), m.pathSpec(), EnumSet.copyOf(m.dispatch().toJavaSet())));
    // TODO (2019-03-23) static resources
    // TODO (2019-03-23) welcome files
    return server;
  }

  @Nonnull
  ThreadPool createThreadPool() {
    String name = "jetty-" + address() + "-" + port();
    ThreadGroup threadGroup = new ThreadGroup(name);
    var pool = new QueuedThreadPool(200, 8, 60000, null, threadGroup) {
      @Override
      protected Thread newThread(@Nonnull Runnable runnable) {
        return new Thread(threadGroup, runnable);
      }
    };
    pool.setName(name);
    return pool;
  }

  /**
   * A variant of the {@link DefaultJettyHttpModule} that allows to share
   * Jetty's thread pool for all the work. Provides {@link
   * ThreadingModule::workExecutor}.
   *
   * @param <C> The type of the request context.
   */
  @Module
  public static abstract class SharedJettyThreading<C> extends DefaultJettyHttpModule<C> implements ThreadingModule {
    private static final Logger LOG = LoggerFactory.getLogger(SharedJettyThreading.class);

    private final Lazy<WrappedJettyThreadPool> sharedThreadPool = Lazy.of(this::createSharedThreadPool);

    @Nonnull
    @Override
    WrappedJettyThreadPool createThreadPool() {
      return sharedThreadPool.get();
    }

    @Nonnull
    private WrappedJettyThreadPool createSharedThreadPool() {
      ThreadPool delegate = SharedJettyThreading.super.createThreadPool();
      try {
        ((LifeCycle)delegate).start();
        shutdownController().onFinalize(((LifeCycle) delegate)::stop);
      } catch (Exception e) {
        throw Exceptions.alwaysRethrow(e);
      }
      return new WrappedJettyThreadPool(delegate);
    }

    @Override
    @Provision(shared = true)
    public ExecutorService workExecutor() {
      LOG.info("Using Jetty shared thread pool");
      WrappedJettyThreadPool pool = sharedThreadPool.get();
      var lifeCycle = (LifeCycle) pool.delegate;
      return new AbstractExecutorService() {
        @Override
        public void shutdown() {
          throw new UnsupportedOperationException();
        }
        @Override
        public List<Runnable> shutdownNow() {
          throw new UnsupportedOperationException();
        }
        @Override
        public boolean isShutdown() {
          return !lifeCycle.isRunning();
        }
        @Override
        public boolean isTerminated() {
          return lifeCycle.isStopped() || lifeCycle.isFailed();
        }
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
          // TODO FIXME (2019-03-30) honor timeout
          pool.join();
          return true;
        }
        @Override
        public void execute(Runnable command) {
          pool.execute(command);
        }
      };
    }

    private static class WrappedJettyThreadPool implements ThreadPool {
      private final ThreadPool delegate;

      public WrappedJettyThreadPool(ThreadPool delegate) {
        this.delegate = delegate;
      }

      @Override
      public void join() throws InterruptedException {
        delegate.join();
      }

      @Override
      @ManagedAttribute("number of threads in pool")
      public int getThreads() {
        return delegate.getThreads();
      }

      @Override
      @ManagedAttribute("number of idle threads in pool")
      public int getIdleThreads() {
        return delegate.getIdleThreads();
      }

      @Override
      @ManagedAttribute("indicates the pool is low on available threads")
      public boolean isLowOnThreads() {
        return delegate.isLowOnThreads();
      }

      @Override
      public void execute(Runnable command) {
        delegate.execute(command);
      }

      ThreadPool delegate() {
        return delegate;
      }
    }
  }

}
