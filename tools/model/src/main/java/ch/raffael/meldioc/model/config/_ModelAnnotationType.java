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
import ch.raffael.meldioc.util.immutables.IllegalBuilderStateException;
import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import org.immutables.value.Value;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

@Immutable.Public
abstract class _ModelAnnotationType {

  // referencing subclass in static initializer may be dangerous -> lazy to be sure
  private static final Lazy<Map<Class<? extends Annotation>, ModelAnnotationType>> ALL_MAP = Lazy.of(
      () -> List.of(mapEntry(Provision.class, b -> b.onMethod().willImplement().willDecorate()),
          mapEntry(ExtensionPoint.Acceptor.class, b -> b.onClass().auxiliaryRole()),
          mapEntry(ExtensionPoint.class, b -> b.onMethod().willDecorate()),
          mapEntry(Parameter.class, b -> b.onMethod().willImplement().willDecorate()),
          mapEntry(Parameter.Prefix.class, b -> b.onClass().modifier()),
          mapEntry(Setup.class, b -> b.onMethod().supportsParameters()),
          mapEntry(Feature.Mount.class, b -> b.onMethod().willImplement()),
          mapEntry(Feature.class, b -> b.onClass().featureRole().willImplement().willDecorate()),
          mapEntry(Configuration.class, b -> b.onClass().featureRole().willImplement().willDecorate()),
          mapEntry(Feature.DependsOn.class, b -> b.modifier().onImplements()))
          .toMap(t -> t));
  private static final Lazy<Set<ModelAnnotationType>> ALL = Lazy.of(
      () -> LinkedHashSet.ofAll(ALL_MAP.get().values()));
  private static <T extends Annotation> Tuple2<Class<T>, ModelAnnotationType>
  mapEntry(Class<T> annotationType, Consumer<? super ModelAnnotationType.Builder> conf) {
    ModelAnnotationType.Builder builder = ModelAnnotationType.builder()
        .annotationType(annotationType);
    conf.accept(builder);
    return Tuple.of(annotationType, builder.build());
  }

  public static Set<ModelAnnotationType> all() {
    return ALL.get();
  }

  public static <A extends Annotation> ModelAnnotationType of(Class<A> annotationType) {
    return ALL_MAP.get().get(annotationType).getOrElseThrow(
        () -> new IllegalArgumentException("Not a model annotation type: " + annotationType));
  }

  public abstract Class<? extends Annotation> annotationType();

  @Value.Lazy
  @Value.Auxiliary
  public Map<String, AnnotationAttribute> attributes() {
    return _AnnotationAttribute.allOf(annotationType());
  }

  @Value.Derived
  @Value.Auxiliary
  public String displayName() {
    return "@" + annotationType().getSimpleName();
  }

  @Value.Default
  public boolean role() {
    return true;
  }

  public boolean modifier() {
    return !role();
  }

  @Value.Default
  public boolean auxiliaryRole() {
    return false;
  }

  @Value.Default
  public boolean featureRole() {
    return false;
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

  @Value.Default
  public boolean willDecorate() {
    return false;
  }

  @Value.Default
  public boolean willImplement() {
    return false;
  }

  @Value.Check
  void check() {
    if (featureRole() && !role()) {
      throw new IllegalBuilderStateException("featureRole but not role");
    }
    if (auxiliaryRole() && !role()) {
      throw new IllegalBuilderStateException("auxiliary role but not role");
    }
  }

  static abstract class Builder {

    Builder() {
    }

    public ModelAnnotationType.Builder modifier() {
      return self().role(false);
    }

    public ModelAnnotationType.Builder auxiliaryRole() {
      return self().auxiliaryRole(true);
    }

    public ModelAnnotationType.Builder featureRole() {
      return self().featureRole(true);
    }

    public ModelAnnotationType.Builder onMethod() {
      return self().onMethod(true);
    }

    public ModelAnnotationType.Builder onClass() {
      return self().onClass(true);
    }

    public ModelAnnotationType.Builder onInterface() {
      return self().onInterface(true);
    }

    public ModelAnnotationType.Builder onImplements() {
      return self().onImplements(true);
    }

    public ModelAnnotationType.Builder supportsParameters() {
      return self().supportsParameters(true);
    }

    public ModelAnnotationType.Builder willDecorate() {
      return self().willDecorate(true);
    }

    public ModelAnnotationType.Builder willImplement() {
      return self().willImplement(true);
    }

    private ModelAnnotationType.Builder self() {
      return (ModelAnnotationType.Builder) this;
    }
  }

}
