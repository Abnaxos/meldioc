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

package ch.raffael.compose.logging;

import ch.raffael.compose.logging.spi.Adapter;
import ch.raffael.compose.logging.spi.Adapter.InitFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * An front-end to SLF4J's {@code LoggerFactory}. It adds:
 *
 * <ul>
 *   <li>checks to current logger backend configuration (specifically if JUL
 *       is redirected correctly)
 *   <li>handles inner and anonymous classes correctly (use canonical name)
 *   <li>provides a parameterless factory method that uses the {@code
 *       StackWalker} introduced in Java 9 to determine the logger name
 * </ul>
 */
public class Logging {

  private static final Map<String, Adapter> LOGGING_INITIALIZERS =
      ServiceLoader.load(Adapter.class).stream()
          .map(ServiceLoader.Provider::get)
          .collect(Collectors.toMap(Adapter::backendClassName, identity()));

  private static final StackWalker WALKER = StackWalker.getInstance(
      EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), 4);

  private static final ClassValue<String> LOGGER_NAME_CACHE = new ClassValue<>() {
    @Override
    protected String computeValue(Class<?> type) {
      if (type.isArray()) {
        throw new IllegalArgumentException("Cannot create logger for array type: " + type);
      }
      Class<?> loggerType = type;
      String name;
      while ((name = type.getCanonicalName()) == null) {
        loggerType = loggerType.getEnclosingClass();
        if (loggerType == null) {
          throw new IllegalStateException("Cannot determine canonical name of " + type);
        }
      }
      return name;
    }
  };

  private static final Optional<Adapter> ADAPTER = init0();

  private Logging() {
  }

  /**
   * Does nothing as logging will be initialized whenever this class is
   * loaded. It is strongly recommended to call this first in your main
   * method to ensure just that.
   */
  public static void init() {
  }

  public static void shutdown() {
    ADAPTER.ifPresent(Adapter::shutdown);
  }

  /**
   * Create a new logger for the caller class as of {@link #logger(Class)}.
   */
  public static Logger logger() {
    return logger(WALKER.getCallerClass());
  }

  /**
   * Creates a new logger for the given class. If the caller class is an
   * anonymous or local inner class, this method searches the innermost
   * enclosing non-anonymous class. If the class is an array, the component
   * type will be used.
   */
  public static Logger logger(Class<?> type) {
    var loggerType = type;
    while (loggerType.isArray()) {
      loggerType = loggerType.getComponentType();
    }
    return logger(LOGGER_NAME_CACHE.get(loggerType));
  }

  public static Logger logger(String name) {
    return LoggerFactory.getLogger(name);
  }

  private static Optional<Adapter> init0() {
    var initLogger = new Adapter.InitLogger() {
      @SuppressWarnings("UseOfSystemOutOrSystemErr")
      @Override
      public void warn(String message) {
        System.err.println("WARNING: " + Logging.class + ": " + message);
      }
    };
    Optional<Class<?>> factoryClass = Optional.ofNullable(LoggerFactory.getILoggerFactory().getClass());
    Optional<Adapter> adapter = Optional.empty();
    while (factoryClass.isPresent()) {
      adapter = factoryClass
          .map(Class::getName)
          .flatMap(c -> Optional.ofNullable(LOGGING_INITIALIZERS.get(c)));
      if (adapter.isPresent()) {
        break;
      }
      factoryClass = factoryClass.flatMap(c -> Optional.ofNullable(c.getSuperclass()));
    }
    adapter.map(i -> i.initialize(initLogger))
        .ifPresentOrElse(flags -> {
          if (!flags.contains(InitFlag.SKIP_JUL_TO_SLF4J_BRIDGE)) {
            initLogger.warn("Installing SLF4JBridgeHandler; this may impact performance, consider a better solution");
            boolean install;
            try {
              Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");
              install = true;
            } catch (ClassNotFoundException e) {
              install = false;
              initLogger.warn("SLF4JBridgeHandler not found on classpath: " + e);
            }
            if (install) {
              SLF4JBridgeHandler.removeHandlersForRootLogger();
              SLF4JBridgeHandler.install();
            }
          }
        }, () -> initLogger.warn("Unknown SLF4J backend: " + LoggerFactory.getILoggerFactory()));
    return adapter;
  }

}
