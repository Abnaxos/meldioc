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

import ch.raffael.compose.tooling.model.ClassRef;

import javax.lang.model.element.TypeElement;

/**
 * @since 2019-03-26
 */
public class Adaptors extends Environment.WithEnv {

  Adaptors(Environment env) {
    super(env);
  }

  public ClassRef classRef(TypeElement typeElement) {
    return ClassRef.of(
        env.elements().getPackageOf(typeElement).getQualifiedName().toString(),
        qualifiedInnerClassName(typeElement, new StringBuilder()).toString());
  }

  private StringBuilder qualifiedInnerClassName(TypeElement typeElement, StringBuilder buf) {
    var enclosing = typeElement.getEnclosingElement();
    if (!(enclosing instanceof TypeElement)) {
      buf.setLength(0);
      return buf.append(typeElement.getSimpleName());
    } else {
      return buf.append('.').append(typeElement.getSimpleName());
    }
  }

}
