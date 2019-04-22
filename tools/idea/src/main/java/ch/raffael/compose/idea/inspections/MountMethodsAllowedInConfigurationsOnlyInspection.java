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

import ch.raffael.compose.Configuration;
import ch.raffael.compose.Module;
import ch.raffael.compose.idea.AbstractComposeInspection;
import ch.raffael.compose.idea.ComposeQuickFix;
import ch.raffael.compose.idea.Context;
import ch.raffael.compose.idea.Names;
import ch.raffael.compose.model.messages.Message;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.util.PsiTreeUtil;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import static io.vavr.API.*;

public class MountMethodsAllowedInConfigurationsOnlyInspection extends AbstractComposeInspection {

  @Override
  protected Option<PsiElement> findMethodProblemElement(PsiMethod element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return findAnnotationElement(element, Module.Mount.class);
  }

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return Seq(
        ComposeQuickFix.forMethod("Remove " + Names.shortQualifiedName(Module.Mount.class) + " annotation",
            element, msg.element(),
            ctx -> Option(ctx.psi())
                .filter(PsiModifierListOwner.class::isInstance).map(PsiModifierListOwner.class::cast)
                .forEach(e -> Option(AnnotationUtil.findAnnotation(e, Module.Mount.class.getCanonicalName()))
                    .forEach(PsiElement::delete))),
        Option(PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance))
            .map(PsiClass.class::cast)
            .map(c -> Tuple(c, "Annotate '" + c.getName() + "' with @" + Names.shortQualifiedName(Configuration.class)))
            .map(tpl -> tpl.map1(c -> SmartPointerManager.getInstance(c.getProject()).createSmartPsiElementPointer(c)))
            .flatMap(tpl -> tpl.apply((cref, descr) ->
                ComposeQuickFix.forMethod(descr, element, msg.element(),
                    ctx -> Option(cref.getElement())
                        .filter(PsiClass.class::isInstance).map(PsiClass.class::cast)
                        .forEach(mods -> {
                          Annotations.addAnnotation(mods, Configuration.class);
                          Option(AnnotationUtil.findAnnotation(mods, Module.class.getCanonicalName()))
                              .forEach(PsiElement::delete);
                        })).map(ComposeQuickFix::lowPriority)
            )));
  }
}
