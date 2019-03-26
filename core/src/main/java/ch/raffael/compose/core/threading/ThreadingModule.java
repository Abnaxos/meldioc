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
import io.vavr.Function0;

import java.util.concurrent.ExecutorService;

/**
 * @since 2019-03-17
 */
@Module
public interface ThreadingModule {

  @Provision
  ExecutorService workExecutor();

  abstract class Basic implements ThreadingModule {
    private final Function0<? extends ExecutorService> executorServiceFun;
    protected Basic(Function0<? extends ExecutorService> executorServiceFun) {
      this.executorServiceFun = executorServiceFun;
    }
    public static <T extends ExecutorService> T applyShutdownModule(T executorService, ShutdownModule shutdownModule) {
      shutdownModule.shutdownCoordinator().onFinalize(executorService::shutdownNow);
      return executorService;
    }
  }

}
