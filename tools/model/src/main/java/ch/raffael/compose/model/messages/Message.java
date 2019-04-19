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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.*;

/**
 * Represents a model error or warning message.
 */
public interface Message<S, T> {

  CElement<S, T> element();

  String message();

  Seq<CElement<S, T>> messageArgs();

  boolean languageError();

  default String renderMessage(Function<? super S, ? extends CharSequence> elementRenderer) {
    return defaultRenderMessage(this, elementRenderer);
  }

  static <S, T> SimpleMessage<S, T> missingComposeClassAnnotation(CElement<S, T> element) {
    return SimpleMessage.of(element, "Class should be annotated with @"
        + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName());
  }

  static <S, T> SimpleMessage<S, T> conflictingComposeAnnotations(CElement<S, T> element) {
    return SimpleMessage.of(element, "Multiple compose annotations");
  }

  static <S, T> SimpleMessage<S, T> objectOverride(CElement<S, T> element) {
    return SimpleMessage.of(element, "Standard Object methods cannot be used for composition");
  }

  static <S, T> SimpleMessage<S, T> nonOverridableMethod(CElement<S, T> element) {
    return SimpleMessage.of(element, "Composition methods cannot be final, native or static or private");
  }

  static <S, T> SimpleMessage<S, T> conflictingCompositionRoles(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(element, "Composition role conflicts with $1")
        .withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> provisionOverrideMissing(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(element,
        "Non-shared provision overriding shared provision must specify override=true (overrides $1)")
        .withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> noImplementationCandidate(CElement<S, T> element, CElement<S, T> conflict) {
    // TODO FIXME (2019-04-14) fix error message
    return SimpleMessage.of(element,
        "No implementation candidate found for method $1")
        .withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> multipleImplementationCandidates(CElement<S, T> element, Seq<CElement<S, T>> conflicts) {
    return SimpleMessage.of(element,
        "Multiple implementation candidates found")
        .withMessageArgs(conflicts);
  }

  static <S, T> SimpleMessage<S, T> methodNotAccessible(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(element,
        "Method not accessible to $1").withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> parametersNotAllowed(CElement<S, T> element) {
    return SimpleMessage.of(element, "Method must not take any parameters");
  }

  static <S, T> SimpleMessage<S, T> mustReturnReference(CElement<S, T> element) {
    return SimpleMessage.of(element, "Method must return a reference type");
  }

  static <S, T> SimpleMessage<S, T> mountMustBeAbstract(CElement<S, T> element) {
    return SimpleMessage.of(element, "Mount methods must be abstract");
  }

  static <S, T> SimpleMessage<S, T> mountRequiresConfiguration(CElement<S, T> element) {
    return SimpleMessage.of(element, "Mount methods are allowed in configurations only");
  }

  static <S, T> SimpleMessage<S, T> mustReturnModule(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(element, "Mount methods must return a @"
        + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName())
        .withMessageArgs(Seq(conflict));
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> shouldNotReturnModule(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(element, "Method should not return a @"
        + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName())
        .withMessageArgs(Seq(conflict));
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> extensionPointApiReturnRecommended(CElement<S, T> element, CElement<S, T> conflict) {
    return SimpleMessage.of(element,
        "Extension point provisions should return a type annotated with @"
            + ExtensionPoint.class.getSimpleName() + "." + ExtensionPoint.Api.class.getSimpleName())
        .withMessageArgs(Seq(conflict));
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> returnValueIgnored(CElement<S, T> element) {
    return SimpleMessage.of(element, "Return value ignored");
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> configValueOutsideModule(CElement<S, T> element) {
    return SimpleMessage.of(element, "@" + Parameter.class.getSimpleName() + " only supported in @"
        + Module.class.getSimpleName() + " or @" + Configuration.class.getSimpleName());
  }

  static <S, T> SimpleMessage<S, T> typesafeConfigNotOnClasspath(CElement<S, T> element) {
    return SimpleMessage.of(element, "Typesafe config is not on classpath, configuration not supported");
  }

  static <S, T> SimpleMessage<S, T> configTypeNotSupported(CElement<S, T> element) {
    return SimpleMessage.of(element, "Type not supported for configuration");
  }

  static <S, T> Message<S, T> noMatchingExtensionPointProvision(CElement<S, T> element) {
    return SimpleMessage.of(element, "No matching extension point provision found");
  }

  static <S, T> Message<S, T> ambiguousExtensionPointProvisions(CElement<S, T> element, Seq<CElement<S, T>> conflicts) {
    return SimpleMessage.of(element, "Multiple matching extension point provisions found")
        .withMessageArgs(conflicts);
  }

  @Retention(RetentionPolicy.SOURCE)
  @interface _Warning {
  }

  static <S, T> String defaultRenderMessage(
      Message<S, T> msg, Function<? super S, ? extends CharSequence> elementRenderer) {
    var args = msg.messageArgs().prepend(msg.element());
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

}
