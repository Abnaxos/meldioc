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

package ch.raffael.meldioc.processor.env;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import ch.raffael.meldioc.model.config.ConfigurationConfig;
import ch.raffael.meldioc.model.config.MountConfig;
import ch.raffael.meldioc.model.config.ParameterConfig;
import ch.raffael.meldioc.model.config.ParameterPrefixConfig;
import ch.raffael.meldioc.model.config.ProvisionConfig;
import ch.raffael.meldioc.processor.util.Elements;
import ch.raffael.meldioc.rt.$Shared;
import com.squareup.javapoet.ClassName;
import io.vavr.Lazy;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;

import javax.annotation.processing.Generated;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static io.vavr.API.*;
import static java.util.Objects.requireNonNull;

/**
 * Known {@code javax.lang.model} elements such as {@code Object} or our
 * annotations.
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

  private final Lazy<DeclaredType> object = lazyDeclaredType(Object.class);
  public DeclaredType object() {
    return object.get();
  }

  private final Lazy<Set<ExecutableElement>> objectMethods = object
      .map(DeclaredType::asElement)
      .map(TypeElement.class::cast)
      .map(t -> t.getEnclosedElements().stream()
          .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
          .filter(e -> !e.getSimpleName().toString().startsWith("<"))
          .filter(e -> e.getModifiers().contains(Modifier.PUBLIC) || e.getModifiers().contains(Modifier.PROTECTED))
          .collect(HashSet.collector()));
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

  private final Lazy<DeclaredType> throwable = lazyDeclaredType(Throwable.class);
  public DeclaredType throwable() {
    return throwable.get();
  }

  private final Lazy<DeclaredType> error = lazyDeclaredType(Error.class);
  public DeclaredType error() {
    return error.get();
  }

  private final Lazy<DeclaredType> runtimeException = lazyDeclaredType(RuntimeException.class);
  public DeclaredType runtimeException() {
    return runtimeException.get();
  }

  private final Lazy<Option<DeclaredType>> javaxGenerated = optionalDeclaredType(Generated.class);
  public Option<DeclaredType> javaxGenerated() {
    return javaxGenerated.get();
  }

  private final Lazy<DeclaredType> iterable = lazyDeclaredType(Iterable.class);
  public DeclaredType iterable() {
    return iterable.get();
  }

  private final Lazy<ExecutableElement> iterableIterator = noParamMethod(iterable, "iterator");
  public ExecutableElement iterableIterator() {
    return iterableIterator.get();
  }

  private final Lazy<DeclaredType> iterator = lazyDeclaredType(Iterator.class);
  public DeclaredType iterator() {
    return iterator.get();
  }

  private final Lazy<ExecutableElement> iteratorNext = noParamMethod(iterator, "next");
  public ExecutableElement iteratorNext() {
    return iteratorNext.get();
  }

  private final Lazy<DeclaredType> collection = lazyDeclaredType(Collection.class);
  public DeclaredType collection() {
    return collection.get();
  }

  private final Lazy<DeclaredType> list = lazyDeclaredType(List.class);
  public DeclaredType list() {
    return list.get();
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

  private final Lazy<Option<DeclaredType>> config = optionalDeclaredType(CONFIG_TYPE);
  public Option<DeclaredType> config() {
    return config.get();
  }

  private final Lazy<Option<DeclaredType>> configMemorySize = optionalDeclaredType(CONFIG_MEMORY_SIZE_TYPE);
  public Option<DeclaredType> configMemorySize() {
    return configMemorySize.get();
  }

  private final Lazy<DeclaredType> rtShared = lazyDeclaredType($Shared.class);
  public DeclaredType rtShared() {
    return rtShared.get();
  }

  public DeclaredType rtProvision(TypeMirror componentType) {
    return Elements.asDeclaredType(env.types().getDeclaredType(
        Elements.asTypeElement(rtShared.get().asElement()), componentType));
  }

  private final Lazy<DeclaredType> configuration = lazyDeclaredType(Configuration.class);
  public DeclaredType configuration() {
    return configuration.get();
  }

  private final Lazy<ExecutableElement> configurationShellName = noParamMethod(configuration, ConfigurationConfig.SHELL_NAME);
  public ExecutableElement configurationShellName() {
    return configurationShellName.get();
  }

  private final Lazy<ExecutableElement> configurationMount = noParamMethod(configuration, ConfigurationConfig.MOUNT);
  public ExecutableElement configurationMount() {
    return configurationMount.get();
  }

  private final Lazy<ExecutableElement> configurationPackageLocal = noParamMethod(configuration, ConfigurationConfig.PACKAGE_LOCAL);
  public ExecutableElement configurationPackageLocal() {
    return configurationPackageLocal.get();
  }

  private final Lazy<DeclaredType> setup = lazyDeclaredType(Setup.class);
  public DeclaredType setup() {
    return setup.get();
  }

  private final Lazy<DeclaredType> parameter = lazyDeclaredType(Parameter.class);
  public DeclaredType parameter() {
    return parameter.get();
  }

  private final Lazy<ExecutableElement> parameterValue = noParamMethod(parameter, ParameterConfig.VALUE);
  public ExecutableElement parameterValue() {
    return parameterValue.get();
  }

  private final Lazy<ExecutableElement> parameterAbsolute = noParamMethod(parameter, ParameterConfig.ABSOLUTE);
  public ExecutableElement parameterAbsolute() {
    return parameterAbsolute.get();
  }

  private final Lazy<DeclaredType> parameterPrefix = lazyDeclaredType(Parameter.Prefix.class);
  public DeclaredType parameterPrefix() {
    return parameterPrefix.get();
  }

  private final Lazy<ExecutableElement> parameterPrefixValue = noParamMethod(parameterPrefix, ParameterPrefixConfig.VALUE);
  public ExecutableElement parameterPrefixValue() {
    return parameterPrefixValue.get();
  }

  private final Lazy<DeclaredType> extensionPointAcceptor = lazyDeclaredType(ExtensionPoint.Acceptor.class);
  public DeclaredType extensionPointAcceptor() {
    return extensionPointAcceptor.get();
  }

  private final Lazy<DeclaredType> extensionPoint = lazyDeclaredType(ExtensionPoint.class);
  public DeclaredType extensionPoint() {
    return extensionPoint.get();
  }

  private final Lazy<DeclaredType> feature = lazyDeclaredType(Feature.class);
  public DeclaredType feature() {
    return feature.get();
  }

  private final Lazy<DeclaredType> featureMount = lazyDeclaredType(Feature.Mount.class);
  public DeclaredType featureMount() {
    return featureMount.get();
  }

  private final Lazy<ExecutableElement> featureMountInjected = noParamMethod(featureMount, MountConfig.INJECTED);
  public ExecutableElement featureMountInjected() {
    return featureMountInjected.get();
  }

  private final Lazy<DeclaredType> provision = lazyDeclaredType(Provision.class);
  public DeclaredType provision() {
    return provision.get();
  }

  private final Lazy<ExecutableElement> provisionShared = noParamMethod(provision, ProvisionConfig.SHARED);
  public ExecutableElement provisionShared() {
    return provisionShared.get();
  }

  private final Lazy<ExecutableElement> provisionOverride = noParamMethod(provision, ProvisionConfig.OVERRIDE);
  public ExecutableElement provisionOverride() {
    return provisionOverride.get();
  }

  private Lazy<Option<DeclaredType>> optionalDeclaredType(ClassName className) {
    return Lazy.of(() -> Option(
        (DeclaredType) env.elements().getTypeElement(className.toString()).asType()));
  }

  private Lazy<Option<DeclaredType>> optionalDeclaredType(Class<?> clazz) {
    return Lazy.of(() -> Option(
        (DeclaredType) env.elements().getTypeElement(clazz.getCanonicalName()).asType()));
  }

  private Lazy<DeclaredType> lazyDeclaredType(Class<?> type) {
    return Lazy.of(() -> (DeclaredType) requireNonNull(
            env.elements().getTypeElement(type.getCanonicalName()), "Type " + type.getName()).asType());
  }

  private Lazy<ExecutableElement> noParamMethod(Lazy<DeclaredType> type, String name) {
    return type.map(DeclaredType::asElement)
        .map(e -> e.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
            .filter(m -> m.getParameters().isEmpty())
            .filter(m -> m.getSimpleName().toString().equals(name))
            .findAny()
            .orElseThrow(() -> new IllegalStateException(type.get() + "::" + name + "() not found")));
  }

}
