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

import ch.raffael.compose.logging.Logging;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import static ch.raffael.compose.core.ShutdownHooks.shutdownHooks;

/**
 * TODO javadoc
 */
public class HelloApp {

  private static final Logger LOG = LoggerFactory.getLogger(HelloApp.class);

  public static void main(String[] args) {
    Logging.init();
    var config = ConfigFactory.load().resolve();
    DefaultHelloAppContext ctx = DefaultHelloAppContextShell.builder()
        .config(config)
        .build();
    shutdownHooks().add(ctx);
    ctx.shutdownController().onFinalize(() -> LOG.info("This is my shutdown hook"));
    ctx.start();
    LOG.info("Hello application ready, JVM uptime {}", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()));
  }
}
