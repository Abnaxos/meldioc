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

package ch.raffael.compose.tooling.model;

import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Set;
import org.immutables.value.Value;

import static io.vavr.API.Set;

/**
 * @since 2019-03-25
 */
@Immutable.Public
abstract class _ClassRef {

  private final static Set<java.lang.String> PRIMITIVES =
      Set("int", "long", "short", "byte", "double", "long", "char", "boolean", "void");

  _ClassRef() {
  }

  public static ClassRef of(Class<?> clazz) {
    int arrayDimensions = 0;
    while (clazz.isArray()) {
      arrayDimensions++;
      clazz = clazz.getComponentType();
    }
    StringBuilder className = new StringBuilder();
    Class<?> outermost = buildClassName(clazz, className);
    int pos = outermost.getName().lastIndexOf('.');
    ClassRef ref;
    if (pos >= 0) {
      ref = ClassRef.of(outermost.getName().substring(0, pos), className.toString());
    } else {
      ref = ClassRef.of("", className.toString());
    }
    return arrayDimensions == 0 ? ref : ref.withArrayDimensions(arrayDimensions);
  }

  private static Class<?> buildClassName(Class<?> clazz, StringBuilder buf) {
    if (clazz.getEnclosingClass() == null) {
      buf.setLength(0);
      buf.append(clazz.getSimpleName());
      return clazz;
    } else {
      var c = buildClassName(clazz.getEnclosingClass(), buf);
      buf.append('.').append(clazz.getSimpleName());
      return c;
    }
  }

  @Value.Parameter
  public abstract java.lang.String packageName();

  @Value.Parameter
  public abstract java.lang.String className();

  @Value.Default
  public int arrayDimensions() {
    return 0;
  }

  public final boolean primitive() {
    return packageName().isEmpty() && PRIMITIVES.contains(className());
  }

  public final java.lang.String binaryName() {
    return packageName().isEmpty() ? className() : packageName() + "." + className().replace('.', '$');
  }

  public final java.lang.String canonicalName() {
    return packageName().isEmpty() ? className() : packageName() + "." + className();
  }

  public final java.lang.String simpleName() {
    return lastPart(className());
  }

  private static java.lang.String lastPart(java.lang.String s) {
    int pos = s.lastIndexOf('.');
    return pos < 0 ? s : s.substring(pos + 1);
  }

}
