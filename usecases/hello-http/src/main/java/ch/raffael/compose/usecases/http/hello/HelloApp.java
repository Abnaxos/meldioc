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

import ch.raffael.compose.modules.http.Filter;
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

import static io.vavr.API.*;

/**
 * TODO javadoc
 */
public class HelloApp {

  private static final Logger LOG = LoggerFactory.getLogger(HelloApp.class);

  public static void main(String[] args) throws Exception {
    var config = ConfigFactory.load().resolve();
    HelloAppAssembly ctx = HelloAppAssemblyShell.builder()
        .config(config)
        .build();
    Runtime.getRuntime().addShutdownHook(new Thread(ctx::shutdown, "Shutdown"));
    ctx.shutdownController().onFinalize(() -> LOG.info("This is my shutdown hook"));
//    ctx.shutdownController().onPerform(() -> { throw new Exception("Ooops"); });
    ctx.start();
    LOG.info("Hello application ready, JVM uptime {}", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()));
//    Thread.sleep(1000);
//    ctx.shutdown();
  }

  static void sayHello(HttpServletRequest request, HttpServletResponse response, String greeting) throws IOException {
    byte[] bytes = (greeting + " " + Option(request.getPathInfo())
        .filter(p -> !p.isEmpty())
        .getOrElse("whoever you are") + "!").getBytes(StandardCharsets.UTF_8);
    response.setContentType("text/plain;charset=" + StandardCharsets.UTF_8.name());
    response.setContentLength(bytes.length);
    try (var out = response.getOutputStream()) {
      out.write(bytes);
    }
  }

  static void logRequest(HttpServletRequest request, HttpServletResponse response, Filter.Chain next) throws Exception {
    var start = System.nanoTime();
    LOG.info("Request to {} from {} by {}", request.getRequestURI(), request.getRemoteHost(), request.getHeader("User-Agent"));
    try {
      next.handle(request, response);
    } finally {
      LOG.info("Request handling time: {}", Duration.ofNanos(System.nanoTime() - start));
    }
  }

  static void logRequestId(HelloRequestContext ctx, HttpServletRequest request, HttpServletResponse response, Filter.Chain next) throws Exception {
    var start = System.nanoTime();
    LOG.info("Begin {}", ctx.requestId().get());
    try {
      next.handle(request, response);
    } finally {
      LOG.info("End {}", ctx.requestId().get());
    }
  }

}
