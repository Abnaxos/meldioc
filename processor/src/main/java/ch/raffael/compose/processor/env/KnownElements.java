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
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.processor.util.Elements;
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
 * TODO javadoc
 */
public class KnownElements extends Environment.WithEnv {

  public static final String CONFIG_PACKAGE = "com.typesafe.config";
  public static final ClassName CONFIG_TYPE = ClassName.get(CONFIG_PACKAGE, "Config");
  public static final ClassName CONFIG_MEMORY_SIZE_TYPE = ClassName.get(CONFIG_PACKAGE, "ConfigMemorySize");
  public static final ClassName CONFIG_NOT_RESOLVED_EXCEPTION_TYPE =
      ClassName.get(CONFIG_PACKAGE, "ConfigException", "NotResolved");

  public static final String PROVISION_SHARED_ATTR = "shared";
  public static final String PROVISION_OVERRIDE_ATTR = "override";

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

  private final Lazy<DeclaredType> rtProvision = lazyDeclaredType($Provision.class);
  public DeclaredType rtProvision() {
    return rtProvision.get();
  }

  public DeclaredType rtProvision(TypeMirror componentType) {
    return Elements.asDeclaredType(env.types().getDeclaredType(
        Elements.asTypeElement(rtProvision.get().asElement()), componentType));
  }

  private final Lazy<DeclaredType> assembly = lazyDeclaredType(Assembly.class);
  public DeclaredType assembly() {
    return assembly.get();
  }

  private final Lazy<ExecutableElement> assemblyBuilderName = noParamMethod(assembly, "shellName");
  public ExecutableElement assemblyShellName() {
    return assemblyBuilderName.get();
  }

  private final Lazy<ExecutableElement> assemblyPackageLocal = noParamMethod(assembly, "packageLocal");
  public ExecutableElement assemblyPackageLocal() {
    return assemblyPackageLocal.get();
  }

  private final Lazy<DeclaredType> compose = lazyDeclaredType(Compose.class);
  public DeclaredType compose() {
    return compose.get();
  }

  private final Lazy<DeclaredType> configuration = lazyDeclaredType(Configuration.class);
  public DeclaredType configuration() {
    return configuration.get();
  }

  private final Lazy<ExecutableElement> configurationPath = noParamMethod(configuration, "path");
  public ExecutableElement configurationPath() {
    return configurationPath.get();
  }

  private final Lazy<ExecutableElement> configurationAbsolute = noParamMethod(configuration, "absolute");
  public ExecutableElement configurationAbsolute() {
    return configurationAbsolute.get();
  }

  private final Lazy<DeclaredType> configurationPrefix = lazyDeclaredType(Configuration.Prefix.class);
  public DeclaredType configurationPrefix() {
    return configurationPrefix.get();
  }

  private final Lazy<ExecutableElement> configurationPrefixValue = noParamMethod(configurationPrefix, "value");
  public ExecutableElement configurationPrefixValue() {
    return configurationPrefixValue.get();
  }

  private final Lazy<DeclaredType> extensionPointApi = lazyDeclaredType(ExtensionPoint.Api.class);
  public DeclaredType extensionPointApi() {
    return extensionPointApi.get();
  }

  private final Lazy<DeclaredType> extensionPointProvision = lazyDeclaredType(ExtensionPoint.Provision.class);
  public DeclaredType extensionPointProvision() {
    return extensionPointProvision.get();
  }

  private final Lazy<DeclaredType> module = lazyDeclaredType(Module.class);
  public DeclaredType module() {
    return module.get();
  }

  private final Lazy<DeclaredType> moduleMount = lazyDeclaredType(Module.Mount.class);
  public DeclaredType moduleMount() {
    return moduleMount.get();
  }

  private final Lazy<ExecutableElement> moduleMountExternal = noParamMethod(moduleMount, "external");
  public ExecutableElement moduleMountExternal() {
    return moduleMountExternal.get();
  }

  private final Lazy<DeclaredType> provision = lazyDeclaredType(Provision.class);
  public DeclaredType provision() {
    return provision.get();
  }

  private final Lazy<ExecutableElement> provisionShared = noParamMethod(provision, "shared");
  public ExecutableElement provisionShared() {
    return provisionShared.get();
  }

  private final Lazy<ExecutableElement> provisionOverride = noParamMethod(provision, "override");
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
