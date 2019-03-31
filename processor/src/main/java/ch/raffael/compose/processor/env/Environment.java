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

import ch.raffael.compose.processor.model.CompositionTypeModel;
import ch.raffael.compose.tooling.validation.ProblemReporter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO javadoc
 */
public final class Environment {

  private final ProcessingEnvironment procEnv;
  private final ProblemReporter<Element, AnnotationMirror> problems;
  private final KnownElements known;
  private final Adaptors adaptors;
  private final CompositionTypeModel.Pool compositionTypeModelPool;

  private final ConcurrentHashMap<DeclaredType, CompositionTypeModel> compositionInfos = new ConcurrentHashMap<>();

  public Environment(ProcessingEnvironment procEnv, ProblemReporter<Element, AnnotationMirror> problems) {
    this.procEnv = procEnv;
    this.problems = problems;
    known = new KnownElements(this);
    adaptors = new Adaptors(this);
    compositionTypeModelPool = new CompositionTypeModel.Pool(this);
  }

  public ProcessingEnvironment procEnv() {
    return procEnv;
  }

  public ProblemReporter<Element, AnnotationMirror> problems() {
    return problems;
  }

  public KnownElements known() {
    return known;
  }

  public Adaptors adaptors() {
    return adaptors;
  }

  public Elements elements() {
    return procEnv().getElementUtils();
  }

  public Types types() {
    return procEnv().getTypeUtils();
  }

  public CompositionTypeModel.Pool compositionTypeModels() {
    return compositionTypeModelPool;
  }

  public static abstract class WithEnv {
    protected final Environment env;
    protected WithEnv(Environment env) {
      this.env = env;
    }
  }

}
