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

import ch.raffael.compose.model.Model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * TODO javadoc
 */
public final class Environment {

  private final ProcessingEnvironment procEnv;
  private final KnownElements known;
  private final Adaptors adaptors;
  private final AnnotationProcessingModelAdaptor modelAdaptor;
  private final Model<Element, TypeMirror> model;

  public Environment(ProcessingEnvironment procEnv) {
    this.procEnv = procEnv;
    known = new KnownElements(this);
    adaptors = new Adaptors(this);
    modelAdaptor = new AnnotationProcessingModelAdaptor(this);
    model = Model.create(modelAdaptor);
  }

  public ProcessingEnvironment procEnv() {
    return procEnv;
  }

  public KnownElements known() {
    return known;
  }

  public Adaptors adaptors() {
    return adaptors;
  }

  public AnnotationProcessingModelAdaptor modelAdaptor() {
    return modelAdaptor;
  }

  public Elements elements() {
    return procEnv().getElementUtils();
  }

  public Types types() {
    return procEnv().getTypeUtils();
  }

  public Model<Element, TypeMirror> model() {
    return model;
  }

  public static abstract class WithEnv {
    protected final Environment env;
    protected WithEnv(Environment env) {
      this.env = env;
    }
  }

}
