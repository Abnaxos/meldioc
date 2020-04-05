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

package ch.raffael.meldioc.logging.spi;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.LogManager;

/**
 * Logging initializer for Log4j 2. It checks that the JUL &rarr; Log4J bridge
 * is configured. If it isn't, it prints a warning and instructions on how
 * to configure this correctly. If it is, it makes sure the JUL &rarr; SLF4J
 * bridge isn't installed.
 */
public class Log4J2Adapter extends Adapter.Default {
  private static final String LOG4J_MANAGER = "org.apache.logging.log4j.jul.LogManager";

  public Log4J2Adapter() {
    super("org.apache.logging.slf4j.Log4jLoggerFactory");
  }

  @Override
  public Set<InitFlag> initialize(InitLogger initLogger) {
    if (LogManager.getLogManager().getClass().getName().equals(LOG4J_MANAGER)) {
      return EnumSet.of(InitFlag.SKIP_JUL_TO_SLF4J_BRIDGE);
    } else {
      initLogger.warn("java.util.logging is not redirected to Log4j.");
      initLogger.warn("Start the JVM with");
      initLogger.warn("  -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager");
      initLogger.warn("to fix this.");
      return Set.of();
    }
  }

  @Override
  public void shutdown() {
    org.apache.logging.log4j.LogManager.shutdown();
  }
}
