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

package ch.raffael.compose.model.validation;

import javax.annotation.Nullable;

/**
 * TODO javadoc
 */
@Deprecated(forRemoval = true)
public class Validator<E, A> {

  private final ProblemReporter<E, A> reporter;
  private final E element;
  @Nullable
  private final A annotation;

  private int errorCount = 0;
  private int warningCount = 0;

  Validator(ProblemReporter<E, A> reporter, E element, A annotation) {
    this.reporter = reporter;
    this.element = element;
    this.annotation = annotation;
  }

  Validator(ProblemReporter<E, A> reporter, E element) {
    this.reporter = reporter;
    this.element = element;
    this.annotation = null;
  }

  public Validator<E, A> validIdentifier(String identifier, String identifierDescription) {
    return validIdentifierImpl(identifier, identifierDescription, false);
  }

  public Validator<E, A> validJavaIdentifier(String identifier, String identifierDescription) {
    return validIdentifierImpl(identifier, identifierDescription, true);
  }

  public Validator<E, A> validJavaPackageName(String packageName) {
    if (!packageName.isEmpty()) {
      var elements = packageName.split("\\.");
      for (int i = 0; i < elements.length; i++) {
        elements[i] = validJavaIdentifier(elements[i], "package name").substituteOnError(elements[i], "$invalid$");
      }
    }
    return this;
  }

  private Validator<E, A> validIdentifierImpl(String identifier, String identifierDescription, boolean allowDollar) {
    if (identifier.isEmpty()) {
      return error("Empty " + identifierDescription);
    }
    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
      return error("Invalid "+identifierDescription);
    }
    if (!allowDollar && identifier.charAt(0) == '$') {
      return error("Invalid " + identifierDescription + " ('$' is reserved)");
    }
    for (int i = 1; i < identifier.length(); i++) {
      if (!Character.isJavaIdentifierPart(identifier.charAt(1))) {
        return error("Invalid " + identifierDescription);
      }
      if (!allowDollar && identifier.charAt(1) == '$') {
        return error("Invalid " + identifierDescription + " ('$' is reserved)");
      }
    }
    return this;
  }

  protected Validator<E, A> error(String format, Object... args) {
    if (annotation == null) {
      reporter.error(element, format, (Object[]) args);
    } else {
      reporter.error(element, annotation, format, (Object[]) args);
    }
    errorCount++;
    return this;
  }

  protected Validator<E, A> warning(String format, Object... args) {
    if (annotation == null) {
      reporter.warning(element, format, (Object[]) args);
    } else {
      reporter.warning(element, annotation, format, (Object[]) args);
    }
    warningCount++;
    return this;
  }

  public int errorCount() {
    return errorCount;
  }

  public int warningCount() {
    return warningCount;
  }

  public boolean hasErrors() {
    return errorCount > 0;
  }

  public <T> T substituteOnError(T value, T substitute) {
    if (hasErrors()) {
      return substitute;
    } else {
      return value;
    }
  }

}
