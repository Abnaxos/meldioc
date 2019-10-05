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

import io.vavr.CheckedRunnable;

import java.util.function.Supplier;

/**
 * The shutdown controller allows registering hooks run on the three shutdown phases:
 *
 * <ul>
 *   <li><strong>prepare:</strong> stop accepting new work (e.g. stop
 *     accepting HTTP requests).
 *   <li><strong>perform:</strong> shut things down, save state if applicable etc.
 *   <li><strong>finalize:</strong> shutdown the core components like the
 *     work thread pool or a database connections.
 *  </ul>
 *
 *  The shutdown controller also supports running shutdown-preventing actions.
 *  Any shutdown request will have to wait until all shutdown preventing actions
 *  are done. As soon as a shutdown is on the horizon (initiated or announced),
 *  no more shutdown preventing actions will be possible.
 */
public interface ShutdownController {
  void onPrepare(CheckedRunnable callback);

  void onPerform(CheckedRunnable callback);

  void onFinalize(CheckedRunnable callback);

  /**
   * Run an action that prevents a shutdown being performed while running.
   */
  default void runPreventingShutdown(Runnable runnable) {
    getPreventingShutdown(() -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Run an action that prevents a shutdown being performed while running.
   */
  <T> T getPreventingShutdown(Supplier<T> supplier);

  State state();

  enum State {
    /**
     * No shutdown initiated up to now.
     */
    DORMANT,
    /**
     * A shutdown has been announced, shutdown preventing actions are not
     * possible anymore.
     */
    ANNOUNCED,
    /**
     * A shutdown has been initiated, possibly waiting for shutdown
     * preventing actions to finish. Shutdown preventing actions are not
     * possible anymore.
     */
    INITIATED,
    /**
     * {@code onPrepare} hook a are being run.
     */
    PREPARING,
    /**
     * {@code onPerform} hook a are being run.
     */
    PERFORMING,
    /**
     * {@code onFinalize} hook a are being run.
     */
    FINALIZING,
    /**
     * The shutdown is complete, with or without errors.
     */
    COMPLETE;


    public boolean isBefore(State before) {
      return ordinal() < before.ordinal();
    }

    public boolean isBeforeOrEqual(State before) {
      return ordinal() <= before.ordinal();
    }

    public void checkStateBefore(State before) {
      if (!isBefore(before)) {
        throw new IllegalShutdownStateException(this);
      }
    }

    public void checkStateBeforeOrEqual(State before) {
      if (!isBeforeOrEqual(before)) {
        throw new IllegalShutdownStateException(this);
      }
    }
  }

  class IllegalShutdownStateException extends IllegalStateException {
    private final State state;

    public IllegalShutdownStateException(State state) {
      super("Illegal shutdown controller state: " + state);
      this.state = state;
    }

    public State state() {
      return state;
    }
  }
}
