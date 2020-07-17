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

import ch.raffael.meldioc.util.Exceptions;

/**
 * TODO JavaDoc
 */
public class AdviceException extends RuntimeException {
  public AdviceException() {
    super();
  }

  public AdviceException(String message) {
    super(message);
  }

  public AdviceException(String message, Throwable cause) {
    super(message, cause);
  }

  public AdviceException(Throwable cause) {
    super(cause);
  }

  public static AdviceException onBefore(Throwable cause) {
    return create("Exception on before advice", cause);
  }

  public static AdviceException onAfter(Throwable cause) {
    return create("Exception on after advice", cause);
  }

  private static AdviceException create(String msg, Throwable cause) {
    Exceptions.rethrowIfFatal(cause);
    return new AdviceException(msg + ": " + cause, cause);
  }
}
