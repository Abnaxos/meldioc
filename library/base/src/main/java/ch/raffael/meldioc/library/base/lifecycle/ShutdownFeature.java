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

package ch.raffael.meldioc.library.base.lifecycle;

import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.library.base.threading.ThreadingFeature;

/**
 * Standard feature for controlled 3-phase shutdown.
 */
@Feature
public interface ShutdownFeature {

  @Provision
  ShutdownController shutdownController();

  @Feature
  interface WithActuator extends ShutdownFeature {
    @Provision
    default ShutdownController shutdownController() {
      return shutdownActuator().controller();
    }

    @Provision
    ShutdownController.Actuator shutdownActuator();
  }

  @Feature
  abstract class Parallel implements WithActuator, ThreadingFeature {
    @Provision(shared = true)
    @Override
    public ShutdownController.Actuator shutdownActuator() {
      return new ExecutorShutdownController(this::workExecutor).actuator();
    }
  }

  @Feature
  abstract class SameThread implements WithActuator {
    @Provision(shared = true)
    @Override
    public ShutdownController.Actuator shutdownActuator() {
      return new ExecutorShutdownController(() -> Runnable::run).actuator();
    }
  }
}
