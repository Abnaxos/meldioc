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
 */
public interface ShutdownController {
  void onPrepare(CheckedRunnable callback);

  void onPerform(CheckedRunnable callback);

  void onFinalize(CheckedRunnable callback);

}
