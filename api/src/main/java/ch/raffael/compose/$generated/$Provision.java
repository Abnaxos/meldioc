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

package ch.raffael.compose.$generated;

import ch.raffael.compose.runtime.ProvisionException;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * TODO javadoc
 */
public class $Provision<T> {

  private final String description;
  final Callable<? extends T> provider;

  $Provision(String description, Callable<? extends T> provider) {
    this.description = description;
    this.provider = provider;
  }

  static String descriptionFor(Class<?> sourceClass, String sourceMember) {
    return className(sourceClass, new StringBuilder())
        .append("::").append(sourceMember)
        .toString();
  }

  private static StringBuilder className(Class<?> cls, StringBuilder buf) {
    if (cls.getEnclosingClass() != null) {
      className(cls.getEnclosingClass(), buf);
      buf.append('.');
    }
    buf.append(cls.getSimpleName());
    return buf;
  }

  public static <T> $Provision<T> direct(Class<?> sourceClass, String sourceMember, Callable<? extends T> provider) {
    return new $Provision<>(descriptionFor(sourceClass, sourceMember), provider);
  }

  public static <T> Shared<T> shared(Class<?> sourceClass, String sourceMember, Callable<? extends T> provider) {
    return new Shared<>(descriptionFor(sourceClass, sourceMember), provider);
  }

  public T get() {
    try {
      T apply = provider.call();
      if (apply == null) {
        throw new ProvisionException(description + ": provider returned null");
      }
      return apply;
    } catch (VirtualMachineError | LinkageError | ThreadDeath | ProvisionException e) {
      throw e;
    } catch (Throwable e) {
      throw new ProvisionException(description + ": " + e, e);
    }
  }

  public String description() {
    return description;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "description='" + description + '\'' +
        '}';
  }

  static class Shared<T> extends $Provision<T> {

    @Nullable
    private volatile T instance = null;

    Shared(String description, Callable<? extends T> provider) {
      super(description, provider);
    }

    public T get() throws ProvisionException {
      T i;
      if ((i = instance) == null) {
        synchronized (this) {
          if ((i = instance) == null) {
            i = instance = super.get();
          }
        }
      }
      assert i != null;
      return i;
    }
  }
}
