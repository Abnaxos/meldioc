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

package ch.raffael.meldioc.logging.spi;

import java.util.Set;

/**
 * An initializer that does some initialisation, checks and logging specific
 * to the SLF4J backend in use.
 */
public interface Adapter {

  /**
   * The classname of the backend this class can handle.
   */
  String backendClassName();

  /**
   * Perform the init.
   * @param initLogger A logger to log warnings to.
   * @return Hints on how to proceed.
   */
  Set<InitFlag> initialize(InitLogger initLogger);

  /**
   * Shutdown the logging system.
   */
  void shutdown();

  abstract class Default implements Adapter {
    private final String backendClassName;

    protected Default(String backendClassName) {
      this.backendClassName = backendClassName;
    }

    @Override
    public String backendClassName() {
      return backendClassName;
    }
  }

  interface InitLogger {
    void warn(String message);
  }

  enum InitFlag {
    /**
     * Do not install the java.util.logging &rarr; SLF4J bridge.
     */
    SKIP_JUL_TO_SLF4J_BRIDGE
  }

}
