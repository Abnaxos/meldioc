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

import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.shutdown.ShutdownModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Basic infrastructure for multithreaded applications.
 */
@Module
public interface ThreadingModule {

  @Provision
  ExecutorService workExecutor();

  @Provision
  ForkJoinPool forkJoinPool();

  /**
   * Use the system's ForkJoinPool ({@link ForkJoinPool#commonPool()} for
   * calculations.
   */
  @Module
  abstract class WithSystemForkJoinPool implements ThreadingModule {
    @Provision
    @Override
    public ForkJoinPool forkJoinPool() {
      return ForkJoinPool.commonPool();
    }
  }

  final class Util {
    public static <T extends ExecutorService> T applyShutdownModule(T executorService, ShutdownModule shutdownModule) {
      shutdownModule.shutdownController().onFinalize(executorService::shutdownNow);
      return executorService;
    }
  }

}
