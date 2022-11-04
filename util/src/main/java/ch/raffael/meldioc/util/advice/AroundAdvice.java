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

package ch.raffael.meldioc.util.advice;

import ch.raffael.meldioc.util.Exceptions;
import org.jetbrains.annotations.Nullable;

/**
 * TODO JavaDoc
 */
public interface AroundAdvice {

  AdviceCloseable before();

  static AroundAdvice nop() {
    return Nop.INSTANCE;
  }

  interface AdviceCloseable extends AutoCloseable {
    @Override
    void close() throws AdviceException;
  }

  abstract class Basic implements AroundAdvice {

    private final AdviceCloseable closeable;

    protected Basic() {
      closeable = () -> after(null);
    }

    @Override
    public AdviceCloseable before() {
      AdviceException throwing = null;
      try {
        beforeImpl();
      } catch (AdviceException e) {
        throwing = e;
      } catch (Throwable e) {
        Exceptions.rethrowIfFatal(e);
        throwing = AdviceException.onBefore(e);
      }
      if (throwing != null) {
        after(throwing);
        throw throwing;
      }
      return closeable;
    }

    private void after(@Nullable Throwable throwing) {
      try {
        afterImpl();
      } catch (AdviceException e) {
        if (throwing != null) {
          throwing.addSuppressed(e);
        } else {
          throw e;
        }
      } catch (Throwable e) {
        Exceptions.rethrowIfFatal(e, throwing);
        if (throwing != null) {
          throwing.addSuppressed(AdviceException.onAfter(e));
        } else {
          throw AdviceException.onAfter(e);
        }
      }
    }

    protected abstract void beforeImpl();

    protected abstract void afterImpl();
  }

  final class Nop implements AroundAdvice {

    private static final String NOP_TO_STRING = Nop.class.getCanonicalName();
    private static final AroundAdvice.AdviceCloseable CLOSEABLE = new AroundAdvice.AdviceCloseable() {
      @Override
      public void close() throws AdviceException {
      }
      @Override
      public String toString() {
        return NOP_TO_STRING;
      }
    };
    private static final Nop INSTANCE = new Nop();

    private Nop() {
    }

    @Override
    public AdviceCloseable before() {
      return CLOSEABLE;
    }
  }
}
