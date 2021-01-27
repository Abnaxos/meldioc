/*
 *  Copyright (c) 2021 Raffael Herzog
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

package ch.raffael.meldioc.library.http.server.undertow.testlib;

import ch.raffael.meldioc.library.base.lifecycle.Lifecycle;
import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition;
import ch.raffael.meldioc.library.http.server.undertow.util.RequestContexts;
import com.typesafe.config.ConfigFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UndertowTestServer implements AutoCloseable {

  private final UndertowTestServerContext context;

  public UndertowTestServer(RoutingDefinition<RequestContexts.Empty> routing) {
    this(() -> routing);
  }

  public UndertowTestServer(Class<? extends RoutingDefinition<RequestContexts.Empty>> routing) {
    this(() -> {
      try {
        return routing.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException("Error instantiating routing: " + e, e);
      }
    });
  }

  public UndertowTestServer(RoutingFeature routing) {
    context = UndertowTestServerContextShell.builder()
        .config(ConfigFactory.empty())
        .mountRoutingFeature(routing)
        .build();
    Lifecycle.of(context.lifecylce()).start();
  }

  @Override
  public void close() throws Exception {
    context.lifecylce().shutdownActuator().performShutdown();
  }

  public int getPort() {
    return context.undertowServer().getListenerInfo().stream()
        .filter(i -> "http".equals(i.getProtcol()) && (i.getAddress() instanceof InetSocketAddress))
        .findFirst().map(i -> ((InetSocketAddress) i.getAddress()).getPort())
        .orElseThrow(() -> new IllegalStateException("No server address found"));
  }

  public URL url(String path) {
    return url(Map.of(), path);
  }

  public URL url(Map<String, Object> params, String path) {
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException("path must start with '/'");
    }
    var urlString = new StringBuilder("http://localhost:").append(getPort()).append(path);
    var firstParam = true;
    for (var p : params.entrySet()) {
      if (firstParam) {
        urlString.append('?');
        firstParam = false;
      } else {
        urlString.append('&');
      }
      urlString.append(urlEncode(p.getKey()));
      if (p.getValue() != null) {
        urlString.append('=').append(urlEncode(p.getValue()));
      }
    }
    try {
      return new URL(urlString.toString());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Illegal URL: urlString");
    }
  }

  public String urlEncode(Object value) {
    return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
  }
}
