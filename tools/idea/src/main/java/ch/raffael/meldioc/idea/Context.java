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

package ch.raffael.meldioc.idea;

import ch.raffael.meldioc.model.Model;
import ch.raffael.meldioc.model.messages.Message;
import ch.raffael.meldioc.model.messages.MessageSink;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import io.vavr.collection.Set;

import java.util.concurrent.atomic.AtomicReference;

import static io.vavr.API.*;

public final class Context {

  private final Logger LOG = Logger.getInstance(Context.class);

  private volatile AtomicReference<Set<Message<PsiElement, PsiType>>> messages = new AtomicReference<>(Set());

  private final MessageSink<PsiElement, PsiType> messageSink = msg -> {
    LOG.debug("Found problem: " + msg);
    messages.updateAndGet(s -> s.add(msg));
  };
  private final boolean isOnTheFly;
  private final IdeaAdaptor adaptor;
  private final Model<PsiElement, PsiType> model;
  private final PsiElement dummyElement;

  Context(boolean isOnTheFly, IdeaAdaptor adaptor, PsiElement dummyElement) {
    this.isOnTheFly = isOnTheFly;
    this.adaptor = adaptor;
    model = Model.create(adaptor, messageSink);
    this.dummyElement = dummyElement;
  }

  public void inspect(PsiClass cls) {
    model.modelOf(PsiTypesUtil.getClassType(cls));
  }

  public Set<Message<PsiElement, PsiType>> messages() {
    return messages.get();
  }

  public boolean onTheFly() {
    return isOnTheFly;
  }

  public IdeaAdaptor adaptor() {
    return adaptor;
  }

  public Model<PsiElement, PsiType> model() {
    return model;
  }
}
