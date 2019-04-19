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

package ch.raffael.compose.model;

import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Set;
import org.immutables.value.Value;

import static io.vavr.API.*;

/**
 * Named reference to a Java class.
 */
@Immutable.Public
abstract class _ClassRef {

  public static final class Primitives {
    public static final ClassRef INT = ClassRef.of("", "int");
    public static final ClassRef LONG = ClassRef.of("", "long");
    public static final ClassRef SHORT = ClassRef.of("", "short");
    public static final ClassRef BYTE = ClassRef.of("", "byte");
    public static final ClassRef DOUBLE = ClassRef.of("", "double");
    public static final ClassRef FLOAT = ClassRef.of("", "float");
    public static final ClassRef CHAR = ClassRef.of("", "char");
    public static final ClassRef BOOLEAN = ClassRef.of("", "boolean");
    public static final Set<ClassRef> ALL = Set(INT, LONG, SHORT, BYTE, DOUBLE, FLOAT, CHAR, BOOLEAN);
  }
  public static final class Lang {
    public static final ClassRef OBJECT = ClassRef.of("java.lang", "Object");
    public static final ClassRef ENUM = ClassRef.of("java.lang", "Enum");
    public static final ClassRef NUMBER = ClassRef.of("java.lang", "Number");
    public static final ClassRef INTEGER = ClassRef.of("java.lang", "Integer");
    public static final ClassRef LONG = ClassRef.of("java.lang", "Long");
    public static final ClassRef SHORT = ClassRef.of("java.lang", "Short");
    public static final ClassRef BYTE = ClassRef.of("java.lang", "Byte");
    public static final ClassRef DOUBLE = ClassRef.of("java.lang", "Double");
    public static final ClassRef FLOAT = ClassRef.of("java.lang", "Float");
    public static final ClassRef CHAR = ClassRef.of("java.lang", "Character");
    public static final ClassRef BOOLEAN = ClassRef.of("java.lang", "Boolean");
    public static final ClassRef STRING = ClassRef.of("java.lang", "String");
    public static final ClassRef CHAR_SEQUENCE = ClassRef.of("java.lang", "CharSequence");
    public static final ClassRef THROWABLE = ClassRef.of("java.lang", "Throwable");
    public static final ClassRef EXCEPTION = ClassRef.of("java.lang", "Exception");
    public static final ClassRef RUNTIME_EXCEPTION = ClassRef.of("java.lang", "RuntimeException");
    public static final ClassRef ERROR = ClassRef.of("java.lang", "Error");
  }

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

  public final boolean isPrimitive() {
    return Primitives.ALL.contains((ClassRef)this);
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
