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

package ch.raffael.meldioc.model.config;

import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

@Immutable.Pure
public abstract class AnnotationAttribute implements AnnotationAttribute_With {
  AnnotationAttribute() {}
  public static Builder builder() {return new Builder();}
  static Map<String, AnnotationAttribute> allOf(Class<? extends Annotation> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(m -> !m.isSynthetic())
        .filter(m -> !Modifier.isStatic(m.getModifiers()))
        .map(AnnotationAttribute::of)
        .collect(LinkedHashMap.collector(AnnotationAttribute::name));
  }
  static AnnotationAttribute of(Method method) {
    boolean isArray = method.getReturnType().isArray();
    Class<?> type = isArray ? method.getReturnType().getComponentType() : method.getReturnType();
    if (Class.class.isAssignableFrom(type)) {
      type = ClassRef.class;
    }
    Object def = method.getDefaultValue();
    if (def instanceof Class) {
      def = ClassRef.of((Class<?>) def);
    }
    return AnnotationAttribute_Immutable.builder()
        .name(method.getName())
        .valueType(isArray ? Array.newInstance(type, 0).getClass() : type)
        .defaultValue(Option.of(def))
        .build();
  }

  public abstract String name();

  public abstract Class<?> valueType();

  public abstract Option<Object> defaultValue();

  public static final class Builder extends AnnotationAttribute_Immutable.Builder {
    Builder() {}
  }
}
