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

package ch.raffael.compose.util.fun;

import io.vavr.CheckedFunction0;
import io.vavr.Function0;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * @since 2019-03-20
 */
public final class Unit implements Serializable {

  private static final long serialVersionUID = 2019_03_20_00L;

  private static final Unit INSTANCE = new Unit();

  private Unit() {
  }

  public static Unit unit() {
    return INSTANCE;
  }

  public static Supplier<Unit> supplierOf(Runnable runnable) {
    return () -> {
      runnable.run();
      return unit();
    };
  }

  public static Function0<Unit> functionOf(Runnable runnable) {
    return () -> {
      runnable.run();
      return unit();
    };
  }

  public static CheckedFunction0<Unit> functionOf(Callable runnable) {
    return () -> {
      runnable.call();
      return unit();
    };
  }

  @Override
  public String toString() {
    return "<Unit>";
  }

  private Object readResolve() throws ObjectStreamException {
    return unit();
  }

}
