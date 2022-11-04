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

package ch.raffael.meldioc.util;

import org.jetbrains.annotations.Nullable;

/**
 * Some standard exceptions for illegal program control flow. Sometimes,
 * you'll get a compiler error if you don't handle certain cases that cannot
 * happen (switch statements are very prone to this). This lets you put some
 * statements there that make the compiler happy.
 */
public final class IllegalFlow extends AssertionError {

  private IllegalFlow(@Nullable Object detailMessage) {
    super(detailMessage);
  }

  public static IllegalFlow unreachableCode() {
    throw unreachableCode(null);
  }

  public static IllegalFlow unreachableCode(@Nullable Object detailMessage) {
    throw new IllegalFlow(message("Unreachable code", detailMessage));
  }

  public static IllegalFlow unexpectedException(Throwable exception) {
    throw unexpectedException(exception, null);
  }

  public static IllegalFlow unexpectedException(Throwable exception, @Nullable Object detailMessage) {
    throw (IllegalFlow) new IllegalFlow(message("Unexpected exception", detailMessage)).initCause(exception);
  }

  public static IllegalFlow notImplemented() {
    throw notImplemented(null);
  }

  public static IllegalFlow notImplemented(@Nullable Object detailMessage) {
    throw new IllegalFlow(message("Unreachable code", detailMessage));
  }

  private static String message(String baseMessage, @Nullable Object detailMessage) {
    return baseMessage + (detailMessage == null ? "" : " (" + detailMessage + ")");
  }

}
