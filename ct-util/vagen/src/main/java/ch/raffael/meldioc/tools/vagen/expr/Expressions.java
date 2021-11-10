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

package ch.raffael.meldioc.tools.vagen.expr;

import ch.raffael.meldioc.tools.vagen.expr.support.RangeCategory;
import ch.raffael.meldioc.tools.vagen.expr.support.StringCategory;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MethodClosure;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

public final class Expressions {

  private static final Seq<Class<?>> CATEGORIES = List.of(RangeCategory.class, StringCategory.class);
  private static final Seq<Class<?>> BUILTINS = List.empty();
  private static final String PREAMBLE = CATEGORIES.map(c -> "use(" + c.getCanonicalName() + ") {").mkString("\n");
  private static final String POSTAMBLE = Stream.range(0, CATEGORIES.length()).map(__ -> "}").mkString();
  //private static final int LINE_OFFSET = -CATEGORIES.length();

  private static final GroovyClassLoader GROOVY_CL = new GroovyClassLoader(Expressions.class.getClassLoader());
  @SuppressWarnings("StaticCollection")
  private static final ConcurrentHashMap<String, Class<?>> SCRIPT_CACHE = new ConcurrentHashMap<>(1);

  private static final ReadOnlyBinding BUILTINS_BINDING = new ReadOnlyBinding(
          BUILTINS.toStream().appendAll(CATEGORIES)
              .flatMap(c ->
                  Stream.of(c.getDeclaredMethods())
                      .filter(m -> Modifier.isStatic(m.getModifiers())
                          && Modifier.isPublic(m.getModifiers())
                          && m.isAnnotationPresent(Builtin.class))
                      .map(Method::getName)
                      .distinct()
                      .map(m -> Tuple.of(m, new MethodClosure(c, m))))
              .groupBy(io.vavr.Tuple2::_1)
              .mapValues(v -> v.head()._2()));

  private Expressions() {
  }

  @Nullable
  public static Object eval(Binding binding, String groovy) {
    groovy = PREAMBLE + "\n" + groovy + "\n" + POSTAMBLE;
    var scriptClass = SCRIPT_CACHE.computeIfAbsent(groovy, GROOVY_CL::parseClass);
    Script script;
    try {
      script = (Script)scriptClass.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException("Cannot instantiate Groovy script " + scriptClass, e);
    }
    script.setBinding(new CompositeBinding(binding, BUILTINS_BINDING));
    return script.run();
  }

  public static String evalToString(Binding binding, String groovy) {
    return asString(eval(binding, groovy));
  }

  public static String stringOrEval(Binding binding, String stringOrEval) {
    stringOrEval = stringOrEval.trim();
    if (stringOrEval.isEmpty()) {
      return "";
    } else if (stringOrEval.startsWith("`")) {
      stringOrEval = stringOrEval.substring(1);
      if (stringOrEval.endsWith("`")) {
        stringOrEval = stringOrEval.substring(0, stringOrEval.length() - 1);
      }
      return stringOrEval;
    } else {
      return evalToString(binding, stringOrEval);
    }
  }

  public static String asString(@Nullable Object value) {
    return value == null ? "" : DefaultGroovyMethods.asType(value, String.class);
  }

  public static boolean isGroovyTrue(Object object) {
    return Boolean.TRUE.equals(InvokerHelper.invokeMethod(object, "asBoolean", null));
  }
}
