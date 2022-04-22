/*
 *  Copyright (c) 2022 Raffael Herzog
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
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.util.advice.AroundAdvice;

import java.util.concurrent.ExecutorService;

@Feature
public abstract class AbstractWorkExecutorFeature implements WorkExecutorFeature {

  protected final DefaultWorkExecutorProvider workExecutorProvider =
      new DefaultWorkExecutorProvider(this::workExecutorImplementation);

  protected AbstractWorkExecutorFeature() {
  }

  @Provision(singleton = true)
  @Override
  public ExecutorService workExecutor() {
    return workExecutorProvider.workExecutor();
  }

  @Provision(singleton = true)
  abstract protected ExecutorService workExecutorImplementation();

  @Feature
  public static abstract class WithTaskAdvice extends AbstractWorkExecutorFeature implements TaskAdviceFeature {

    @Provision(singleton = true)
    public AroundAdvice taskAdvice() {
      return workExecutorProvider.taskAdvice();
    }

    @ExtensionPoint
    protected Profile taskAdviceProfile() {
      return workExecutorProvider.taskAdviceProfile();
    }
  }
}
