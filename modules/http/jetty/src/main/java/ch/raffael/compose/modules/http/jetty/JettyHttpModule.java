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
import io.vavr.concurrent.Future;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.net.InetSocketAddress;
import java.util.EnumSet;

/**
 * TODO javadoc
 */
@Module
@Configuration.Prefix("http-server")
public abstract class JettyHttpModule implements HttpModule, @DependsOn ThreadingModule, @DependsOn ShutdownModule {

  @ExtensionPoint.Provision
  protected Servlets.Default servletsExtensionPoint() {
    return new Servlets.Default();
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
  protected Future<Server> jettyServer() {
    return Future.of(workExecutor(), () -> {
      var server = new Server(new InetSocketAddress(address(), port()));
      shutdownCoordinator().onPrepare(server::stop);
      var context = new ServletContextHandler(server, contextPath(), sessionsEnabled(), securityEnabled());
      Servlets.Default ext = servletsExtensionPoint();
      ext.handlerMappings().forEach(m ->
          context.addServlet(new ServletHolder(m.name().orElse(null), new ServletWrapper(m)), m.pathSpec()));
      ext.filterMappings().forEach(m ->
          context.addFilter(new FilterHolder(new FilterWrapper(m)), m.pathSpec(), EnumSet.copyOf(m.dispatch().toJavaSet())));
      // TODO (2019-03-23) static resources
      // TODO (2019-03-23) welcome files
      server.start();
      return server;
    });
  }

}
