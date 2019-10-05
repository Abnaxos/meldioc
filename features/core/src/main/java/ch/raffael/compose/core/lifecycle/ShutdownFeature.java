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

package ch.raffael.compose.core.lifecycle;

import ch.raffael.compose.Feature;
import ch.raffael.compose.Provision;
import ch.raffael.compose.core.threading.ThreadingFeature;

/**
 * Standard feature for controlled 3-phase shutdown.
 */
@Feature
public interface ShutdownFeature {

  @Provision(shared = true)
  ShutdownController shutdownController();

  @Feature
  abstract class Parallel implements ShutdownFeature, ThreadingFeature {
    @Override
    @Provision(shared = true)
    public ExecutorShutdownController shutdownController() {
      return new ExecutorShutdownController(this::workExecutor);
    }
  }

  @Feature
  abstract class SameThread implements ShutdownFeature {
    @Override
    @Provision(shared = true)
    public ExecutorShutdownController shutdownController() {
      return new ExecutorShutdownController(() -> Runnable::run);
    }
  }
}
