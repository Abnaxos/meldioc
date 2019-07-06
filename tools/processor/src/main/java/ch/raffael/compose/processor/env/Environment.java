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
import ch.raffael.compose.processor.TypeRef;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Provides access to several components of the processing and code
 * generation context.
 */
public final class Environment {

  private final ProcessingEnvironment procEnv;
  private final KnownElements known;
  private final Adaptor adaptor;
  private final Model<Element, TypeRef> model;

  public Environment(ProcessingEnvironment procEnv) {
    this.procEnv = procEnv;
    known = new KnownElements(this);
    adaptor = new Adaptor(this);
    model = Model.create(adaptor);
  }

  public ProcessingEnvironment procEnv() {
    return procEnv;
  }

  /**
   * Known {@code javax.lang.model} elements such as {@code Object} or our
   * annotations.
   */
  public KnownElements known() {
    return known;
  }

  public Adaptor adaptor() {
    return adaptor;
  }

  public Elements elements() {
    return procEnv().getElementUtils();
  }

  public Types types() {
    return procEnv().getTypeUtils();
  }

  public Model<Element, TypeRef> model() {
    return model;
  }

  public TypeRef typeRef(TypeMirror mirror) {
    return new TypeRef(types(), mirror);
  }

  public static abstract class WithEnv {
    protected final Environment env;
    protected WithEnv(Environment env) {
      this.env = env;
    }
  }

}
