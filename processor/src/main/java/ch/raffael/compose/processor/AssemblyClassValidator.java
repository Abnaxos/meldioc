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

package ch.raffael.compose.processor;

import ch.raffael.compose.processor.env.Environment;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * TODO javadoc
 */
class AssemblyClassValidator extends Environment.WithEnv {

  AssemblyClassValidator(Environment env) {
    super(env);
  }

  void validateAll(TypeElement type, String targetPackageName) {
    validateNoTypeParameters(type);
    validateCompilantConstructor(type, targetPackageName);
  }

  private void validateNoTypeParameters(TypeElement type) {
    if (!type.getTypeParameters().isEmpty()) {
      env.problems().error(type, "Assembly cannot have type parameters");
    }
  }

  private void validateCompilantConstructor(TypeElement type, String targetPackageName) {
    type.getEnclosedElements().stream()
        .filter(ExecutableElement.class::isInstance).map(ExecutableElement.class::cast)
        .filter(e -> e.getParameters().isEmpty())
        .filter(e -> "<init>".equals(e.getSimpleName().toString()))
        .filter(c -> {
          var mods = c.getModifiers();
          if (mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.PROTECTED)) {
            return true;
          } else if (mods.contains(Modifier.PRIVATE)) {
            return false;
          } else {
            return targetPackageName.equals(env.elements().getPackageOf(type).getQualifiedName().toString());
          }
        })
        .findFirst()
        .ifPresentOrElse(c -> {}, () ->
            env.problems().error(type, "No accessible no-arguments constructor"));
  }


}
