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

package ch.raffael.meldioc.tools.vagen.tree;

import ch.raffael.meldioc.tools.vagen.Scope;
import io.vavr.collection.Stream;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class Node {

  private final String type;
  private final String description;

  protected Node(String description) {
    this(null, description);
  }

  protected Node(@Nullable String type, String description) {
    this.type = type == null ? getClass().getSimpleName() : type;
    this.description = description;
  }

  public abstract Stream<String> lines(Scope scope);

  Stream<String> error(Scope scope, Object msg) {
    return new ErrorNode(msg).lines(scope);
  }

  @Override
  public String toString() {
    return type + "[" + description + "]";
  }

  public static Node operation(Object name, Consumer<? super Scope> consumer) {
    return new Node("OperationNode", String.valueOf(name)) {
      @Override
      public Stream<String> lines(Scope scope) {
        try {
          consumer.accept(scope);
        } catch (Exception e) {
          return error(scope, e);
        }
        return Stream.empty();
      }
    };
  }

  public static Node nop(String name) {
    return new Node("NopNode", name) {
      @Override
      public Stream<String> lines(Scope scope) {
        return Stream.empty();
      }
    };
  }
}
