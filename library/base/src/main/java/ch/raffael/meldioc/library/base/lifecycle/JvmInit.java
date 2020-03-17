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

package ch.raffael.meldioc.library.base.lifecycle;

import ch.raffael.meldioc.library.base.ShutdownHooks;
import ch.raffael.meldioc.logging.Logging;

/**
 * Simple utility class for JVM applications. It installs {@link
 * Logging#init() initializes logging} and eager-loads {@link ShutdownHooks}
 * in its static initializer. The preferred usage is to have your
 * application's main class extend this class:
 *
 * <pre>
 *   public class MyMain extends JvmInit {
 *     public static void main(String[] args) {
 *       // main goes here
 *     }
 *   }
 * </pre>
 */
public class JvmInit {

  static {
    Logging.init();
    // eager loading installs the shutdown hook
    ShutdownHooks.shutdownHooks();
  }

  /**
   * This method does nothing as initialization is done in this class'
   * static initializer, but it may be useful to ensure class initialization
   * and document why. The preferred method is to use extends in your main
   * class.
   */
  public static void jvmInit() {
    // intentionally left blank
  }
}
