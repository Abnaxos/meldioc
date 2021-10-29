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
import groovy.lang.MissingPropertyException;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;

import java.util.LinkedHashMap;

public class CompositeBinding extends Binding {

  private final Seq<? extends Binding> parents;

  public CompositeBinding() {
    this(new LinkedHashMap<>(), List.empty());
  }

  public CompositeBinding(Map<String, ?> bindings) {
    this(bindings.toJavaMap(), List.empty());
  }
  public CompositeBinding(java.util.Map<String, ?> bindings) {
    this(bindings, List.empty());
  }

  public CompositeBinding(Binding... bindings) {
    this(new LinkedHashMap<>(), List.of(bindings));
  }

  public CompositeBinding(Seq<? extends Binding> bindings) {
    this(new LinkedHashMap<>(), bindings);
  }

  public CompositeBinding(Map<String, ?> bindings, Binding... parents) {
    this(bindings.toJavaMap(), List.of(parents));
  }

  public CompositeBinding(java.util.Map<String, ?> bindings, Binding... parents) {
    this(bindings, List.of(parents));
  }

  public CompositeBinding(java.util.Map<String, ?> bindings, Seq<? extends Binding> parents) {
    super(bindings);
    this.parents = parents;
  }

  @SuppressWarnings("ObjectEquality")
  @Override
  public Object getVariable(String name) {
    if (super.hasVariable(name)) {
      var val = super.getVariable(name);
      if (val == Bindings.REMOVED) {
        throw new MissingPropertyException(name, getClass());
      }
    }
    return super.hasVariable(name)
           ? super.getVariable(name)
           : parents.find(b -> b.hasVariable(name)).map(b -> b.getVariable(name)).getOrNull();
  }

  @SuppressWarnings("ObjectEquality")
  @Override
  public boolean hasVariable(String name) {
    if (super.hasVariable(name)) {
      var val = super.getVariable(name);
      return val != Bindings.REMOVED;
    } else {
      return parents.exists(b -> b.hasVariable(name));
    }
  }

  @SuppressWarnings({"ObjectEquality", "rawtypes", "unchecked"})
  @Override
  public java.util.Map getVariables() {
    var result = new LinkedHashMap();
    parents.forEach(b -> {
      var m = b.getVariables();
      if (m != null) {
        result.putAll(m);
      }
    });
    java.util.Map m = super.getVariables();
    if (m != null) {
      m = new LinkedHashMap(m);
      m.entrySet().removeIf(e -> ((java.util.Map.Entry)e).getValue() == Bindings.REMOVED);
      result.putAll(m);
    }
    return result;
  }

  @Override
  public void removeVariable(String name) {
    super.setVariable(name, Bindings.REMOVED);
  }
}
