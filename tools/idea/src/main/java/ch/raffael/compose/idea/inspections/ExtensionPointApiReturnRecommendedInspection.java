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

import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.idea.AbstractComposeInspection;
import ch.raffael.compose.idea.ComposeQuickFix;
import ch.raffael.compose.idea.Context;
import ch.raffael.compose.model.messages.Message;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import static io.vavr.API.*;

public class ExtensionPointApiReturnRecommendedInspection extends AbstractComposeInspection {

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return Seq(
        returnTypeClass(element)
            .filter(rt -> PsiManager.getInstance(element.getProject()).isInProject(rt))
            .flatMap(rt -> ComposeQuickFix.forMethod(
                "Annotate " + rt.getName() + " with @ExtensionPoint.Api", (PsiMethod) element, msg.element(),
                ctx -> returnTypeClass(ctx.psi())
                    .flatMap(c -> Option(c.getModifierList()))
                    .forEach(mods -> {
                      Project project = mods.getProject();
                      PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
                      PsiAnnotation annotation = factory.createAnnotationFromText(
                          "@" + ExtensionPoint.Api.class.getCanonicalName(), mods);
                      annotation = (PsiAnnotation)mods.addBefore(annotation, mods.getFirstChild());
                      JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation);
                      UndoUtil.markPsiFileForUndo(mods.getContainingFile());
                    }))));
  }

  private Option<PsiClass> returnTypeClass(PsiElement element) {
    return Option(element).filter(PsiMethod.class::isInstance).map(PsiMethod.class::cast)
        .flatMap(m -> Option(m.getReturnType()))
        .filter(PsiClassType.class::isInstance).map(PsiClassType.class::cast)
        .flatMap(rt -> Option(rt.resolve()));
  }
}
