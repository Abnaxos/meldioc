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
import ch.raffael.compose.Provision;
import ch.raffael.compose.Mount;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.util.ElementPredicates;
import ch.raffael.compose.tooling.model.ComposeConfig;
import ch.raffael.compose.tooling.model.ConfigurationConfig;
import ch.raffael.compose.tooling.model.ExtensionPointConfig;
import ch.raffael.compose.tooling.model.ModelAnnotationType;
import ch.raffael.compose.tooling.model.ProvisionConfig;
import ch.raffael.compose.tooling.model.MountConfig;
import ch.raffael.compose.tooling.util.Verified;
import ch.raffael.compose.util.fun.Fun;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static ch.raffael.compose.util.Messages.capitalize;
import static ch.raffael.compose.util.fun.Fun.let;
import static ch.raffael.compose.util.fun.Fun.none;
import static ch.raffael.compose.util.fun.Fun.some;

/**
 * @since 2019-03-24
 */
public class CompositionInfo extends Environment.WithEnv {

  private final TypeElement element;

  private final Seq<ProvisionMethod> provisionMethods;
  private final Seq<ExtensionPointMethod> extensionPointMethods;
  private final Seq<ConfigurationMethod> configurationMethods;
  private final Seq<MountMethod> mountMethods;
  private final Seq<ComposeMethod> composeMethods;

  public CompositionInfo(Environment env, TypeElement element) {
    super(env);
    this.element = element;
    var all = env.elements().getAllMembers(element).stream()
        .filter(this::isProcessableMethod)
        .map(ExecutableElement.class::cast)
        .collect(Vector.collector());
    provisionMethods = findModelMethods(all, Provision.class,
        ProvisionConfig::of, ProvisionMethod::of);
    extensionPointMethods = findModelMethods(all, ExtensionPoint.class,
        ExtensionPointConfig::of, ExtensionPointMethod::of);
    configurationMethods = findModelMethods(all, Configuration.class,
        ConfigurationConfig::of, ConfigurationMethod::of);
    mountMethods = findModelMethods(all, Mount.class,
        MountConfig::of, MountMethod::of);
    composeMethods = findModelMethods(all, Compose.class,
        ComposeConfig::of, ComposeMethod::of);
  }

  private <M, A extends Annotation, C> Seq<M> findModelMethods(
      Seq<? extends ExecutableElement> all,
      Class<A> annotation,
      Function<? super A, ? extends C> toConfig,
      BiFunction<? super ExecutableElement, ? super C, ? extends M> toModel) {
    return all.map(m -> Optional.ofNullable(m.getAnnotation(annotation))
        .map(toConfig)
        .map(c -> toModel.apply(m, c)))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }
//  private <M, A extends Annotation> List<M> findModelMethods(
//      ElementRole role,
//      Class<A> annotation,
//      BiFunction<ExecutableElement, ? super A, M> toModel) {
//    return allMethods.stream()
//        .map(m -> Optional.ofNullable(m.getAnnotation(annotation))
//            .map(a -> toModel.apply(m, a)))
//        .flatMap(Optional::stream)
//        .collect(Collectors.toList());
//  }

  void validate() {
    // TODO (2019-03-24) check for collisions
    // TODO (2019-03-24) filter and report final methods
    // mount methods *must* be abstract
  }

  public TypeElement element() {
    return element;
  }

  public Seq<ProvisionMethod> provisionMethods() {
    return provisionMethods;
  }

  public Seq<ExtensionPointMethod> extensionPointMethods() {
    return extensionPointMethods;
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

  private <T extends Element> boolean isProcessableMethod(T element) {
    if (ElementPredicates.isMethod(element)) {
      return isProcessableMethod((ExecutableElement) element);
    } else
      return false;
  }

  private boolean isProcessableMethod(ExecutableElement element) {
    Optional<ModelAnnotationType> typeOption = let(findModelAnnotationTypes(element), a -> {
      if (a.isEmpty()) {
        return none();
      } else if (a.size() > 1) {
        env.problems().error(this.element, "Multiple incompatible annotations: " +
            a.map(ModelAnnotationType::displayName).mkString(", "));
        return none();
      }
      return some(a.head());
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
    if (!ElementPredicates.isNeitherStaticNorPrivate(element)) {
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
      var type = Verified.verify(exec.getEnclosingElement())
          .nonnull("Method's enclosing element is null: %s", exec)
          .instanceOf(TypeElement.class, "Method's enclosing element is not a type: %s", exec)
          .get();
      objectMethod = env.known().objectMethods().stream()
          .noneMatch(m -> env.elements().overrides(exec, m, type));
    }
    return objectMethod;
  }

  private Seq<ModelAnnotationType> findModelAnnotationTypes(Element element) {
    return ModelAnnotationType.all()
        .map(t -> let(element.getAnnotation(t.annotationType()),
            a -> a == null ? Fun.<ModelAnnotationType>none() : some(t)))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

}
