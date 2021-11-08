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

package ch.raffael.meldioc.tools.vagen.tree;

import ch.raffael.meldioc.tools.vagen.Scope;
import io.vavr.collection.Stream;

/**
 * TODO JavaDoc
 */
public final class DefineNode extends ListNode {

  private final String name;
  private final boolean use;

  public DefineNode(boolean use, String name) {
    super("def" + (use ? "+use" : "") + ": " + name);
    this.use = use;
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public Stream<String> lines(Scope scope) {
    return use ? use().lines(scope) : Stream.empty();
  }

  public CompositeNode use() {
    return new CompositeNode("UseNode", "use: " + name) {
      @Override
      public Stream<? extends Node> children() {
        return DefineNode.this.children();
      }
    };
  }
}
