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
import com.squareup.javapoet.ClassName;
import io.vavr.Lazy;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.raffael.compose.tooling.util.Verified.verify;
import static java.util.Objects.requireNonNull;

/**
 * TODO javadoc
 */
public class KnownElements extends Environment.WithEnv {

  public static final String CONFIG_PACKAGE = "com.typesafe.config";
  public static final ClassName CONFIG_TYPE = ClassName.get(CONFIG_PACKAGE, "Config");
  public static final ClassName CONFIG_MEMORY_SIZE_TYPE = ClassName.get(CONFIG_PACKAGE, "ConfigMemorySize");
  public static final ClassName CONFIG_NOT_RESOLVED_EXCEPTION_TYPE =
      ClassName.get(CONFIG_PACKAGE, "ConfigException", "NotResolved");

  public KnownElements(Environment env) {
    super(env);
  }

  private Verifiers verifiers() {
    return env.verifiers();
  }

  private final Lazy<DeclaredType> object = lazyDeclaredType(Object.class);
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

  private final Lazy<DeclaredType> enumeration = lazyDeclaredType(Enum.class);

  public DeclaredType enumeration() {
    return enumeration.get();
  }

  private final Lazy<DeclaredType> charSequence = lazyDeclaredType(CharSequence.class);
  public DeclaredType charSequence() {
    return charSequence.get();
  }

  private final Lazy<DeclaredType> string = lazyDeclaredType(String.class);
  public DeclaredType string() {
    return string.get();
  }

  private final Lazy<DeclaredType> iterable = lazyDeclaredType(Iterable.class);
  public DeclaredType iterable() {
    return iterable.get();
  }

  private final Lazy<ExecutableElement> iterableIterator = Lazy.of(() -> ((TypeElement) iterable().asElement())
      .getEnclosedElements().stream()
      .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
      .filter(e -> e.getParameters().isEmpty())
      .filter(e -> e.getSimpleName().toString().equals("iterator"))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("Iterable::iterator() not found")));
  public ExecutableElement iterableIterator() {
    return iterableIterator.get();
  }

  private final Lazy<DeclaredType> iterator = lazyDeclaredType(Iterator.class);
  public DeclaredType iterator() {
    return iterator.get();
  }

  private final Lazy<ExecutableElement> iteratorNext = Lazy.of(() -> ((TypeElement) iterator().asElement())
      .getEnclosedElements().stream()
      .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
      .filter(e -> e.getParameters().isEmpty())
      .filter(e -> e.getSimpleName().toString().equals("next"))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("Iterator::next() not found")));
  public ExecutableElement iteratorNext() {
    return iteratorNext.get();
  }

  private final Lazy<DeclaredType> number = lazyDeclaredType(Number.class);
  public DeclaredType number() {
    return number.get();
  }

  private final Lazy<DeclaredType> duration = lazyDeclaredType(Duration.class);
  public DeclaredType duration() {
    return duration.get();
  }

  private final Lazy<DeclaredType> period = lazyDeclaredType(Period.class);
  public DeclaredType period() {
    return period.get();
  }

  private final Lazy<DeclaredType> temporalAmount = lazyDeclaredType(TemporalAmount.class);
  public DeclaredType temporalAmount() {
    return temporalAmount.get();
  }

  private final Lazy<Optional<DeclaredType>> config = Lazy.of(() -> optionalDeclaredType(CONFIG_TYPE));
  public Optional<DeclaredType> config() {
    return config.get();
  }

  private final Lazy<Optional<DeclaredType>> configMemorySize = Lazy.of(() -> optionalDeclaredType(CONFIG_MEMORY_SIZE_TYPE));
  public Optional<DeclaredType> configMemorySize() {
    return configMemorySize.get();
  }

  private final Lazy<DeclaredType> rtProvision = lazyDeclaredType($Provision.class);
  public DeclaredType rtProvision() {
    return rtProvision.get();
  }

  private final Lazy<DeclaredType> annotation = lazyDeclaredType(Annotation.class);
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

  private DeclaredType declaredType(Class<?> type) {
    return (DeclaredType) requireNonNull(
        env.elements().getTypeElement(type.getCanonicalName()), "Type " + type.getName()).asType();
  }

  private Optional<DeclaredType> optionalDeclaredType(ClassName className) {
    return Optional.ofNullable((DeclaredType) env.elements().getTypeElement(className.toString()).asType());
  }

  private Lazy<DeclaredType> lazyDeclaredType(Class<?> type) {
    return Lazy.of(() -> declaredType(type));
  }

  private static String f(String format, Object... args) {
    return String.format(format, args);
  }

}
