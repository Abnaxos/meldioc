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
import io.vavr.collection.Seq;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;

public class CompositeBinding extends Binding {

  private final Seq<? extends Binding> parents;

  public CompositeBinding(Binding... parents) {
    this(List.of(parents));
  }

  public CompositeBinding(Seq<? extends Binding> parents) {
    super(java.util.Map.of());
    if (parents.isEmpty()) {
      throw new IllegalArgumentException("Empty composite binding");
    }
    this.parents = parents;
  }

  public Binding getPrimaryBinding() {
    return parents.head();
  }

  private static final ThreadLocal<IdentityHashMap<Binding, Object>> RECURSION = new ThreadLocal<>();

  @Override
  public Object getVariable(String name) {
//    return parents.toStream().flatMap(p -> {
//      try {
//        return some(p.getVariable(name));
//      } catch (MissingPropertyException e) {
//        return none();
//      }
//    }).headOption().getOrElseThrow(() -> new MissingPropertyException(name, getClass()));
    var rec = RECURSION.get();
    if (rec == null) {
      rec = new IdentityHashMap<>();
      RECURSION.set(rec);
    }
    if (rec.put(this, true) != null) {
      throw new IllegalStateException("RECURSION");
    }
    try {
      return parents
          .find(p -> p.hasVariable(name)).map(p -> p.getVariable(name))
          .getOrElseThrow(() -> new MissingPropertyException(name, getClass()));
    } finally {
      rec.remove(this);
      if (rec.isEmpty()) {
        RECURSION.remove();
      }
    }
  }

  @Override
  public boolean hasVariable(String name) {
    return parents.exists(p -> p.hasVariable(name));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public java.util.Map getVariables() {
    var result = new LinkedHashMap();
    parents.forEach(b -> {
      var m = b.getVariables();
      if (m != null) {
        result.putAll(m);
      }
    });
    return result;
  }

  @Override
  public void setVariable(String name, Object value) {
    getPrimaryBinding().setVariable(name, value);
  }

  @Override
  public void removeVariable(String name) {
    getPrimaryBinding().removeVariable(name);
  }
}
