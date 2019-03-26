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

import ch.raffael.compose.processor.env.Environment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ch.raffael.compose.tooling.util.Verified.verifierOf;

/**
 * @since 2019-03-24
 */
public final class Verifiers {

  private final Environment env;

  Verifiers(Environment env) {
    this.env = env;
  }

  public Consumer<TypeMirror> subtypeOf(Supplier<? extends TypeMirror> superType) {
    return verifierOf(t -> env.types().isSubtype(t, superType.get()),
        t -> t + "is not a subtype of " + superType.get());
  }

  public Consumer<AnnotationMirror> mirrorOf(Supplier<? extends DeclaredType> annotationType) {
    return verifierOf(m -> m.getAnnotationType().equals(annotationType.get()), m -> m + " is not a " + annotationType.get());
  }

}
