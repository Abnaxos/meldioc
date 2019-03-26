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

import ch.raffael.compose.Context;
import ch.raffael.compose.Compose;
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Mount;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Map;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import static io.vavr.API.Seq;

/**
 * @since 2019-03-25
 */
public abstract class ModelElementConfig {

  private static final
  Lazy<Map<Class<? extends Annotation>, Function<? super Annotation, ? extends ModelElementConfig>>>
      BUILDERS = Lazy.of(() -> Seq(entry(Context.class, AssemblyConfig::of),
      entry(Compose.class, ComposeConfig::of),
      entry(Configuration.class, ConfigurationConfig::of),
      entry(ExtensionPoint.class, ExtensionPointConfig::of),
      entry(Module.class, ModuleConfig::of),
      entry(Provision.class, ProvisionConfig::of),
      entry(Mount.class, MountConfig::of))
      .toMap(t -> t));
  private static Tuple2<Class<? extends Annotation>, Function<? super Annotation, ? extends ModelElementConfig>>
  entry(Class<? extends Annotation> annotationType, Function<? super Annotation, ? extends ModelElementConfig> fun) {
    return Tuple.of(annotationType, fun);
  }

  public static ModelElementConfig of(Annotation annotation) {
    return BUILDERS.get().get(annotation.annotationType())
        .getOrElseThrow(() -> new IllegalArgumentException("No model annotation type: " + annotation.annotationType()))
        .apply(annotation);
  }

  ModelElementConfig() {
  }

  public abstract ModelAnnotationType type();

}
