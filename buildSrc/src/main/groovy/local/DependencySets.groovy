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

package local

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler


/**
 * TODO javadoc
 */
class DependencySets implements Plugin<Project> {

  public static final String EXT = 'dependencySets'

  @Override
  void apply(Project target) {
    target.extensions.create(EXT, DependencySets_Extension, target.dependencies)
  }

  @SuppressWarnings("unused")
  static newDependencySet(DependencyHandler depHandler, String version, String group, Object... entries) {
    new DependencySet(depHandler, version, group, entries)
  }

  @EqualsAndHashCode
  @ToString
  static class DependencySet implements Collection<Dependency> {

    String group = null
    String version = null

    final List<Dependency> deps

    DependencySet(DependencyHandler depHandler, String version, String group, Object... entries) {
      this.group = group
      this.version = version
      List<Dependency> deps = []
      entries.each { e ->
        if (e instanceof Map) {
          e = e.clone()
          if (!group) group = e.group
          if (!version) version = e.version
          if (e.name) {
            if (!e.group) e.group = group
            if (!e.version) e.version = version
            Closure config = e.remove('configure') as Closure
            deps.add(config ? depHandler.create(e, config) : depHandler.create(e))
          }
        } else if (e instanceof CharSequence) {
          deps.add(depHandler.create([name: e as String, group: group, version: version]))
        } else if (e instanceof DependencySet) {
          deps.addAll(e)
        } else {
          throw new GradleException("Invalid element: (${e?.getClass()?.getSimpleName()}) $e")
        }
      }
      this.deps = deps.asImmutable()
    }

    private DependencySet(DependencySet parent, Closure filter) {
      this.group = parent.group
      this.version = parent.version
      this.deps = parent.deps.findAll(filter)
    }


    @SuppressWarnings("unused")
    def pick(String... names) {
      new DependencySet(this, {e -> names.contains(e.name)})
    }

    @SuppressWarnings("unused")
    def pick(Closure filter) {
      new DependencySet(this, filter)
    }

    @Override
    Iterator<Dependency> iterator() {
      deps.iterator()
    }

    @Override
    int size() {
      deps.size
    }

    @Override
    boolean isEmpty() {
      deps.isEmpty()
    }

    @Override
    boolean contains(Object o) {
      deps.contains(o)
    }

    @Override
    Object[] toArray() {
      deps.toArray()
    }

    @Override
    <T> T[] toArray(T[] a) {
      deps.toArray(a)
    }

    @Override
    boolean add(Dependency map) {
      throw new UnsupportedOperationException()
    }

    @Override
    boolean remove(Object o) {
      throw new UnsupportedOperationException()
    }

    @Override
    boolean containsAll(Collection<?> c) {
      deps.contains(c)
    }

    @Override
    boolean addAll(Collection<? extends Dependency> c) {
      throw new UnsupportedOperationException()
    }

    @Override
    boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException()
    }

    @Override
    boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException()
    }

    @Override
    void clear() {
      throw new UnsupportedOperationException()
    }
  }
}

/**
 * TODO javadoc
 */
class DependencySets_Extension {

  private final DependencyHandler dependencyHandler

  private final Map<String, DependencySets.DependencySet> dependencySets = [:]

  DependencySets_Extension(DependencyHandler dependencyHandler) {
    this.dependencyHandler = dependencyHandler
  }

  def propertyMissing(String name) {
    if (dependencySets.containsKey(name)) {
      return dependencySets[name]
    } else {
      throw new MissingPropertyException(name, getClass())
    }
  }

  def methodMissing(String name, Object args) {
    if (dependencySets.containsKey(name)) {
      throw new GradleException("Duplicate library set '$name'")
    }
    def argsList = [dependencyHandler] as List<Object>
    argsList.addAll(InvokerHelper.asArray(args) as List)
    dependencySets.put(name, InvokerHelper.invokeMethod(DependencySets, 'newDependencySet', argsList as Object[]) as DependencySets.DependencySet)
  }

}
