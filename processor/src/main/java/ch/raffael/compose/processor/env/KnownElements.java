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

package ch.raffael.compose.processor.env;

import ch.raffael.compose.$generated.$Provision;
import ch.raffael.compose.Assembly;
import ch.raffael.compose.Compose;
import ch.raffael.compose.tooling.util.Verified;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.raffael.compose.tooling.util.Verified.verify;

/**
 * TODO javadoc
 */
public class KnownElements extends Environment.WithEnv {

  public KnownElements(Environment env) {
    super(env);
  }

  private Verifiers verifiers() {
    return env.verifiers();
  }

  private final Verified<DeclaredType> object = declaredType(Object.class).memoize();
  public DeclaredType object() {
    return object.get();
  }

  private final Verified<Set<ExecutableElement>> objectMethods = verify(object)
      .map(DeclaredType::asElement)
      .map(TypeElement.class::cast)
      .map(t -> t.getEnclosedElements().stream()
          .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
          .filter(e -> !e.getSimpleName().toString().startsWith("<"))
          .filter(e -> e.getModifiers().contains(Modifier.PUBLIC) || e.getModifiers().contains(Modifier.PROTECTED))
          .collect(Collectors.toSet()))
      .map(Collections::unmodifiableSet)
      .memoize();
  public Set<ExecutableElement> objectMethods() {
    return objectMethods.get();
  }

  private final Verified<DeclaredType> rtProvision = declaredType($Provision.class).memoize();
  public DeclaredType rtProvision() {
    return rtProvision.get();
  }

  private final Verified<DeclaredType> annotation = declaredType(Annotation.class).memoize();
  public DeclaredType annotation() {
    return annotation.get();
  }

  private final Verified<DeclaredType> compose = annotationType(Compose.class).memoize();
  public DeclaredType compose() {
    return compose.get();
  }


  private final Verified<DeclaredType> assembly = annotationType(Assembly.class).memoize();
  public DeclaredType assembly() {
    return assembly.get();
  }

  private final Verified<ExecutableElement> assemblyBuilderName = annotationAttr(assembly, "shellName").memoize();
  public ExecutableElement assemblyShellName() {
    return assemblyBuilderName.get();
  }

  private final Verified<ExecutableElement> assemblyPackageLocal = annotationAttr(assembly, "packageLocal").memoize();
  public ExecutableElement assemblyPackageLocal() {
    return assemblyPackageLocal.get();
  }

  private Verified<DeclaredType> declaredType(Class<?> cls) {
    return Verified.of(() -> env.procEnv().getElementUtils().getTypeElement(cls.getCanonicalName()))
        .nonnull("Class %s not found", cls.getCanonicalName())
        .map(Element::asType)
        .instanceOf(DeclaredType.class);
  }

  private Verified<DeclaredType> annotationType(Class<?> cls) {
    return Verified.of(() -> env.procEnv().getElementUtils().getTypeElement(cls.getCanonicalName()))
        .nonnull("Class %s not found", cls.getCanonicalName())
        .map(Element::asType)
        .instanceOf(DeclaredType.class)
        .with(verifiers().subtypeOf(this::annotation));
  }

  private Verified<ExecutableElement> annotationAttr(Verified<? extends DeclaredType> type, String name) {
    return verify(type)
        .map(t -> (TypeElement) t.asElement())
        .map(t -> t.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance)
            .map(ExecutableElement.class::cast)
            .filter(m -> m.getParameters().isEmpty())
            .filter(m -> name.equals(m.getSimpleName().toString()))
            .findAny())
        .with(Verified.present((v) -> f("Attribute %s not found in annotation type %s", name, type.get())))
        .map(Optional::get);
  }

  private static String f(String format, Object... args) {
    return String.format(format, args);
  }

}
