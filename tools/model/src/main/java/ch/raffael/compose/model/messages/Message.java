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

package ch.raffael.compose.model.messages;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.model.CElement;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a model error or warning message.
 */
public interface Message<S, T> {

  Option<Id> id();

  CElement<S, T> element();

  String message();

  Seq<CElement<S, T>> conflicts();

  boolean languageError();

  default String renderMessage(Function<? super S, ? extends CharSequence> elementRenderer) {
    return defaultRenderMessage(this, elementRenderer);
  }

  static <S, T> SimpleMessage<S, T> missingComposeClassAnnotation(CElement<S, T> element) {
    return SimpleMessage.of(Id.MissingComposeClassAnnotation, element,
        "Class should be annotated with @"
            + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName());
  }

  static <S, T> SimpleMessage<S, T> conflictingComposeAnnotations(CElement<S, T> element) {
    return SimpleMessage.of(Id.ConflictingComposeAnnotations, element,
        "Multiple compose annotations");
  }

  static <S, T> SimpleMessage<S, T> objectOverride(CElement<S, T> element) {
    return SimpleMessage.of(Id.ObjectOverride, element,
        "Standard Object methods cannot be used for composition");
  }

  static <S, T> SimpleMessage<S, T> nonOverridableMethod(CElement<S, T> element) {
    return SimpleMessage.of(Id.NonOverridableMethod, element,
        "Composition methods cannot be final, native or static or private");
  }

  static <S, T> SimpleMessage<S, T> conflictingCompositionRoles(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.ConflictingCompositionRoles, element,
        "Composition role conflicts with $1", conflict);
  }

  static <S, T> SimpleMessage<S, T> provisionOverrideMissing(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.ProvisionOverrideMissing, element,
        "Non-shared provision overriding shared provision must specify override=true (overrides $1)",
        conflict);
  }

  static <S, T> SimpleMessage<S, T> noImplementationCandidate(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.NoImplementationCandidate, element,
        "No implementation candidate found for method $1", conflict);
  }

  static <S, T> SimpleMessage<S, T> multipleImplementationCandidates(CElement<S, T> element, Seq<CElement<S, T>> conflicts) {
    return SimpleMessage.of(Id.MultipleImplementationCandidates, element,
        "Multiple implementation candidates found", conflicts);
  }

  static <S, T> SimpleMessage<S, T> methodNotAccessible(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.MethodNotAccessible, element,
        "Method not accessible to $1", conflict);
  }

  static <S, T> SimpleMessage<S, T> noParametersAllowed(CElement<S, T> element) {
    return SimpleMessage.of(Id.NoParametersAllowed, element,
        "Method must not take any parameters");
  }

  static <S, T> SimpleMessage<S, T> mustReturnReference(CElement<S, T> element) {
    return SimpleMessage.of(Id.MustReturnReference, element,
        "Method must return a reference type");
  }

  static <S, T> SimpleMessage<S, T> mountMethodMustBeAbstract(CElement<S, T> element) {
    return SimpleMessage.of(Id.MountMethodMustBeAbstract, element,
        "Mount methods must be abstract");
  }

  static <S, T> SimpleMessage<S, T> mountMethodsAllowedInConfigurationsOnly(CElement<S, T> element) {
    return SimpleMessage.of(Id.MountMethodsAllowedInConfigurationsOnly, element,
        "Mount methods are allowed in configurations only");
  }

  static <S, T> SimpleMessage<S, T> mountMethodMustReturnModule(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.MountMethodMustReturnModule, element,
        "Mount methods must return a @" + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName(),
        conflict);
  }

  static <S, T> SimpleMessage<S, T> methodShouldNotReturnModule(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.MethodShouldNotReturnModule, element,
        "Method should not return a @"
            + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName(),
        conflict);
  }

  static <S, T> SimpleMessage<S, T> extensionPointApiReturnRecommended(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(Id.ExtensionPointApiReturnRecommended, element,
        "Extension point provisions should return a type annotated with @"
            + ExtensionPoint.class.getSimpleName() + "." + ExtensionPoint.Api.class.getSimpleName(),
        conflict);
  }

  static <S, T> SimpleMessage<S, T> returnValueIgnored(CElement<S, T> element) {
    return SimpleMessage.of(Id.ReturnValueIgnored, element,
        "Return value ignored");
  }

  static <S, T> SimpleMessage<S, T> configValueOutsideModule(CElement<S, T> element) {
    return SimpleMessage.of(Id.ConfigValueOutsideModule, element,
        "@" + Parameter.class.getSimpleName() + " only supported in @"
            + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName());
  }

  static <S, T> SimpleMessage<S, T> typesafeConfigNotOnClasspath(CElement<S, T> element) {
    return SimpleMessage.of(Id.TypesafeConfigNotOnClasspath, element,
        "Typesafe config is not on classpath, configuration not supported");
  }

  static <S, T> SimpleMessage<S, T> configTypeNotSupported(CElement<S, T> element) {
    return SimpleMessage.of(Id.ConfigTypeNotSupported, element,
        "Type not supported for configuration");
  }

  static <S, T> Message<S, T> noMatchingExtensionPointProvision(CElement<S, T> element) {
    return SimpleMessage.of(Id.NoMatchingExtensionPointProvision, element,
        "No matching extension point provision found");
  }

  static <S, T> Message<S, T> multipleMatchingExtensionPointProvisions(CElement<S, T> element, Seq<CElement<S, T>> conflicts) {
    return SimpleMessage.of(Id.MultipleMatchingExtensionPointProvisions, element,
        "Multiple matching extension point provisions found", conflicts);
  }

  static <S, T> String defaultRenderMessage(
      Message<S, T> msg, Function<? super S, ? extends CharSequence> elementRenderer) {
    var args = msg.conflicts().prepend(msg.element());
    StringBuilder result = new StringBuilder();
    Matcher matcher = Pattern.compile("\\$\\d+").matcher(msg.message());
    while (matcher.find()) {
      var index = Integer.parseInt(matcher.group().substring(1));
      matcher.appendReplacement(result,
          index < 0 || index >= args.length()
              ? "<?" + matcher.group() + ">"
              : elementRenderer.apply(args.get(index).source()).toString());
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
    MissingComposeClassAnnotation,
    ConflictingComposeAnnotations,
    ObjectOverride,
    NonOverridableMethod,
    ConflictingCompositionRoles,
    ProvisionOverrideMissing,
    NoImplementationCandidate,
    MultipleImplementationCandidates,
    MethodNotAccessible,
    NoParametersAllowed,
    MustReturnReference,
    MountMethodMustBeAbstract,
    MountMethodsAllowedInConfigurationsOnly,
    MountMethodMustReturnModule,
    TypesafeConfigNotOnClasspath,
    ConfigTypeNotSupported,
    NoMatchingExtensionPointProvision,
    MultipleMatchingExtensionPointProvisions,

    // Warnings
    MethodShouldNotReturnModule,
    ExtensionPointApiReturnRecommended,
    ReturnValueIgnored,
    ConfigValueOutsideModule;

    public static final String ID_PREFIX = "compose.";
    private final String id;

    Id() {
      this.id = ID_PREFIX + name();
    }

    public String id() {
      return id;
    }

    public boolean warning() {
      return ordinal() >= MethodShouldNotReturnModule.ordinal();
    }

  }

}
