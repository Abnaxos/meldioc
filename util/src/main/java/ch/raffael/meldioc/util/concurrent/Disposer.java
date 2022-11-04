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

package ch.raffael.meldioc.util.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A simple utility that helps to thread-safely dispose lazy things.
 *
 * <p>General usage:
 *
 * <ul>
 *   <li>call {@code onDispose()} to add hook to be run when disposing things
 *   <li>call {@code dispose()} to run all added hooks and remove them
 * </ul>
 *
 * Both {@code onDispose()} and {@code dispose()} synchronize on a lock that may be
 * passed to the constructor. By default, this {@code Disposer} instance will be used
 * for locking.
 */
public class Disposer {

  private final Object lock;
  @Nullable
  private Runnable disposeRunnable = null;

  public Disposer() {
    this(null);
  }

  public Disposer(@Nullable Object lock) {
    this.lock = Objects.requireNonNullElse(lock, this);
  }

  public <T> T onDispose(T instance, Runnable onDispose) {
    onDispose(onDispose);
    return instance;
  }

  public <T> T onDispose(T instance, Consumer<? super T> onDispose) {
    onDispose(() -> onDispose.accept(instance));
    return instance;
  }

  public void onDispose(Runnable onDispose) {
    synchronized (lock) {
      if (disposeRunnable == null) {
        this.disposeRunnable = onDispose;
      } else {
        var prev = disposeRunnable;
        disposeRunnable = () -> {
          prev.run();
          onDispose.run();
        };
      }
    }
  }

  public void dispose() {
    synchronized (lock) {
      if (disposeRunnable == null) {
        return;
      }
      var r = disposeRunnable;
      disposeRunnable = null;
      r.run();
    }
  }

  public Object lock() {
    return lock;
  }
}
