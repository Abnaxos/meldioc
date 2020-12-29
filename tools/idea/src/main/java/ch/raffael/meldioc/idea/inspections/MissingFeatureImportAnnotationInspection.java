/*
 *  Copyright (c) 2020 Raffael Herzog
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

import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.idea.AbstractMeldInspection;
import ch.raffael.meldioc.idea.Context;
import ch.raffael.meldioc.idea.MeldQuickFix;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import static io.vavr.control.Option.none;
import static java.util.Arrays.asList;

public class MissingFeatureImportAnnotationInspection extends AbstractMeldInspection {

  @Override
  protected Option<PsiElement> findClassProblemElement(PsiClass element,
                                                       Message<PsiElement, PsiType> msg,
                                                       Context inspectionContext) {
    return findSuperReference(element, msg).map(PsiElement.class::cast)
        .orElse(() -> super.findClassProblemElement(element, msg, inspectionContext));
  }

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    if (!(element instanceof PsiClass)) {
      return List.empty();
    }
    return List.of(findSuperReference((PsiClass)element, msg).flatMap(e ->
        MeldQuickFix.create(PsiJavaCodeReferenceElement.class, "Annotate with @Import", e, msg.element(), ctx -> {
          if (ctx.psi().getReferenceNameElement() == null) {
            return;
          }
          var annotation = JavaPsiFacade.getInstance(ctx.psi().getProject()).getElementFactory()
              .createAnnotationFromText("@" + Feature.Import.class.getCanonicalName(), ctx.psi());
          ctx.psi().addBefore(annotation, ctx.psi().getReferenceNameElement());
          JavaCodeStyleManager.getInstance(ctx.psi().getProject()).shortenClassReferences(ctx.psi());
        })));
  }

  private Option<PsiJavaCodeReferenceElement> findSuperReference(PsiClass element, Message<PsiElement, PsiType> msg) {
    if (msg.conflicts().headOption().isEmpty()) {
      return none();
    }
    var conflict = msg.conflicts().head();
    var superTypes = List.<PsiJavaCodeReferenceElement>of();
    if (element.getExtendsList() != null) {
      superTypes = superTypes.appendAll(asList(element.getExtendsList().getReferenceElements()));
    }
    if (element.getImplementsList() != null) {
      superTypes = superTypes.appendAll(asList(element.getImplementsList().getReferenceElements()));
    }
    return superTypes.find(t -> t.isReferenceTo(conflict.source()));
  }
}
