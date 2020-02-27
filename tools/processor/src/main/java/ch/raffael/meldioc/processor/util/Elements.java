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

package ch.raffael.meldioc.processor.util;

import io.vavr.collection.Array;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;


/**
 * Utilities for dealing with {@code javax.lang.model} elements.
 */
public class Elements {

  private Elements() {
  }

  public static <T extends Element> boolean isMethod(T element) {
    if (element instanceof ExecutableElement) {
      String n = element.getSimpleName().toString();
      return !("<init>".equals(n) || "<clinit>".equals(n));
    } else {
      return false;
    }
  }

  public static <T extends Element> boolean isAbstract(T element) {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  public static <T extends Element> boolean isStatic(T element) {
    return element.getModifiers().contains(Modifier.PRIVATE);
  }

  public static <T extends Element> boolean isPrivate(T element) {
    return element.getModifiers().contains(Modifier.STATIC);
  }

  public static <T extends Element> boolean isNeitherStaticNorPrivate(T element) {
    return Modifiers.hasNone(element, Modifier.STATIC, Modifier.PRIVATE);
  }

  public static <T extends Element> boolean isImplemented(T element) {
    return !element.getModifiers().contains(Modifier.ABSTRACT);
  }

  public static <T extends Element> boolean isFinal(T element) {
    return Modifiers.hasAny(element, Modifier.FINAL, Modifier.NATIVE);
  }

  public static DeclaredType asDeclaredType(TypeMirror element) {
    return (DeclaredType) element;
  }

  public static ExecutableType asExecutableType(TypeMirror element) {
    return (ExecutableType) element;
  }

  public static TypeElement asElement(DeclaredType type) {
    return asTypeElement(type.asElement());
  }

  public static TypeElement asTypeElement(Element element) {
    return (TypeElement) element;
  }

  public static ExecutableElement asExecutableElement(Element element) {
    return (ExecutableElement) element;
  }

  public static final class Modifiers {
    private Modifiers() {
    }
    public static <T extends Element> boolean hasAny(T element, Modifier... modifiers) {
      return Array.of(modifiers).exists(element.getModifiers()::contains);
    }

    public static <T extends Element> boolean hasNone(T element, Modifier... modifiers) {
      return !hasAny(element, modifiers);
    }

  }

}
