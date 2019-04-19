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

package ch.raffael.compose.model.config;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.Setup;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import org.immutables.value.Value;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import static io.vavr.API.*;

@Immutable.Public
abstract class _ModelAnnotationType {

  // referencing subclass in static initializer may be dangerous -> lazy to be sure
  private static final Lazy<Map<Class<? extends Annotation>, ModelAnnotationType>> ALL = Lazy.of(
      () -> Seq(mapEntry(Provision.class, b -> b.onMethod(true)),
          mapEntry(ExtensionPoint.Api.class, b -> b.onClass(true)),
          mapEntry(ExtensionPoint.Provision.class, b -> b.onMethod(true)),
          mapEntry(Parameter.class, b -> b.onMethod(true)),
          mapEntry(Parameter.Prefix.class, b -> b.onClass(true)),
          mapEntry(Setup.class, b -> b.onMethod(true).supportsParameters(true)),
          mapEntry(Module.Mount.class, b -> b.onMethod(true)),
          mapEntry(Module.class, b -> b.onClass(true)),
          mapEntry(Configuration.class, b -> b.onClass(true)),
          mapEntry(Module.DependsOn.class, b -> b.onImplements(true)))
          .toMap(t -> t));
  private static <T extends Annotation> Tuple2<Class<T>, ModelAnnotationType>
  mapEntry(Class<T> annotationType, Consumer<? super ModelAnnotationType.Builder> conf) {
    var builder = ModelAnnotationType.builder()
        .annotationType(annotationType);
    conf.accept(builder);
    return Tuple.of(annotationType, builder.build());
  }

  public static Seq<ModelAnnotationType> all() {
    return ALL.get().values();
  }

  public static <A extends Annotation> ModelAnnotationType of(Class<A> annotationType) {
    return ALL.get().get(annotationType).getOrElseThrow(
        () -> new IllegalArgumentException("Not a model annotation type: " + annotationType));
  }

  public abstract Class<? extends Annotation> annotationType();

  @Value.Derived
  @Value.Auxiliary
  public String displayName() {
    return annotationType().getSimpleName();
  }

  @Value.Default
  public boolean onMethod() {
    return false;
  }

  @Value.Default
  public boolean onClass() {
    return false;
  }

  @Value.Default
  public boolean onInterface() {
    return onClass();
  }

  @Value.Default
  public boolean onImplements() {
    return false;
  }

  @Value.Default
  public boolean supportsParameters() {
    return false;
  }

}
