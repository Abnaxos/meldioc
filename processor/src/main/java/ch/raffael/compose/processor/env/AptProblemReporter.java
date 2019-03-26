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

import ch.raffael.compose.tooling.validation.ProblemReporter;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import static ch.raffael.compose.processor.Debug.DEVEL_MODE;

/**
 * @since 2019-03-23
 */
public final class AptProblemReporter implements ProblemReporter<Element, AnnotationMirror> {

  private final Messager messager;

  private int errorCount = 0;
  private int warningCount = 0;

  public AptProblemReporter(Messager messager) {
    this.messager = messager;
  }

  @Override
  public void error(Element element, AnnotationMirror annotation, String messageFormat, Object... args) {
    if (!DEVEL_MODE) {
      messager.printMessage(Diagnostic.Kind.ERROR, String.format(messageFormat, args), element, annotation);
    } else {
      messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "ERROR: " + String.format(messageFormat, args), element, annotation);
    }
    errorCount++;
  }

  @Override
  public void error(Element element, String messageFormat, Object... args) {
    if (!DEVEL_MODE) {
      messager.printMessage(Diagnostic.Kind.ERROR, String.format(messageFormat, args), element);
    } else {
      messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "ERROR: " + String.format(messageFormat, args), element);
    }
    errorCount++;
  }

  @Override
  public void warning(Element element, AnnotationMirror annotation, String messageFormat, Object... args) {
    messager.printMessage(Diagnostic.Kind.WARNING, String.format(messageFormat, args), element, annotation);
    warningCount++;
  }

  @Override
  public void warning(Element element, String messageFormat, Object... args) {
    messager.printMessage(Diagnostic.Kind.WARNING, String.format(messageFormat, args), element);
    warningCount++;
  }

  @Override
  public int errorCount() {
    return errorCount;
  }

  @Override
  public int warningCount() {
    return warningCount;
  }
}
