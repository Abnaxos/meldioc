/*
 *  Copyright (c) 2021 Raffael Herzog
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
[#compress]
  [#import "/parameters.ftl" as p]
  [#import "/codegen.ftl" as c]
[/#compress]

/**
 * TODO JavaDoc
 */
public final class Blocks {

  private Blocks() {
  }

  static <T1> Curry1<T1> curry(Capture<? extends T1> cap1) {
    return new Curry1<>(cap1);
  }

  [#list 2..p.pcount as i]
  static <${c.joinf(1..*i "T#")}> Curry${c.ntype(1..*i)} curry(${c.joinf(1..*i "Capture<? extends T#> cap#")}) {
    return Blocks.<${c.joinf(1..<i "T#")}>curry(${c.joinf( 1..<i "cap#")}).append(cap${i});
  }

  [/#list]

  [#list 0..p.pcount as i]
  @FunctionalInterface
  public interface Block${c.ntype(1..*i)} {
    void run(${c.joinf(1..*i "Capture<? extends T#> val#")});
  }

  [/#list]

  [#list 1..p.pcount as i]
  static class Curry${c.ntype(1..i)} {
    [#if !i?is_first]
    private final Curry${c.ntype(1..*(i-1) ">:T#")} prev;
    [/#if]
    private final Capture<? extends T${i}> param;

    Curry${i}([#if !i?is_first]Curry${c.ntype(1..*(i-1) ">:T#")} prev, [/#if]Capture<? extends T${i}> param) {
      [#if !i?is_first]
      this.prev = prev;
      [/#if]
      this.param = param;
    }

    void run(Block${c.ntype(1..*i "<:T#")} block) {
      block.run([#compress]
        [#list (i-1)..0 as j][#list 0..<j as _]prev.[/#list]param[#sep], [/#list]
      [/#compress]);
    }

    Block0 runnable(Block${c.ntype(1..*i "<:T#")} block) {
      return () -> run(block);
    }
    [#if !i?is_last]

    <T${i+1}> Curry${c.ntype(1..*(i+1))} append(Capture<? extends T${i+1}> append) {
      return new Curry${i+1}<>(this, append);
    }
    [/#if]
  }

  [/#list]
}
