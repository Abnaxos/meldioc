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

import ch.raffael.meldioc.idea.AbstractMeldInspection;
import ch.raffael.meldioc.idea.Context;
import ch.raffael.meldioc.idea.MeldQuickFix;
import ch.raffael.meldioc.model.AccessPolicy;
import ch.raffael.meldioc.model.CElement;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import io.vavr.collection.Array;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.util.stream.Stream;

public final class ElementNotAccessibleInspection extends AbstractMeldInspection {

  @Override
  protected Option<PsiElement> findClassProblemElement(PsiClass element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return msg.conflicts().headOption()
        .flatMap(CElement::findClass)
        .flatMap(c -> findExtendsElement(element, c.type()))
        .orElse(() -> super.findClassProblemElement(element, msg, inspectionContext));
  }

  @Override
  protected Option<PsiElement> findMethodProblemElement(PsiMethod element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return msg.element().mountConfigOption().isDefined()
        ? findMethodReturnType(element)
        : super.findMethodProblemElement(element, msg, inspectionContext);
  }

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return msg.conflicts().headOption()
        .map(CElement::source)
        .filter(m -> PsiManager.getInstance(element.getProject()).isInProject(m))
        .map(m -> SmartPointerManager.getInstance(m.getProject()).createSmartPsiElementPointer(m))
        .map(mptr -> Stream.of(AccessPolicy.values())
            .filter(ap ->
                msg.conflicts().head().withAccessPolicy(ap).accessibleTo(inspectionContext.adaptor(), msg.element()))
            .<Option<? extends LocalQuickFix>>map(ap -> MeldQuickFix.forAnyModifierOwner(
                "Make '"
                    + msg.conflicts().head().parentOption().map(p -> p.name() + ".").getOrElse("")
                    + msg.conflicts().head().name()
                    + "' " + ap.displayName(), element, msg.element(),
                ctx -> Option.of((PsiModifierListOwner) mptr.getElement())
                    .flatMap(m -> Option.of(m.getModifierList()))
                    .forEach(mods -> {
                      Stream.of(AccessPolicy.values())
                          .map(AccessPolicy::keyword)
                          .filter(k -> !k.isEmpty())
                          .forEach(k -> mods.setModifierProperty(k, false));
                      //noinspection MagicConstant
                      mods.setModifierProperty(ap.keyword(), true);
                      UndoUtil.markPsiFileForUndo(mods.getContainingFile());
                    })))
            .collect(Array.collector())).getOrElse(Array.empty());
  }

}
