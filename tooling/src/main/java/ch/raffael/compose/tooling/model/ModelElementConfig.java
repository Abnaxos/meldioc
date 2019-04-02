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

import ch.raffael.compose.Assembly;
import ch.raffael.compose.Compose;
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.Map;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * TODO javadoc
 */
public abstract class ModelElementConfig<S> {

  private static final
  Lazy<Map<Class<? extends Annotation>, Function<? super Annotation, ? extends ModelElementConfig>>>
      BUILDERS = Lazy.of(() -> Array.of(entry(Assembly.class, AssemblyConfig::of),
      entry(Compose.class, ComposeConfig::of),
      entry(Configuration.class, ConfigurationConfig::of),
      entry(ExtensionPoint.Api.class, ExtensionPointApiConfig::of),
      entry(Module.class, ModuleConfig::of),
      entry(Provision.class, ProvisionConfig::of),
      entry(Module.Mount.class, MountConfig::of))
      .toMap(t -> t));
  private static Tuple2<Class<? extends Annotation>, Function<? super Annotation, ? extends ModelElementConfig>>
  entry(Class<? extends Annotation> annotationType, Function<? super Annotation, ? extends ModelElementConfig> fun) {
    return Tuple.of(annotationType, fun);
  }

  @SuppressWarnings("unchecked")
  public static <A extends Annotation> ModelElementConfig<A> of(A annotation) {
    return (ModelElementConfig<A>)BUILDERS.get().get(annotation.annotationType())
        .getOrElseThrow(() -> new IllegalArgumentException("No model annotation type: " + annotation.annotationType()))
        .apply(annotation);
  }

  ModelElementConfig() {
  }

  public abstract ModelAnnotationType type();

  public abstract S source();

  public static final class UnknownSource {
    @SuppressWarnings("InstantiationOfUtilityClass")
    private static final UnknownSource INSTANCE = new UnknownSource();
    private UnknownSource() {
    }
    public static UnknownSource instance() {
      return INSTANCE;
    }
    public static UnknownSource unknownSource() {
      return INSTANCE;
    }
  }


}
