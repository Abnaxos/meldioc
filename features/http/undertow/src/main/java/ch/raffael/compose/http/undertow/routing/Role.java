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

package ch.raffael.compose.http.undertow.routing;

import ch.raffael.compose.util.VavrX;
import io.vavr.collection.Array;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;

import java.util.function.Function;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public interface Role {

  String name();

  static OfName ofName(String name) {
    return new OfName(name);
  }

  default Set<? extends Role> all() {
    return Set(this);
  }

  static Set<Role> collect(Role root, Role... parents) {
    return collect(root, Array.of(parents));
  }

  @SafeVarargs
  static <T extends Role> Set<T> collect(T root, Function<? super T, ? extends Traversable <? extends T>> parentsFun, T... parents) {
    return collect(root, parentsFun, Array.of(parents));
  }

  static Set<Role> collect(Role root, Iterable<? extends Role> parents) {
    return collect(root, Role::all, VavrX.traversableOf(parents));
  }

  static Set<Role> collect(Role root, Traversable<? extends Role> parents) {
    return collect(root, Role::all, parents);
  }

  static <T extends Role> Set<T> collect(T root, Function<? super T, ? extends Traversable<? extends T>> parentsFun, Traversable<? extends T> parents) {
    return collect(Set(), root, parentsFun, parents);
  }

  private static <T extends Role> Set<T> collect(Set<T> all, T role, Function<? super T, ? extends Traversable<? extends T>> parentsFun, Traversable<? extends T> parents) {
    if (!all.contains(role)) {
      all = all.add(role);
      all = parents.foldLeft(all, (a, e) -> a.addAll(collect(a, e, parentsFun, parentsFun.apply(e))));
      //all = parents.forEach(p -> collect(all, p, parentsFun, parentsFun.apply(p)));
    }
    return all;
  }

  final class OfName implements Role {
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

}

