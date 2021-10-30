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

package ch.raffael.meldioc.tools.dslgen.expr.support;

import ch.raffael.meldioc.tools.dslgen.expr.Builtin;
import groovy.lang.Category;
import groovy.lang.Closure;

import java.util.regex.Pattern;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asType;

@Category(CharSequence.class)
public final class StringCategory {

  private StringCategory() {
  }

  @Builtin
  public static String wrap(CharSequence self, CharSequence wrap) {
    return wrap(self, wrap, "{}");
  }

  @Builtin
  public static String wrap(CharSequence self, CharSequence wrap, CharSequence marker) {
    return wrap(self, asType(wrap, String.class), asType(marker, String.class));
  }

  @Builtin
  public static String wrap(CharSequence self, String wrap, String marker) {
    var str = asType(self, String.class);
    return str.isEmpty() ? "" : wrap.replace(marker, str);
  }

  @Builtin
  public static String wrap(CharSequence self, Closure<?> wrap) {
    String str = asType(self, String.class);
    return str.isEmpty() ? "" : asType(wrap.call(str), String.class);
  }

  @Builtin
  public static String emptyOr(CharSequence self, CharSequence replacement) {
    if (self.length() == 0) {
      return "";
    } else {
      return asType(replacement, String.class);
    }
  }

  @Builtin
  public static String emptyOr(CharSequence self, Closure<?> closure) {
    if (self.length() == 0) {
      return "";
    } else {
      return asType(closure.call(self), String.class);
    }
  }

  @Builtin
  public static String rm(CharSequence self, CharSequence rm) {
    return asType(self, String.class).replace(rm, "");
  }

  @Builtin
  public static String rm(CharSequence self, Pattern rm) {
    return rm.matcher(self).replaceAll("");
  }
}
