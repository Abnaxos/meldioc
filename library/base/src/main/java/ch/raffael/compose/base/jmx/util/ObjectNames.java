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

package ch.raffael.compose.base.jmx.util;

import ch.raffael.compose.util.Classes;
import io.vavr.collection.Seq;

import javax.management.ObjectName;

import static io.vavr.API.*;

public final class ObjectNames {

  private static final Seq<String> MBEAN_POSTFIXES = Seq("MBean", "MXBean");
  private static final String SPECIAL_NAME_CHARS = "\"*?\\";

  private ObjectNames() {
  }

  public static String strippedName(String name) {
    return MBEAN_POSTFIXES.find(name::endsWith)
        .fold(
            () -> name,
            p -> name.substring(0, name.length() - p.length()));
  }

  public static String strippedName(Class<?> clazz) {
    return strippedName(clazz.getCanonicalName());
  }

  public static String typeName(Class<?> clazz) {
    return typeName(clazz, false);
  }

  public static String typeName(Class<?> clazz, boolean verbatim) {
    var n = Classes.outermost(clazz).getSimpleName();
    return verbatim ? n : strippedName(n);
  }

  public static String typeName(String qualifiedName) {
    return typeName(qualifiedName, false);
  }

  public static String typeName(String qualifiedName, boolean verbatim) {
    var pos = qualifiedName.lastIndexOf('.');
    var n = pos < 0 ? strippedName(qualifiedName) : strippedName(qualifiedName.substring(pos + 1));
    return verbatim ? n : strippedName(n);
  }

  public static Class<?> typeGivingClass(Class<?> clazz) {
    return Classes.outermost(clazz);
  }

  public static String quoteIfNecessary(String component) {
    return shouldQuote(component) ? ObjectName.quote(component) : component;
  }

  public static boolean shouldQuote(String component) {
    if (component.isEmpty()) {
      return true;
    }
    for (int i = 0; i < component.length(); i++) {
      char c = component.charAt(i);
      if (SPECIAL_NAME_CHARS.indexOf(c) >= 0 || Character.isWhitespace(c)) {
        return true;
      }
    }
    return false;
  }

  public static String packageName(String qualifiedName) {
    return packageName(qualifiedName, 0);
  }

  public static String packageName(Class<?> clazz) {
    return packageName(clazz, 0);
  }

  public static String packageName(Class<?> clazz, int up) {
    return packageName(clazz.getName(), up);
  }

  public static String packageName(String qualifiedName, int up) {
    if (qualifiedName.isEmpty()) {
      throw new IllegalArgumentException("Empty qualified name");
    }
    int pos = qualifiedName.lastIndexOf('.');
    checkQNamePos(qualifiedName, pos, 0);
    for (int i = 0; i < up; i++) {
      pos = qualifiedName.lastIndexOf('.', pos - 1);
      checkQNamePos(qualifiedName, pos, i);
    }
    return qualifiedName.substring(0, pos);
  }

  private static void checkQNamePos(String qualifiedName, int pos, int depth) {
    if (pos <= 0) {
      throw new IllegalArgumentException("Empty package name for qualified name '" + qualifiedName + "' at " + depth);
    }
  }
}
