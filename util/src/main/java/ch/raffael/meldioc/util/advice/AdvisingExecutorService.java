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

package ch.raffael.meldioc.util.advice;

import ch.raffael.meldioc.util.concurrent.DecoratingExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * TODO JavaDoc
 */
public class AdvisingExecutorService<D extends ExecutorService> extends DecoratingExecutorService<D> {

  private final AroundAdvice advice;

  public AdvisingExecutorService(D delegate, AroundAdvice advice) {
    super(delegate);
    this.advice = advice;
  }

  @Override
  @SuppressWarnings("try")
  protected Runnable decorateRunnable(Runnable original) {
    return () -> {
      try (var __ = advice.before()) {
        original.run();
      }
    };
  }

  @Override
  @SuppressWarnings("try")
  protected <T> Callable<T> decorateCallable(Callable<? extends T> original) {
    return () -> {
      try (var __ = advice.before()) {
        return original.call();
      }
    };
  }
}
