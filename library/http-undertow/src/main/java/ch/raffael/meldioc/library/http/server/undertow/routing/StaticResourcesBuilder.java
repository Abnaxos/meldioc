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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import javax.annotation.Nullable;
import java.util.EnumSet;

public final class StaticResourcesBuilder {

  private static final StackWalker STACK_WALKER = StackWalker.getInstance(
      EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), 2);

  @Nullable
  private ResourceManager resourceManager = null;

  StaticResourcesBuilder() {
  }

  public StaticResourcesBuilder classpath(String path) {
    checkResourceManagerChangeable();
    var c = baseFromCaller();
    return classpath(c._1(), c._2(), path);
  }


  public StaticResourcesBuilder classpath(Class<?> base, String path) {
    checkResourceManagerChangeable();
    return classpath(base.getClassLoader(), baseForClass(base), path);
  }

  public StaticResourcesBuilder classpath(ClassLoader loader, String base, String path) {
    checkResourceManagerChangeable();
    boolean absolute = false;
    while (path.length() > 0 && path.charAt(0) == '/') {
      absolute = true;
      path = path.substring(1);
    }
    if (!absolute) {
      path = path.isEmpty() ? base : base + "/" + path;
    }
    resourceManager = new ClassPathResourceManager(loader, path);
    return this;
  }

  HttpHandler materialize(HttpHandler next) {
    return new ResourceHandler(resourceManager, next);
  }

  private void checkResourceManagerChangeable() {
    if (resourceManager != null) {
      throw new IllegalStateException("Resource manager already set");
    }
  }

  @SuppressWarnings("ObjectEquality")
  private Tuple2<ClassLoader, String> baseFromCaller() {
    if (resourceManager == null) {
      throw new IllegalStateException("No resource manager set");
    }
    return STACK_WALKER.walk(s -> s
        .map(StackWalker.StackFrame::getDeclaringClass)
        .dropWhile(c -> c != StaticResourcesBuilder.class)
        .dropWhile(c -> c == StaticResourcesBuilder.class)
        .findFirst()
        .map(c -> Tuple.of(c.getClassLoader(), baseForClass(c)))
        .orElseThrow(() -> new IllegalStateException("Cannot determine caller class")));
  }

  private String baseForClass(Class<?> c) {
    return c.getPackageName().replace('.', '/');
  }
}
