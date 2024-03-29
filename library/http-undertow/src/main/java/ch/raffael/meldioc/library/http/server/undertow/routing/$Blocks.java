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

package ch.raffael.meldioc.library.http.server.undertow.routing;

///> "// vagen ${new Date()}"
/**
 * TODO JavaDoc
 */
/// filename Blocks.java
/// normalize spaces
///
/// = `$Blocks
///   --> `Blocks
/// = `@$.Public
///   --> `public
/// ~ `<,\s+
///   --> `<
@$.Public final class $Blocks {
  private $Blocks() {
  }
  ///<<< false
  private static class CaptureN {}
  private static class CurryPrev {}
  ///>>>

  @FunctionalInterface
  public interface Block0 {
    void run();
  }

  ///<<</ n: 1..count
  ///
  /// = `BlockN<Tall>
  ///   --> "Block$n${fwd 1..n collect {"T$it"} join ', ' emptyOr {"<$it>"}}"
  /// = `CaptureN v
  ///   --> fwd 1..n collect {"Capture<? extends T$it> v$it"} join ', '
  @FunctionalInterface
  public interface BlockN<Tall> {
    void run(CaptureN v);
  }
  ///>>>

  ///<<<
  /// = `CurryN
  ///   --> `Curry1
  /// = `$.x(), $.x()
  ///   --> `v1
  static <T> CurryN<T> curry(Capture<? extends T> v1) {
    return new CurryN<>($.x(), $.x());
  }
  ///>>>

  ///<<< n: 2..count
  /// = `<Tall>
  ///   --> "<${fwd 1..n collect {"T$it"} join ', '}>"
  /// = `CurryN
  ///   --> "Curry$n"
  /// = `curryN
  ///   --> `curry
  /// ~ `Capture.*vAll
  ///   --> fwd 1..n collect {"Capture<? extends T$it> v$it"} join ', '
  static <Tall> CurryN<Tall> curryN(Capture<? extends Tall> vAll) {
    ///<<< false
    Capture<Tall> vPrev = $.x();
    Capture<Tall> vLast= $.x();
    ///>>>
    ///<<<
    /// = `vPrev
    ///   --> fwd 1..(n-1) collect {"v$it"} join ', '
    /// = `vLast
    ///   --> "v$n"
    /// = `<Tall>
    ///   --> "<${fwd 1..(n-1) collect {"T$it"} join ', '}>"
    return $Blocks.<Tall>curry(vPrev).append(vLast);
    ///>>>
  }
  ///>>>

  ///<<</ n: 1..count
  ///
  /// = `CurryN
  ///   --> "Curry$n"
  /// = `<Tall>
  ///   --> fwd 1..n collect{"T$it"} join ', ' emptyOr {"<$it>"}
  static class CurryN<Tall> {
    /// = `CurryPrev
    ///   --> "Curry${n-1}<${fwd 1..<n collect {"? extends T$it"} join ', '}>"
    /// = `CaptureN
    ///   --> "Capture<? extends T$n>"
    /// = `Curry0<> prev,
    ///   -->
    ///
    ///<<< n>1
    private final CurryPrev prev;
    ///>>>
    private final CaptureN curry;

    CurryN(CurryPrev prev, CaptureN curry) {
      ///<<< n>1
      this.prev = prev;
      ///>>>
      this.curry = curry;
    }

    ///<<<
    /// = `BlockN<? super Tall>
    ///   --> "Block$n<${fwd 1..n collect {"? super T$it"} join ', '}>"
    /// = `$.x()
    ///   --> ((n-1)..0).collect {"${'prev.'*it}curry"} join ', '
    void run(BlockN<? super Tall> block) {
      block.run($.x());
    }

    Block0 runnable(BlockN<? super Tall> block) {
      return () -> run(block);
    }
    ///<<< n < count

    /// = `CurryN
    ///   --> "Curry${n+1}"
    /// = `<Tall>
    ///   --> "<${fwd 1..n collect {"T$it"} join ', '}, T>"
    /// = `$.x(), $.x()
    ///   --> `this, curry
    <T> CurryN<Tall> append(Capture<? extends T> curry) {
      return new CurryN<>($.x(), $.x());
    }
    ///>>>
    ///>>>
  }
  ///>>>
}
