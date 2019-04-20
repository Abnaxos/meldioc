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

package ch.raffael.compose.idea.inspections;

import ch.raffael.compose.idea.AbstractComposeInspection;
import ch.raffael.compose.idea.Context;
import ch.raffael.compose.model.messages.Message;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

public class NoImplementationCandidateInspection extends AbstractComposeInspection {

  private static final Logger LOG = Logger.getInstance(NoImplementationCandidateInspection.class);

  @Override
  protected void inspectClass(PsiClass aClass, Context ctx) {
    LOG.debug("Inspecting: " + aClass);
    ctx.inspect(aClass);
    ctx.messages().forEach(m -> LOG.debug("Compose message: " + m));
    ctx.messages().filter(m -> m.id().map(id -> id.equals(Message.Id.NoImplementationCandidate)).getOrElse(false))
        .filter(m -> aClass.equals(m.element().source()))
        .forEach(m -> {
          LOG.debug("Registering problem: " + m);
          ctx.problems().registerProblem(findInspectionElement(aClass), m.renderMessage(PsiElement::toString));
        });
  }
}
