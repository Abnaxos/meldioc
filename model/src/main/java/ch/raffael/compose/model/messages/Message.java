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

import ch.raffael.compose.Assembly;
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.model.Element;
import io.vavr.collection.Seq;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public interface Message<S, T> {

  Element<S, T> element();

  String message();

  Seq<Element<S, T>> messageArgs();

  boolean languageError();

  static <S, T> SimpleMessage<S, T> missingComposeClassAnnotation(Element<S, T> element) {
    return SimpleMessage.of(element, "Class should be annotated with @"
        + Module.class.getSimpleName() + " or @" + Assembly.class.getSimpleName());
  }

  static <S, T> SimpleMessage<S, T> multipleCompositionRoles(Element<S, T> element) {
    return SimpleMessage.of(element, "Multiple compose annotations");
  }

  static <S, T> SimpleMessage<S, T> objectOverride(Element<S, T> element) {
    return SimpleMessage.of(element, "Standard Object methods cannot be used for composition");
  }

  static <S, T> SimpleMessage<S, T> nonOverridableMethod(Element<S, T> element) {
    return SimpleMessage.of(element, "Composition methods cannot be final, native or static or private");
  }

  static <S, T> SimpleMessage<S, T> conflictingCompositionRoles(Element<S, T> element, Element<S, T> conflict) {
    return SimpleMessage.of(element, "Composition role conflicts with $1")
        .withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> provisionOverrideMissing(Element<S, T> element, Element<S, T> conflict) {
    return SimpleMessage.of(element,
        "Non-shared provision overriding shared provision must specify override=true (overrides $1)")
        .withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> methodNotAccessible(Element<S, T> element, Element<S, T> conflict) {
    return SimpleMessage.of(element,
        "Method not accessible to $1").withMessageArgs(Seq(conflict));
  }

  static <S, T> SimpleMessage<S, T> parametersNotAllowed(Element<S, T> element) {
    return SimpleMessage.of(element, "Method must not take any parameters");
  }

  static <S, T> SimpleMessage<S, T> mustReturnReference(Element<S, T> element) {
    return SimpleMessage.of(element, "Method must return a reference type");
  }

  static <S, T> SimpleMessage<S, T> mountMustBeAbstract(Element<S, T> element) {
    return SimpleMessage.of(element, "Mount methods must be abstract");
  }

  static <S, T> SimpleMessage<S, T> mountRequiresAssembly(Element<S, T> element) {
    return SimpleMessage.of(element, "Mount methods are allowed in assemblies only");
  }

  static <S, T> SimpleMessage<S, T> mustReturnModule(Element<S, T> element, Element<S, T> conflict) {
    return SimpleMessage.of(element, "Mount methods must return a module or assembly")
        .withMessageArgs(Seq(conflict));
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> shouldNotReturnModule(Element<S, T> element, Element<S, T> conflict) {
    return SimpleMessage.of(element, "Method should not return a module or assembly")
        .withMessageArgs(Seq(conflict));
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> extensionPointApiReturnRecommended(Element<S, T> element, Element<S, T> conflict) {
    return SimpleMessage.of(element,
        "Extension point provisions should return a type annotated with @"
        + ExtensionPoint.class.getSimpleName() + "." + ExtensionPoint.Api.class.getSimpleName())
        .withMessageArgs(Seq(conflict));
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> returnValueIgnored(Element<S, T> element) {
    return SimpleMessage.of(element, "Return value ignored");
  }

  @_Warning
  static <S, T> SimpleMessage<S, T> configValueOutsideModule(Element<S, T> element) {
    return SimpleMessage.of(element,
        "@" + Configuration.class.getSimpleName() + " only supported in modules or assemblies");
  }

  static <S, T> SimpleMessage<S, T> configTypeNotSupported(Element<S, T> element) {
    return SimpleMessage.of(element, "Type not supported for configuration");
  }

  @Retention(RetentionPolicy.SOURCE)
  @interface _Warning {
  }

}
