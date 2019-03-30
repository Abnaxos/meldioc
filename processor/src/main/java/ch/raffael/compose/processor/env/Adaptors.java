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
import java.util.function.Function;

import static ch.raffael.compose.tooling.util.Verified.verify;

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

  public <T extends ModelElementConfig<AnnotationMirror>> Optional<T> findConfig(Element element,
                                                                                 Function<AnnotationMirror, T> converter) {
    return element.getAnnotationMirrors().stream()
        .filter(m -> m.getAnnotationType().equals(env.known().assembly()))
        .findFirst()
        .map(converter);
  }

  public AssemblyConfig<AnnotationMirror> assemblyConfigOf(AnnotationMirror mirror) {
    return AssemblyConfig.<AnnotationMirror>builder()
        .source(mirror)
        .shellName(attributeValue(mirror, env.known().assemblyShellName(), String.class))
        .packageLocal(attributeValue(mirror, env.known().assemblyPackageLocal(), Boolean.class))
        .build();
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

  private <T> T attributeValue(AnnotationMirror mirror,
                                      ExecutableElement attribute,
                                      Class<T> type) {
    return attributeValue(env.elements().getElementValuesWithDefaults(mirror), attribute, type);
  }

  private <T> T attributeValue(Map<? extends ExecutableElement, ? extends AnnotationValue> allValues,
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
            qualifiedInnerClassName(element, new StringBuilder()).toString());
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
}
