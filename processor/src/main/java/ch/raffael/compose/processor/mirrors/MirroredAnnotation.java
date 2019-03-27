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

package ch.raffael.compose.processor.mirrors;

import ch.raffael.compose.processor.InternalErrorException;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.tooling.model.AssemblyConfig;
import ch.raffael.compose.tooling.model.ClassRef;
import ch.raffael.compose.tooling.model.ModelElementConfig;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static ch.raffael.compose.tooling.util.Verified.verify;

/**
 * @since 2019-03-23
 */
public abstract class MirroredAnnotation {

  private MirroredAnnotation() {
  }

  public static AssemblyConfig<AnnotationMirror> assembly(Environment env, AnnotationMirror mirror) throws InternalErrorException {
    return AssemblyConfig.<AnnotationMirror>builder()
        .source(mirror)
        .className(attributeValue(env, mirror, env.known().assemblyClassName(), String.class))
        .packageLocal(attributeValue(env, mirror, env.known().assemblyPackageLocal(), Boolean.class))
        .build();
  }

  public static <T extends ModelElementConfig<AnnotationMirror>> Optional<T> find(
      Environment env,
      Element element,
      BiFunction<Environment, AnnotationMirror, T> converter) {
    return element.getAnnotationMirrors().stream()
        .filter(m -> m.getAnnotationType().equals(env.known().assembly()))
        .findFirst()
        .map(m -> converter.apply(env, m));
  }

  private static <T> T attributeValue(Environment env,
                                      AnnotationMirror mirror,
                                      ExecutableElement attribute,
                                      Class<T> type) {
    return attributeValue(env, env.elements().getElementValuesWithDefaults(mirror), attribute, type);
  }

  private static <T> T attributeValue(Environment env,
                                      Map<? extends ExecutableElement, ? extends AnnotationValue> allValues,
                                      ExecutableElement attribute,
                                      Class<T> expectedType) {
    var value = verify(attribute)
        .map(allValues::get)
        .nonnull("No " + attribute.getSimpleName() + " attribute")
        .map(AnnotationValue::getValue)
        .get();
    if (value instanceof TypeMirror) {
      int arrayDimensions = 0;
      TypeMirror type = (TypeMirror) value;
      while (type instanceof ArrayType) {
        arrayDimensions++;
        type = ((ArrayType) type).getComponentType();
      }
      ClassRef string = null;
      if (type.getKind() == TypeKind.VOID) {
        string = ClassRef.of("", "void");
      } else if (type instanceof PrimitiveType) {
        string = ClassRef.of("", type.getKind().name().toLowerCase());
      } else if (type instanceof DeclaredType) {
        var element = (TypeElement) ((DeclaredType) type).asElement();
        string = ClassRef.of(env.elements().getPackageOf(element).getQualifiedName().toString(),
            getClassName(element, new StringBuilder()).toString());
      }
      if (string != null && arrayDimensions != 0) {
        string = string.withArrayDimensions(arrayDimensions);
      }
      if (string != null) {
        value = string;
      }
    }
    return verify(value).instanceOf(expectedType).get();
  }

  private static StringBuilder getClassName(TypeElement element, StringBuilder buf) {
    Element enclosingElement = element.getEnclosingElement();
    if (!(enclosingElement instanceof TypeElement)) {
      return buf.append(element.getSimpleName());
    } else {
      getClassName((TypeElement) enclosingElement, buf);
      return buf.append('.').append(element.getSimpleName());
    }
  }

}
