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

package ch.raffael.meldioc.tools.dslgen.expr;

import groovy.lang.Binding;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;

public class ReadOnlyBinding extends CompositeBinding {

  public ReadOnlyBinding(Map<String, ?> bindings) {
    super(bindings);
  }

  public ReadOnlyBinding(java.util.Map<String, ?> bindings) {
    super(bindings);
  }

  public ReadOnlyBinding(Map<String, ?> bindings, Binding... parents) {
    super(bindings, parents);
  }

  public ReadOnlyBinding(java.util.Map<String, ?> bindings, Binding... parents) {
    super(bindings, parents);
  }

  public ReadOnlyBinding(java.util.Map<String, ?> bindings, Seq<? extends Binding> parents) {
    super(bindings, parents);
  }

  @Override
  public void setVariable(String name, Object value) {
    throw new UnsupportedOperationException("Read-only binding: " + this);
  }

  @Override
  public void removeVariable(String name) {
    throw new UnsupportedOperationException("Read-only binding: " + this);
  }
}
