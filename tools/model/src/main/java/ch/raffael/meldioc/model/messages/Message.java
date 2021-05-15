/*
 *  Copyright (c) 2021 Raffael Herzog
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

package ch.raffael.meldioc.model.messages;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.util.Strings;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Represents a model error or warning message.
 */
public interface Message<S, T> {

  Map<String, Function<? super SrcElement<?, ?>, String>> RENDER_ATTRIBUTE_EXTRACTORS = HashMap.of(
      "name", SrcElement::name,
      "kind", e -> e.kind().name().toLowerCase(),
      "Kind", e -> Strings.capitalize(e.kind().name().toLowerCase()));
  Pattern RENDER_SUBSTITUTION_RE = Pattern.compile("\\{(?<idx>\\d+)(:(?<attr>\\w+))?}");

  Option<Id> id();

  SrcElement<S, T> element();

  Seq<Message<S, T>> origins();

  String message();

  Seq<SrcElement<S, T>> conflicts();

  boolean languageError();

  default String renderMessage(Function<? super S, ? extends CharSequence> elementRenderer) {
    return defaultRenderMessage(this, elementRenderer);
  }

  static <S, T> SimpleMessage<S, T> conflictingCompositionRoles(SrcElement<S, T> element,
                                                                Seq<SrcElement<S, T>> conflicts) {
    return SimpleMessage.of(Id.ConflictingCompositionRoles, element,
        "Conflicting composition roles", conflicts);
  }

  static <S, T> SimpleMessage<S, T> conflictingOverride(SrcElement<S, T> element,
                                                        Seq<SrcElement<S, T>> conflicts) {
    return SimpleMessage.of(Id.ConflictingOverride, element,
        "Composition roles in conflict with inherited roles", conflicts);
  }

  static <S, T> SimpleMessage<S, T> objectOverride(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.ObjectOverride, element,
        "Standard Object methods cannot be used for composition");
  }

  static <S, T> SimpleMessage<S, T> nonOverridableMethod(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.NonOverridableMethod, element,
        "Composition methods cannot be final, native or static or private");
  }

  static <S, T> SimpleMessage<S, T> provisionOverrideMissing(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    return SimpleMessage.of(Id.ProvisionOverrideMissing, element,
        "Non-singleton provision overriding singleton provision must specify override=true (overrides {1})",
        conflict);
  }

  static <S, T> SimpleMessage<S, T> unresolvedProvision(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    return SimpleMessage.of(Id.UnresolvedProvision, element,
        "Unresolved provision '{1:name}'", conflict);
  }

  static <S, T> SimpleMessage<S, T> conflictingProvisions(SrcElement<S, T> element, Seq<SrcElement<S, T>> conflicts) {
    return SimpleMessage.of(Id.ConflictingProvisions, element,
        "Conflicting provisions for '{1:name}'", conflicts);
  }

  static <S, T> SimpleMessage<S, T> elementNotAccessible(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    if (!element.equals(conflict)) {
      return SimpleMessage.of(Id.ElementNotAccessible, element,
          "{1:Kind} {1:name} not accessible", conflict);
    } else {
      return SimpleMessage.of(Id.ElementNotAccessible, element,
          "{1:Kind} not accessible", conflict);
    }
  }

  static <S, T> SimpleMessage<S, T> abstractMethodWillNotBeImplemented(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    return SimpleMessage.of(Id.AbstractMethodWillNotBeImplemented, element,
        "Abstract method {1} will not be implemented by the configuration", conflict);
  }

  static <S, T> SimpleMessage<S, T> noParametersAllowed(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.NoParametersAllowed, element,
        "Method must not take any parameters");
  }

  static <S, T> SimpleMessage<S, T> mustReturnReference(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.MustReturnReference, element,
        "Method must return a reference type");
  }

  static <S, T> SimpleMessage<S, T> mountMethodMustBeAbstract(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.MountMethodMustBeAbstract, element,
        "Mount methods must be abstract");
  }

  static <S, T> SimpleMessage<S, T> mountMethodsAllowedInConfigurationsOnly(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.MountMethodsAllowedInConfigurationsOnly, element,
        "Mount methods are allowed in configurations only");
  }

  static <S, T> SimpleMessage<S, T> mountMethodMustReturnFeature(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    return SimpleMessage.of(Id.MountMethodMustReturnFeature, element,
        "Mount methods must return a @" + Feature.class.getSimpleName() + " or @" + Configuration.class.getSimpleName(),
        conflict);
  }

  static <S, T> SimpleMessage<S, T> mountAttributeClassMustNotBeParametrized(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    return SimpleMessage.of(Id.MountAttributeClassMustNotBeParametrized, element,
        "Mounted class '{1:name}' must not be parametrized", conflict);
  }

  static <S, T> SimpleMessage<S, T> mountedAbstractProvisionHasNoImplementationCandidate(
      SrcElement<S, T> mountMethod, SrcElement<S, T> provisionMethod)
  {
    return SimpleMessage.of(Id.MountedAbstractProvisionHasNoImplementationCandidate, mountMethod,
        "Mounted abstract provision '{1:name}' has no implementation candidate", provisionMethod);
  }

  static <S, T> SimpleMessage<S, T> missingNoArgsConstructor(SrcElement<S, T> type) {
    return missingNoArgsConstructor(type, type);
  }

  static <S, T> SimpleMessage<S, T> missingNoArgsConstructor(SrcElement<S, T> elem, SrcElement<S, T> type) {
    if (elem.kind() == SrcElement.Kind.METHOD) {
      return SimpleMessage.of(Id.MissingNoArgsConstructor, elem,
          "Illegal return type: class has no accessible no-arg constructor", type);
    } else {
      return SimpleMessage.of(Id.MissingNoArgsConstructor, elem,
          "Class has no accessible no-arg constructor", type);
    }
  }

  static <S, T> SimpleMessage<S, T> illegalInnerClass(SrcElement<S, T> type) {
    return illegalInnerClass(type, type);
  }

  static <S, T> SimpleMessage<S, T> illegalInnerClass(SrcElement<S, T> elem, SrcElement<S, T> type) {
    if (elem.kind() == SrcElement.Kind.METHOD) {
      return SimpleMessage.of(Id.IllegalInnerClass, elem,
          "Illegal return type: features and configurations must be top-level or nested classes");
    } else {
      return SimpleMessage.of(Id.IllegalInnerClass, elem,
          "Illegal inner class: features and configurations must be top-level or nested classes");
    }
  }

  static <S, T> SimpleMessage<S, T> extensionPointAcceptorReturnRecommended(SrcElement<S, T> element, SrcElement<S, T> conflict) {
    return SimpleMessage.of(Id.ExtensionPointAcceptorReturnRecommended, element,
        "Extension point provisions should return a type annotated with @"
            + ExtensionPoint.class.getSimpleName() + "." + ExtensionPoint.Acceptor.class.getSimpleName(),
        conflict);
  }

  static <S, T> SimpleMessage<S, T> returnValueIgnored(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.ReturnValueIgnored, element,
        "Return value ignored");
  }

  static <S, T> SimpleMessage<S, T> meldAnnotationOutsideFeature(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.MeldAnnotationOutsideFeature, element,
        "Meld annotation outside feature");
  }

  static <S, T> SimpleMessage<S, T> typesafeConfigNotOnClasspath(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.TypesafeConfigNotOnClasspath, element,
        "Typesafe config is not on classpath, configuration not supported");
  }

  static <S, T> SimpleMessage<S, T> configTypeNotSupported(SrcElement<S, T> element) {
    return SimpleMessage.of(Id.ConfigTypeNotSupported, element,
        "Type not supported for configuration");
  }

  static <S, T> SimpleMessage<S, T> unresolvedExtensionPoint(SrcElement<S, T> epConsumer, SrcElement<S, T> epType) {
    return SimpleMessage.of(Id.UnresolvedExtensionPoint, epConsumer,
        "Unresolved extension point {1}", epType);
  }

  static <S, T> SimpleMessage<S, T> unresolvedExtensionPoint(SrcElement<S, T> source,
                                                             SrcElement<S, T> epConsumer, SrcElement<S, T> epType) {
    SimpleMessage<S, T> origin = unresolvedExtensionPoint(epConsumer, epType);
    origin = origin.withMessage(origin.message());
    return unresolvedExtensionPoint(source, epType).withOrigins(List.of(origin));
  }

  static <S, T> SimpleMessage<S, T> conflictingExtensionPoints(SrcElement<S, T> element, Seq<SrcElement<S, T>> conflicts) {
    return SimpleMessage.of(Id.ConflictingExtensionPoints, element,
        "Conflicting extension points", conflicts);
  }

  static <S, T> SimpleMessage<S, T> incompatibleThrowsClause(SrcElement<S, T> element, SrcElement<S, T> provision,
                                                             SrcElement<S, T> exception) {
    // TODO (2020-12-17) duplication here for message rendering, extend message rendering accordingly
    return SimpleMessage.of(Id.IncompatibleThrowsClause, element,
        "Incompatible throws clause for provision {1}, implementing method throws {2}",
        provision, exception);
  }

  static <S, T> SimpleMessage<S, T> missingFeatureImportAnnotation(SrcElement<S, T> element, SrcElement<S, T> superType) {
    // TODO (2020-12-19) 0.2: redact error message
    return SimpleMessage.of(Id.MissingFeatureImportAnnotation, element,
        "Class extends/implements non-feature class {1} without `@Feature.Import` annotation"
            + "\nTHIS WILL BE AN ERROR IN THE NEXT MAJOR RELEASE", superType);
  }

  static <S, T> String defaultRenderMessage(
      Message<S, T> msg, Function<? super S, ? extends CharSequence> elementRenderer) {
    Seq<SrcElement<S, T>> args = msg.conflicts().prepend(msg.element());
    StringBuilder result = new StringBuilder();
    Matcher matcher = RENDER_SUBSTITUTION_RE.matcher(msg.message());
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group("idx"));
      Option<SrcElement<S, T>> element = index < args.length() ? some(args.get(index)) : none();
      String attr = matcher.group("attr");
      if (attr == null) {
        matcher.appendReplacement(result, element
                .map(SrcElement::source)
                .map(elementRenderer)
                .map(Object::toString)
                .getOrElse("<?" + matcher.group() + ">"));
      } else {

        matcher.appendReplacement(result, element
            .flatMap(e -> RENDER_ATTRIBUTE_EXTRACTORS.get(attr).map(f -> f.apply(e)))
            .getOrElse(() -> "<?" + matcher.group() + ">"));
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Message IDs for use with `@SuppressWarnings()`.
   *
   * <p>TODO (2019-04-19) @SuppressWarnings not supported yet
   */
  enum Id {
    ConflictingCompositionRoles,
    ConflictingOverride,
    ObjectOverride,
    NonOverridableMethod,
    ProvisionOverrideMissing,
    UnresolvedProvision,
    ConflictingProvisions,
    ElementNotAccessible,
    AbstractMethodWillNotBeImplemented,
    NoParametersAllowed,
    MustReturnReference,
    MountMethodMustBeAbstract,
    MountMethodsAllowedInConfigurationsOnly,
    MountMethodMustReturnFeature,
    MountAttributeClassMustNotBeParametrized,
    MountedAbstractProvisionHasNoImplementationCandidate,
    MissingNoArgsConstructor,
    IllegalInnerClass,
    TypesafeConfigNotOnClasspath,
    ConfigTypeNotSupported,
    UnresolvedExtensionPoint,
    ConflictingExtensionPoints,
    IncompatibleThrowsClause,
    MissingFeatureImportAnnotation(true), // TODO (2020-12-19) 0.2: make this an error

    // Warnings
    ExtensionPointAcceptorReturnRecommended(true),
    ReturnValueIgnored(true),
    MeldAnnotationOutsideFeature(true);

    public static final String ID_PREFIX = "meld.";
    private final String id;
    private final boolean warning;

    Id() {
      this(false);
    }

    Id(boolean warning) {
      this.id = ID_PREFIX + name();
      this.warning = warning;
    }

    public String id() {
      return id;
    }

    public boolean warning() {
      return warning;
    }
  }
}
