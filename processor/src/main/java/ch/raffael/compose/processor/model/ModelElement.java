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

package ch.raffael.compose.processor.model;

import ch.raffael.compose.Module;
import ch.raffael.compose.tooling.model.ModelElementConfig;
import ch.raffael.compose.util.Messages;
import org.immutables.value.Value;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import static ch.raffael.compose.tooling.util.Verified.verify;

/**
 * @since 2019-03-24
 */
public abstract class ModelElement<E extends Element, C extends ModelElementConfig> {

//  public static final Seq<Class<? extends Annotation>> MODEL_ANNOTATIONS =
//      Seq(Provision.class, ExtensionPoint.class, Configuration.class, Compose.class,
//          Mount.class, );
//
//  public static <E extends Element> Optional<ModelElement<? extends E, ? extends Annotation>>
//  of(Environment env, E element) {
//    ModelElement<? extends E, ? extends Annotation>
//    if (env.)
//  }

  public abstract E element();

  public abstract C config();

  public String className() {
    return "$" + Messages.capitalize(contractNestedName(config().type().annotationType()))
        + "_" + Messages.capitalize(element().getSimpleName());
  }

  public String memberName() {
    return "$" + Messages.uncapitalize(contractNestedName(config().type().annotationType()))
        + "_" + element().getSimpleName();
  }

  private String contractNestedName(Class<?> type) {
    // we make an exception for Mount
    if (type.equals(Module.Mount.class)) {
      return Module.Mount.class.getSimpleName();
    }
    if (type.getEnclosingClass() != null) {
      return contractNestedName(type.getEnclosingClass()) + type.getSimpleName();
    } else {
      return type.getSimpleName();
    }
  }

  @Deprecated // problematic because of erasure
  public abstract TypeElement typeElement();

  public static abstract class OfExecutable<C extends ModelElementConfig> extends ModelElement<ExecutableElement, C> {
    @Override
    @Value.Lazy
    public TypeElement typeElement() {
      return verify(element().getReturnType())
          .instanceOf(DeclaredType.class)
          .map(DeclaredType::asElement)
          .instanceOf(TypeElement.class)
          .get();
    }
  }

  public static abstract class OfVariable<C extends ModelElementConfig> extends ModelElement<VariableElement, C> {
    @Override
    @Value.Lazy
    public TypeElement typeElement() {
      return verify(element().asType())
          .instanceOf(DeclaredType.class)
          .map(DeclaredType::asElement)
          .instanceOf(TypeElement.class)
          .get();
    }
  }

  public static abstract class OfType<C extends ModelElementConfig> extends ModelElement<TypeElement, C> {
    @Override
    public TypeElement typeElement() {
      return element();
    }
  }

}
