/*
 *  Copyright (c) 2021 Raffael Herzog
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

package ch.raffael.meldioc.processor.test.tools

import javax.annotation.processing.Completion
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class MarkerProcessor implements Processor {

  static final MSG_HEAD = '###MARKER:'

  final Set<String> supportedOptions = Set.of()
  final Set<String> supportedAnnotationTypes = Set.of(Marker.name)
  private ProcessingEnvironment processingEnv

  @Override
  void init(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv
  }

  @Override
  boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(Marker.class).each {Element e ->
      processingEnv.messager.printMessage(Diagnostic.Kind.OTHER,
                                          "###MARKER:${e.getAnnotation(Marker).value()}", e)
    }
    true
  }

  @Override
  Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
    return List.of()
  }

  @Override
  SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported()
  }

  static String marker(Diagnostic diagnostic) {
    if (diagnostic.getMessage(Locale.US).startsWith(MSG_HEAD)) {
      return diagnostic.getMessage(null).substring(MSG_HEAD.length())
    } else {
      return null
    }
  }
}
