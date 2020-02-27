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

import ch.raffael.meldioc.idea.AbstractComposeInspection;
import ch.raffael.meldioc.idea.ComposeQuickFix;
import ch.raffael.meldioc.idea.Context;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import io.vavr.API;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import static io.vavr.API.*;
import static java.util.function.UnaryOperator.identity;

public final class NonOverridableMethodInspection extends AbstractComposeInspection {

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return Option(element)
        .filter(PsiMethod.class::isInstance).map(PsiMethod.class::cast)
        .map(elem -> API.<Option<? extends LocalQuickFix>>Seq(
            Option.when(elem.hasModifier(JvmModifier.FINAL),
                ComposeQuickFix.forAnyModifierOwner("Make non-final", element, msg.element(),
                    ctx -> Option(ctx.psi().getModifierList())
                        .forEach(ml -> ml.setModifierProperty(PsiModifier.FINAL, false))))
                .flatMap(identity()),
            Option.when(elem.hasModifier(JvmModifier.STATIC),
                ComposeQuickFix.forAnyModifierOwner("Make non-static", element, msg.element(),
                    ctx -> Option(ctx.psi().getModifierList())
                        .forEach(ml -> ml.setModifierProperty(PsiModifier.STATIC, false))))
                .flatMap(identity())))
        .get();
  }
}
