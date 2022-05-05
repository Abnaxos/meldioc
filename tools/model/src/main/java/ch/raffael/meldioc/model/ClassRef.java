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

package ch.raffael.meldioc.model;

import ch.raffael.meldioc.util.immutables.PureImmutable;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;
import org.immutables.value.Value;

/**
 * Named reference to a Java class.
 */
@Value.Immutable(builder = false)
@PureImmutable
public abstract class ClassRef implements ClassRef_With {

  public static final class Primitives {
    public static final ClassRef INT = of("", "int");
    public static final ClassRef LONG = of("", "long");
    public static final ClassRef SHORT = of("", "short");
    public static final ClassRef BYTE = of("", "byte");
    public static final ClassRef DOUBLE = of("", "double");
    public static final ClassRef FLOAT = of("", "float");
    public static final ClassRef CHAR = of("", "char");
    public static final ClassRef BOOLEAN = of("", "boolean");
    public static final ClassRef VOID = of("", "void");
    public static final Set<ClassRef> ALL = LinkedHashSet.of(INT, LONG, SHORT, BYTE, DOUBLE, FLOAT, CHAR, BOOLEAN);
  }
  public static final class Lang {
    public static final ClassRef OBJECT = of("java.lang", "Object");
    public static final ClassRef ENUM = of("java.lang", "Enum");
    public static final ClassRef NUMBER = of("java.lang", "Number");
    public static final ClassRef INTEGER = of("java.lang", "Integer");
    public static final ClassRef LONG = of("java.lang", "Long");
    public static final ClassRef SHORT = of("java.lang", "Short");
    public static final ClassRef BYTE = of("java.lang", "Byte");
    public static final ClassRef DOUBLE = of("java.lang", "Double");
    public static final ClassRef FLOAT = of("java.lang", "Float");
    public static final ClassRef CHAR = of("java.lang", "Character");
    public static final ClassRef BOOLEAN = of("java.lang", "Boolean");
    public static final ClassRef STRING = of("java.lang", "String");
    public static final ClassRef CHAR_SEQUENCE = of("java.lang", "CharSequence");
    public static final ClassRef THROWABLE = of("java.lang", "Throwable");
    public static final ClassRef EXCEPTION = of("java.lang", "Exception");
    public static final ClassRef RUNTIME_EXCEPTION = of("java.lang", "RuntimeException");
    public static final ClassRef ERROR = of("java.lang", "Error");
  }

  ClassRef() {}

  public static ClassRef of(String packageName, String className) {
    return ClassRef_Immutable.of(packageName, className);
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
      ref = of(outermost.getName().substring(0, pos), className.toString());
    } else {
      ref = of("", className.toString());
    }
    return arrayDimensions == 0 ? ref : ref.withArrayDimensions(arrayDimensions);
  }

  private static Class<?> buildClassName(Class<?> clazz, StringBuilder buf) {
    if (clazz.getEnclosingClass() == null) {
      buf.setLength(0);
      buf.append(clazz.getSimpleName());
      return clazz;
    } else {
      Class<?> c = buildClassName(clazz.getEnclosingClass(), buf);
      buf.append('.').append(clazz.getSimpleName());
      return c;
    }
  }

  @Value.Parameter
  public abstract String packageName();

  @Value.Parameter
  public abstract String className();

  @Value.Default
  public int arrayDimensions() {
    return 0;
  }

  public final ClassRef asArray() {
    return withArrayDimensions(arrayDimensions() + 1);
  }

  public final boolean isPrimitive() {
    return Primitives.ALL.contains(this);
  }

  public final String binaryName() {
    return packageName().isEmpty() ? className() : packageName() + "." + className().replace('.', '$');
  }

  public final String canonicalName() {
    return packageName().isEmpty() ? className() : packageName() + "." + className();
  }

  public final String simpleName() {
    return lastPart(className());
  }

  private static String lastPart(java.lang.String s) {
    int pos = s.lastIndexOf('.');
    return pos < 0 ? s : s.substring(pos + 1);
  }
}
