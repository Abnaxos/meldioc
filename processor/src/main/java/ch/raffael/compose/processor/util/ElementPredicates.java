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

package ch.raffael.compose.processor.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import java.util.stream.Stream;

import static ch.raffael.compose.util.fun.Fun.let;

/**
 * @since 2019-03-24
 */
public class ElementPredicates {

  private ElementPredicates() {
  }

  public static <T extends Element> boolean isMethod(T element) {
    if (element instanceof ExecutableElement) {
      String n = element.getSimpleName().toString();
      return !("<init>".equals(n) || "<clinit>".equals(n));
    } else {
      return false;
    }
  }

//  public <T extends Element> Predicate<T> visiblePredicate(String packageName, boolean includeProtected) {
//    return (element) -> {
//      var modifiers = element.getModifiers();
//      if (modifiers.contains(Modifier.PUBLIC)) {
//        return true;
//      }
//      if (modifiers.contains(Modifier.PRIVATE)) {
//        return false;
//      }
//      if (packageName.equals(env.elements().getPackageOf(element).getQualifiedName().toString())) {
//        return true;
//      }
//      return includeProtected && modifiers.contains(Modifier.PROTECTED);
//    };
//  }

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

  public static final class Modifiers {
    private Modifiers() {
    }
    public static <T extends Element> boolean hasAny(T element, Modifier... modifiers) {
      return let(element.getModifiers(), m -> Stream.of(modifiers).anyMatch(m::contains));
    }
    public static <T extends Element> boolean hasNone(T element, Modifier... modifiers) {
      return let(element.getModifiers(), m -> Stream.of(modifiers).noneMatch(m::contains));
    }
  }


}
