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
import io.vavr.collection.Stream;

import java.util.stream.Collectors;

public final class ErrorNode extends Node {

  private final Object message;

  public ErrorNode(Object message) {
    super(String.valueOf(message));
    this.message = message;
  }

  @Override
  public Stream<String> lines(Scope scope) {
    var msg = String.valueOf(message);
    scope.error(msg);
    return Stream.of(errorMessage(message));
  }

  public static String errorMessage(Object msg) {
    return "[[ERROR]] // " + msg.toString().lines().collect(Collectors.joining("\n//"));
  }
}
