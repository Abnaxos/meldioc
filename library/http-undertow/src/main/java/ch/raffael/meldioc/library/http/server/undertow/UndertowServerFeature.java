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

package ch.raffael.meldioc.library.http.server.undertow;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Feature.DependsOn;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.library.base.lifecycle.ShutdownFeature;
import ch.raffael.meldioc.library.base.threading.ThreadingFeature;
import ch.raffael.meldioc.library.http.server.undertow.util.XnioOptions;
import ch.raffael.meldioc.logging.Logging;
import ch.raffael.meldioc.util.concurrent.Disposer;
import ch.raffael.meldioc.util.concurrent.RestrictedExecutorService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import org.slf4j.Logger;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Default Undertow server.
 */
@Feature
@Parameter.Prefix(UndertowServerFeature.UNDERTOW_PARAM_PREFIX)
public abstract class UndertowServerFeature<C> {

  public static final String UNDERTOW_PARAM_PREFIX = "undertow";

  private static final Logger LOG = Logging.logger();

  private final UndertowBlueprint.EP<C> undertowBlueprint = UndertowBlueprint.holder(this::createUndertowBuilder);

  protected final Object startStopLock = new Object();
  private final Disposer workerDisposer = new Disposer(startStopLock);
  private final Disposer undertowDisposer = new Disposer(startStopLock);

  public void start() {
    undertowServer();
  }

  public void start(Executor starter) {
    starter.execute(this::start);
  }

  public void stopAll() {
    synchronized (startStopLock) {
      stopUndertow();
      stopWorker();
    }
  }

  public void stopWorker() {
    workerDisposer.dispose();
  }

  public void stopUndertow() {
    undertowDisposer.dispose();
  }

  @Parameter
  protected int coreWorkers() {
    return ioThreads() * 8;
  }

  @Parameter
  protected int maxWorkers() {
    return coreWorkers();
  }

  @Parameter
  protected Duration workerKeepAlive() {
    return Duration.ofMinutes(1);
  }

  @Parameter
  protected int taskQueueLimit() {
    return Integer.MAX_VALUE;
  }

  @Parameter
  protected int ioThreads() {
    return Math.max(Runtime.getRuntime().availableProcessors(), 2);
  }

  @Parameter("xnio.worker-options")
  protected Config workerOptions() {
    return ConfigFactory.empty();
  }

  @Parameter("xnio.socket-options")
  protected Config socketOptions() {
    return ConfigFactory.empty();
  }

  @Parameter
  protected Config serverOptions() {
    return ConfigFactory.empty();
  }

  @ExtensionPoint
  protected UndertowBlueprint<C> undertowBuilderConfiguration() {
    var config = undertowBlueprint.acceptor();
    config.postConstruct(u -> undertowDisposer.onDispose(() -> {
      LOG.info("Shutting down undertow: {}", u.getListenerInfo());
      u.stop();
    }));
    preConfigure(config);
    return config;
  }

  @Provision(shared = true)
  protected XnioWorker xnioWorker() {
    synchronized (startStopLock) {
      var xnio = Xnio.getInstance(xnioClassLoader());
      var workerOptions = new XnioOptions(xnioClassLoader(), Options.class)
          .set(Options.WORKER_TASK_CORE_THREADS, coreWorkers())
          .set(Options.WORKER_TASK_MAX_THREADS, maxWorkers())
          .set(Options.WORKER_TASK_KEEPALIVE, Math.toIntExact(workerKeepAlive().toMillis()))
          .set(Options.WORKER_TASK_LIMIT, taskQueueLimit())
          .set(Options.WORKER_IO_THREADS, ioThreads())
          .set(Options.CONNECTION_HIGH_WATER, 1000000)
          .set(Options.CONNECTION_LOW_WATER, 1000000)
          .set(Options.TCP_NODELAY, true)
          .set(Options.CORK, true)
          .load(workerOptions());
      try {
        return workerDisposer.onDispose(xnio.createWorker(workerOptions.options()), w -> {
          LOG.info("Shutting down XNIO worker {}", w.getName());
          w.shutdownNow();
        });
      } catch (IOException e) {
        // checked the code: actually, an IOException cannot be thrown here
        throw new IllegalStateException("I/O error initializing Xnio worker", e);
      }
    }
  }

  @Provision(shared = true)
  protected Undertow undertowServer() {
    synchronized (startStopLock) {
      return undertowBlueprint.apply();
    }
  }

  protected ClassLoader xnioClassLoader() {
    return getClass().getClassLoader();
  }

  protected Undertow.Builder createUndertowBuilder() {
    var builder = Undertow.builder();
    new XnioOptions(xnioClassLoader(), UndertowOptions.class)
        .load(serverOptions())
        .forEach(builder::setServerOption);
    new XnioOptions(xnioClassLoader(), Options.class)
        .load(socketOptions())
        .forEach(builder::setSocketOption);
    builder.setWorker(xnioWorker());
    return builder;
  }

  protected void preConfigure(UndertowBlueprint<C> config) {
    config.postStart(u -> LOG.info("Undertow started: {}", u.getListenerInfo()));
  }

  @Feature
  public static abstract class WithShutdown<C> extends UndertowServerFeature<C>
      implements @DependsOn ShutdownFeature
  {
    @Provision(shared = true)
    @Override
    protected XnioWorker xnioWorker() {
      shutdownController().onPrepare(this::stopAll);
      return super.xnioWorker();
    }
  }

  @Feature
  public static abstract class WithSharedWorkers<C> extends UndertowServerFeature<C> implements ThreadingFeature {
    @Provision(shared = true)
    @Override
    public ExecutorService workExecutor() {
      return RestrictedExecutorService.wrap(xnioWorker());
    }

    public void startAsync() {
      start(workExecutor());
    }
  }

  @Feature
  public static abstract class WithSharedWorkersAndShutdown<C> extends WithSharedWorkers<C>
      implements @DependsOn ShutdownFeature
  {
    @Provision(shared = true)
    @Override
    protected XnioWorker xnioWorker() {
      shutdownController().onFinalize(this::stopWorker);
      shutdownController().onPrepare(this::stopUndertow);
      return super.xnioWorker();
    }
  }
}
