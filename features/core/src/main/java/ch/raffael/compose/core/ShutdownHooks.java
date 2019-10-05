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

package ch.raffael.compose.core;

import ch.raffael.compose.core.lifecycle.ExecutorShutdownController;
import ch.raffael.compose.core.lifecycle.ShutdownFeature;
import ch.raffael.compose.logging.Logging;
import io.vavr.collection.Seq;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static ch.raffael.compose.logging.Logging.logger;
import static io.vavr.API.*;

public final class ShutdownHooks {

  private static final Logger LOG = logger();

  private static final Object LOCK = new Object();
  @Nullable
  private static volatile ShutdownHooks INSTANCE = null;

  private final Thread shutdownHook = new Thread(ShutdownHooks.class.getName()) {
    @Override
    public void run() {
      hooks.get().forEach(Runnable::run);
      if (shutdownLogging) {
        Logging.shutdown();
      }
    }
  };

  private final AtomicReference<Seq<Runnable>> hooks = new AtomicReference<>(Seq());
  private volatile boolean shutdownLogging = true;

  private ShutdownHooks() {
  }

  public static ShutdownHooks shutdownHooks() {
    var i = INSTANCE;
    if (i == null) {
      synchronized (LOCK) {
        i = INSTANCE;
        if (i == null) {
          i = new ShutdownHooks();
          Runtime.getRuntime().addShutdownHook(i.shutdownHook);
          INSTANCE = i;
        }
      }
    }
    return i;
  }

  public ShutdownHooks add(Runnable... runnable) {
    hooks.updateAndGet(r -> r.appendAll(Array(runnable)));
    return this;
  }

  public ShutdownHooks add(ShutdownFeature shutdown) {
    return add(() -> {
      var sctl = shutdown.shutdownController();
      if (sctl instanceof ExecutorShutdownController) {
        try {
          ((ExecutorShutdownController) sctl).initiateShutdown().get();
        } catch (InterruptedException | ExecutionException e) {
          LOG.error("Error shutting down (current state: {})", sctl.state(), e);
        }
      } else {
        LOG.error("Cannot perform shutdown with {}", sctl);
      }
    });
  }

  /**
   * @deprecated Not required anymore, logging will be shut down by default
   * @see #shutdownLogging(boolean)
   */
  @Deprecated
  public ShutdownHooks logging() {
    shutdownLogging = true;
    return this;
  }

  /**
   * Enable or disable log shutdown in shutdown hook, enabled by default.
   *
   * @param shutdownLogging {@code true} to shutdown logging
   */
  public ShutdownHooks shutdownLogging(boolean shutdownLogging) {
    this.shutdownLogging = shutdownLogging;
    return this;
  }
}
