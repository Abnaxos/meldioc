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

package ch.raffael.compose.core.threading;

import ch.raffael.compose.Parameter;
import ch.raffael.compose.Module;
import ch.raffael.compose.Module.DependsOn;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.shutdown.ShutdownModule;
import io.vavr.control.Option;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.vavr.API.*;

/**
 * A {@link ThreadingModule} that uses a Java {@link ThreadPoolExecutor}.
 */
@Module
@Parameter.Prefix("workers")
public abstract class JavaThreadPoolModule implements ThreadingModule {

  @Parameter
  protected int corePoolSize() {
    return 5;
  }

  @Parameter
  protected int maxPoolSize() {
    return 20;
  }

  @Parameter
  protected Duration keepAliveTime() {
    return Duration.ofSeconds(2);
  }

  @Parameter
  protected int queueCapacity() {
    return Integer.MAX_VALUE;
  }

  @Override
  @Provision(shared = true)
  public ExecutorService workExecutor() {
    return createRejectedExecutionHandler()
        .map((reh) -> new ThreadPoolExecutor(
            corePoolSize(),
            maxPoolSize(),
            keepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
            createQueue(),
            createThreadFactory(),
            reh))
        .getOrElse(() -> new ThreadPoolExecutor(
            corePoolSize(),
            maxPoolSize(),
            keepAliveTime().toMillis(), TimeUnit.MILLISECONDS,
            createQueue(),
            createThreadFactory()));
  }

  protected BlockingQueue<Runnable> createQueue() {
    return new LinkedBlockingDeque<>(queueCapacity());
  }

  protected ThreadFactory createThreadFactory() {
    return new CountingThreadFactory(threadGroup(), CountingThreadFactory.formatNameBuilder("worker-%d"));
  }

  protected Option<ThreadGroup> threadGroup() {
    return Some(new ThreadGroup("workers"));
  }

  protected Option<? extends RejectedExecutionHandler> createRejectedExecutionHandler() {
    return None();
  }

  /**
   * A {@link JavaThreadPoolModule} that destroys the thread pool on
   * finalizing the shutdown.
   */
  @Module
  public static abstract class WithShutdown extends JavaThreadPoolModule implements @DependsOn ShutdownModule {
    @Override
    @Provision(shared = true)
    public ExecutorService workExecutor() {
      return Util.applyShutdownModule(super.workExecutor(), this);
    }
  }

}
