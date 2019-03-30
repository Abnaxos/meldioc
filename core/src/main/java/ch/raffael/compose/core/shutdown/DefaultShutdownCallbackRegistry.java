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

package ch.raffael.compose.core.shutdown;

import io.vavr.CheckedRunnable;
import io.vavr.collection.List;

import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO javadoc
 */
public class DefaultShutdownCallbackRegistry implements ShutdownController {

  private AtomicReference<List<CheckedRunnable>> onPrepareCallbacks = new AtomicReference<>(List.empty());
  private AtomicReference<List<CheckedRunnable>> onPerformCallbacks = new AtomicReference<>(List.empty());
  private AtomicReference<List<CheckedRunnable>> onFinalizeCallbacks = new AtomicReference<>(List.empty());

  @Override
  public void onPrepare(CheckedRunnable callback) {
    onPrepareCallbacks.updateAndGet(l -> l.push(callback));
  }

  @Override
  public void onPerform(CheckedRunnable callback) {
    onPerformCallbacks.updateAndGet(l -> l.push(callback));
  }

  @Override
  public void onFinalize(CheckedRunnable callback) {
    onFinalizeCallbacks.updateAndGet(l -> l.push(callback));
  }

  public List<CheckedRunnable> onPrepareCallbacks() {
    return onPrepareCallbacks.get();
  }

  public List<CheckedRunnable> onPerformCallbacks() {
    return onPerformCallbacks.get();
  }

  public List<CheckedRunnable> onFinalizeCallbacks() {
    return onFinalizeCallbacks.get();
  }
}
