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

package ch.raffael.compose.library.http.server.undertow.security;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

import java.util.function.Function;

/**
 * Represents a role. Role can imply other roles, support for that is
 * provided by the class {@link Hierarchy}.
 *
 * <p>The recommended way to define roles is using enums, the interface is
 * designed for that. An example:
 *
 * <pre>
 * enum ExampleRoles implements Role {
 *   GUEST,
 *   FOO(GUEST),
 *   BAR(GUEST),
 *   ADMIN(FOO, BAR);
 *
 *   private final Hierarchy&lt;ExampleRoles&gt; hierarchy;
 *
 *   ExampleRoles(ExampleRoles... parents) {
 *     hierarchy = new Hierarchy&lt;&gt;(this, r -> r.hierarchy, parents);
 *   }
 *
 *   &#64;Override
 *   public boolean implies(Role that) {
 *     return hierarchy.implies(that);
 *   }
 * }
 * </pre>
 */
public interface Role {

  String name();

  boolean implies(Role role);

  static OfName ofName(String name) {
    return new OfName(name);
  }

  interface Flat extends Role {
    @Override
    default boolean implies(Role role) {
      return equals(role);
    }
  }


  final class OfName implements Flat {
    private final String name;

    private OfName(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      //noinspection ObjectEquality
      if (o == null || getClass() != o.getClass()) return false;
      return ((OfName) o).name.equals(name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  class Hierarchy<R extends Role> {
    private final R self;
    final Set<Role> all;

    @SafeVarargs
    public Hierarchy(R self, Function<? super R, ? extends Hierarchy<R>> resolver, R... implied) {
      this(self, resolver, HashSet.of(implied));
    }

    public Hierarchy(R self, Function<? super R, ? extends Hierarchy<R>> resolver, Set<R> implied) {
      this.self = self;
      all = HashSet.<Role>of(self).addAll(implied.map(resolver).flatMap(h -> h.all));
    }

    public boolean implies(Role role) {
      return all.contains(role);
    }

    @Override
    public String toString() {
      return Role.class.getSimpleName() + "." + Hierarchy.class.getSimpleName()
          + "[" + self.name() + "->" + all.remove(self) + "]";
    }
  }
}
