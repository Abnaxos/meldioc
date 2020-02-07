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

package ch.raffael.compose.library.http.server.undertow.routing;

/**
 * TODO JavaDoc
 */
public final class Blocks {

  private Blocks() {
  }

  static <T1> Curry1<T1> curry(Capture<? extends T1> cap1) {
    return new Curry1<>(cap1);
  }

  static <T1, T2> Curry2<T1, T2> curry(Capture<? extends T1> cap1, Capture<? extends T2> cap2) {
    return Blocks.<T1>curry(cap1).append(cap2);
  }

  static <T1, T2, T3> Curry3<T1, T2, T3> curry(Capture<? extends T1> cap1, Capture<? extends T2> cap2, Capture<? extends T3> cap3) {
    return Blocks.<T1, T2>curry(cap1, cap2).append(cap3);
  }

  @FunctionalInterface
  public interface Block0 {
    void run();
  }

  @FunctionalInterface
  public interface Block1<V1> {
    void run(Capture<? extends V1> val);
  }

  @FunctionalInterface
  public interface Block2<V1, V2> {
    void run(Capture<? extends V1> val1, Capture<? extends V2> val2);
  }

  @FunctionalInterface
  public interface Block3<V1, V2, V3> {
    void run(Capture<? extends V1> val1, Capture<? extends V2> val2, Capture<? extends V3> val3);
  }

  static class Curry1<T1> {
    private final Capture<? extends T1> param;

    Curry1(Capture<? extends T1> param) {
      this.param = param;
    }

    void run(Block1<? super T1> block) {
      block.run(param);
    }

    Block0 runnable(Block1<? super T1> block) {
      return () -> run(block);
    }

    <T2> Curry2<T1, T2> append(Capture<? extends T2> append) {
      return new Curry2<>(this, append);
    }
  }

  static class Curry2<T1, T2> {
    private final Curry1<T1> prev;
    private final Capture<? extends T2> param;

    Curry2(Curry1<T1> prev, Capture<? extends T2> param) {
      this.prev = prev;
      this.param = param;
    }

    void run(Block2<? super T1, ? super T2> block) {
      block.run(prev.param, param);
    }

    Block0 runnable(Block2<? super T1, ? super T2> block) {
      return () -> run(block);
    }

    <T3> Curry3<T1, T2, T3> append(Capture<? extends T3> append) {
      return new Curry3<>(this, append);
    }
  }

  static class Curry3<T1, T2, T3> {
    private final Curry2<T1, T2> prev;
    private final Capture<? extends T3> param;

    Curry3(Curry2<T1, T2> prev, Capture<? extends T3> param) {
      this.prev = prev;
      this.param = param;
    }

    void run(Block3<? super T1, ? super T2, ? super T3> block) {
      block.run(prev.prev.param, prev.param, param);
    }

    Block0 runnable(Block3<? super T1, ? super T2, ? super T3> block) {
      return () -> run(block);
    }
  }
}
