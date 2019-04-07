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

package ch.raffael.compose.processor.model;

import ch.raffael.compose.Compose;
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.util.Elements;
import ch.raffael.compose.model.config.ComposeConfig;
import ch.raffael.compose.model.config.ConfigurationConfig;
import ch.raffael.compose.model.config.ExtensionPointProvisionConfig;
import ch.raffael.compose.model.config.ModelAnnotationType;
import ch.raffael.compose.model.config.MountConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import io.vavr.API;
import io.vavr.Function3;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Option;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import java.lang.annotation.Annotation;
import java.util.function.Function;

import static ch.raffael.compose.processor.util.Elements.asTypeElement;
import static ch.raffael.compose.util.Messages.capitalize;
import static io.vavr.API.*;
import static java.util.function.Function.identity;

/**
 * TODO javadoc
 */
public final class CompositionTypeModel extends Environment.WithEnv {

  private final DeclaredType type;
  private final TypeElement element;
  private final Pool pool;

  private final Seq<CompositionTypeModel> parents;

  private final Seq<ProvisionMethod> provisionMethods;
  private final Seq<ExtensionPointProvisionMethod> extensionPointProvisionMethods;
  private final Seq<ConfigurationMethod> configurationMethods;
  private final Seq<MountMethod> mountMethods;
  private final Seq<ComposeMethod> composeMethods;
  private final Option<String> configPrefix;

  private CompositionTypeModel(Environment env, Pool pool, DeclaredType type) {
    super(env);
    this.type = type;
    this.element = asTypeElement(type.asElement());
    this.pool = pool;
    parents = API.Seq(element.getSuperclass())
        .appendAll(element.getInterfaces())
        .reject(t -> t instanceof NoType)
        .map(t -> pool.modelOf((DeclaredType) t));
    var allMethods = env.elements().getAllMembers(element).stream()
        .filter(this::isProcessableMethod)
        .map(ExecutableElement.class::cast)
        .collect(Vector.collector());
    provisionMethods = findModelMethods(allMethods, Provision.class,
        ProvisionConfig::of, ProvisionMethod::of);
    extensionPointProvisionMethods = findModelMethods(allMethods, ExtensionPoint.Provision.class,
        ExtensionPointProvisionConfig::of, ExtensionPointProvisionMethod::of);
    configurationMethods = findModelMethods(allMethods, Configuration.class,
        ConfigurationConfig::of, ConfigurationMethod::of);
    mountMethods = findModelMethods(allMethods, Module.Mount.class,
        MountConfig::of, MountMethod::of);
    composeMethods = findModelMethods(allMethods, Compose.class,
        ComposeConfig::of, ComposeMethod::of);
    configPrefix = Option(element.getAnnotation(Configuration.Prefix.class))
        .map(Configuration.Prefix::value);
  }

  private <M, A extends Annotation, C> Seq<M> findModelMethods(
      Seq<? extends ExecutableElement> all,
      Class<A> annotation,
      Function<? super A, ? extends C> toConfig,
      Function3<CompositionTypeModel, ? super ExecutableElement, ? super C, ? extends M> toModel) {
    return all.map(m -> Option(m.getAnnotation(annotation))
        .map(toConfig)
        .map(c -> toModel.apply(this, m, c)))
        .flatMap(identity());
  }

  public Pool pool() {
    return pool;
  }

  public DeclaredType type() {
    return type;
  }

  public TypeElement element() {
    return element;
  }

  public Seq<CompositionTypeModel> parents() {
    return parents;
  }

  public Seq<ProvisionMethod> provisionMethods() {
    return provisionMethods;
  }

  public Seq<ExtensionPointProvisionMethod> extensionPointProvisionMethods() {
    return extensionPointProvisionMethods;
  }

  public Seq<Tuple2<ExtensionPointProvisionMethod, Option<MountMethod>>> allExtensionPointProvisionMethods() {
    return extensionPointProvisionMethods
        .<Tuple2<ExtensionPointProvisionMethod, Option<MountMethod>>>map(m -> Tuple(m, None()))
        .appendAll(mountMethods.flatMap(m ->
            pool.modelOf((DeclaredType) ((ExecutableType) env.types().asMemberOf(type, m.element())).getReturnType())
                .extensionPointProvisionMethods()
                .map(ep -> Tuple(ep, Some(m)))));
  }

  public Seq<ConfigurationMethod> configurationMethods() {
    return configurationMethods;
  }

  public Seq<ComposeMethod> composeMethods() {
    return composeMethods;
  }

  public Seq<MountMethod> mountMethods() {
    return mountMethods;
  }

  public Option<String> configurationPrefix() {
    return configPrefix;
  }

  private <T extends Element> boolean isProcessableMethod(T element) {
    if (Elements.isMethod(element)) {
      return isProcessableMethod((ExecutableElement) element);
    } else
      return false;
  }

  private boolean isProcessableMethod(ExecutableElement element) {
    var typeOption = Some(findModelAnnotationTypes(element)).flatMap(a -> {
      if (a.size() > 1) {
        env.problems().error(this.element, "Multiple incompatible annotations: " +
            a.map(ModelAnnotationType::displayName).mkString(", "));
        return None();
      } else {
        return a.headOption();
      }
    });
    if (typeOption.isEmpty()) {
      return false;
    }
    var type = typeOption.get();
    if (!element.getParameters().isEmpty()) {
      if (!type.supportsParameters()) {
        env.problems().error(element, "Role " + type.displayName());
        return false;
      }
    }
    if (!Elements.isNeitherStaticNorPrivate(element)) {
      env.problems().error(element, capitalize(type.displayName() + " methods cannot be static or private"));
      return false;
    }
    if (isObjectMethod(element)) {
      env.problems().error(element, "Standard object methods cannot be used as " + type.displayName() + " method");
      return false;
    }
    return true;
  }

  private <T extends Element> boolean isObjectMethod(ExecutableElement exec) {
    var objectMethod = env.known().objectMethods().contains(exec);
    if (objectMethod) {
      objectMethod = !env.known().objectMethods()
          .exists(m -> env.elements().overrides(exec, m, (TypeElement) exec.getEnclosingElement()));
    }
    return objectMethod;
  }

  private Seq<ModelAnnotationType> findModelAnnotationTypes(Element element) {
    return ModelAnnotationType.all()
        .map(t -> Option.when(element.getAnnotation(t.annotationType()) != null, t))
        .flatMap(identity());
  }

  public final static class Pool extends Environment.WithEnv {

    private HashMap<DeclaredType, Entry> pool = HashMap.empty();

    public Pool(Environment env) {
      super(env);
    }

    public CompositionTypeModel modelOf(DeclaredType type) {
      return pool.computeIfAbsent(type, e -> new Entry()).apply((e, m) -> {
        pool = m;
        if (e.model == null) {
          e.model = new CompositionTypeModel(env, this, type);
        }
        return e.model;
      });
    }

    private final static class Entry {
      @Nullable
      private CompositionTypeModel model;
    }

  }

}
