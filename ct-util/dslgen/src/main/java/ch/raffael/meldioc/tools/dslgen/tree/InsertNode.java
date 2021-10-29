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

package ch.raffael.meldioc.tools.dslgen.tree;

import ch.raffael.meldioc.tools.dslgen.Scope;
import ch.raffael.meldioc.tools.dslgen.expr.Expressions;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

public final class InsertNode extends Node {

  private final String expression;
  private final String indent;

  public InsertNode(Option<? extends Node> parent, String expression, String indent) {
    super(expression, parent);
    this.expression = expression;
    this.indent = indent;
  }

  @Override
  public Stream<String> lines(Scope scope) {
    try {
      return Stream.of(indent + Expressions.stringOrEval(scope.binding(), expression));
    } catch (Exception e) {
      return error(scope, e);
    }
  }
}
