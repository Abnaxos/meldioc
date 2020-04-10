/*
 *  Copyright (c) 2020 Raffael Herzog
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

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.Map;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * Base class for all element configurations. These are raw representations
 * of the Meld annotations.
 */
public abstract class ElementConfig<S> {

  @SuppressWarnings("rawtypes")
  private static final
  Lazy<Map<Class<? extends Annotation>, Function<? super Annotation, ? extends ElementConfig>>>
      BUILDERS = Lazy.of(() -> Array.of(entry(Configuration.class, ConfigurationConfig::of),
      entry(Setup.class, SetupConfig::of),
      entry(Parameter.class, ParameterConfig::of),
      entry(ExtensionPoint.Acceptor.class, ExtensionPointAcceptorConfig::of),
      entry(Feature.class, FeatureConfig::of),
      entry(Provision.class, ProvisionConfig::of),
      entry(Feature.Mount.class, MountConfig::of))
      .toMap(t -> t));
  @SuppressWarnings("rawtypes")
  private static Tuple2<Class<? extends Annotation>, Function<? super Annotation, ? extends ElementConfig>>
  entry(Class<? extends Annotation> annotationType, Function<? super Annotation, ? extends ElementConfig> fun) {
    return Tuple.of(annotationType, fun);
  }

  @SuppressWarnings("unchecked")
  public static <A extends Annotation> ElementConfig<A> of(A annotation) {
    return (ElementConfig<A>)BUILDERS.get().get(annotation.annotationType())
        .getOrElseThrow(() -> new IllegalArgumentException("No model annotation type: " + annotation.annotationType()))
        .apply(annotation);
  }

  ElementConfig() {
  }

  public abstract ModelAnnotationType type();

  public abstract S source();

  public boolean isConfigType(Class<? extends Annotation> type) {
    return type().annotationType().equals(type);
  }

  public abstract Map<String, Object> valueMap();

  public Map<String, Object> valueMapWithoutDefaults() {
    return valueMap()
        .filter((n, v) -> type().attributes().get(n)
            .flatMap(AnnotationAttribute::defaultValue)
            .map(d -> !d.equals(v))
            .getOrElse(true));
  }

  public String displayName() {
    return type().displayName();
  }

}
