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

package ch.raffael.meldioc.library.base.threading;

import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.util.advice.AdvisingExecutorService;
import ch.raffael.meldioc.util.advice.AroundAdvice;
import ch.raffael.meldioc.util.advice.CompositeAroundAdvice;
import ch.raffael.meldioc.util.concurrent.RestrictedExecutorService;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * TODO JavaDoc
 */
public class DefaultWorkExecutorProvider {

  private final Supplier<? extends ExecutorService> workExecutorImplementation;
  private final MyTaskAdviceProfile taskAdviceProfile = new MyTaskAdviceProfile();
  private final Supplier<Runnable> defaultTaskFinalizer = () -> this::defaultFinalizeTask;

  public DefaultWorkExecutorProvider(Supplier<? extends ExecutorService> workExecutorImplementation) {
    this.workExecutorImplementation = workExecutorImplementation;
    taskAdviceProfile.after(defaultTaskFinalizer);
  }

  public DefaultWorkExecutorProvider withoutDefaultTaskFinalizer() {
    taskAdviceProfile.removeAfter(defaultTaskFinalizer);
    return this;
  }

  public ExecutorService workExecutor() {
    return RestrictedExecutorService.wrap(new AdvisingExecutorService<ExecutorService>(
        workExecutorImplementation.get(), taskAdviceProfile.advice()));
  }

  public AroundAdvice taskAdvice() {
    return taskAdviceProfile.advice();
  }

  public void defaultFinalizeTask() {
    MDC.clear();
  }

  public TaskAdviceFeature.Profile taskAdviceProfile() {
    return taskAdviceProfile;
  }

  @ExtensionPoint.Acceptor
  protected static class MyTaskAdviceProfile extends TaskAdviceFeature.Profile {
    @Override
    protected CompositeAroundAdvice advice() {
      return super.advice();
    }
  }
}
