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

package ch.raffael.meldioc.idea.inspections;

import ch.raffael.meldioc.idea.AbstractMeldInspection;
import ch.raffael.meldioc.idea.MeldQuickFix;
import ch.raffael.meldioc.idea.Context;
import ch.raffael.meldioc.model.config.ModelAnnotationType;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.util.Collection;
import java.util.stream.Stream;

public final class ConflictingCompositionRolesInspection extends AbstractMeldInspection {

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return msg.element().configs()
        .filter(c -> c.type().role())
        .map(cnf -> MeldQuickFix.forAnyModifierOwner("Keep only " + cnf.type().displayName(), element, msg.element(),
            ctx -> {
              Collection<String> removable = ModelAnnotationType.all()
                  .filter(t -> t.role() && !cnf.isConfigType(t.annotationType()))
                  .map(c -> c.annotationType().getCanonicalName())
                  .toJavaSet();
              Stream.of(AnnotationUtil.findAnnotations(ctx.psi(), removable)).forEach(PsiElement::delete);
            }));
  }

}
