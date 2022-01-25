/*
 *  Copyright (c) 2021 Raffael Herzog
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
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.util.stream.Stream;

import static ch.raffael.meldioc.idea.ElementRendering.renderElement;
import static io.vavr.control.Option.some;

public final class TypeNotExtendableInspection extends AbstractMeldInspection {

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    var classSrcElement = msg.conflicts().headOption().getOrNull();
    if (classSrcElement == null) {
      return List.empty();
    }
    var psiElement = some(classSrcElement)
        .map(SrcElement::source)
        .filter(PsiClass.class::isInstance).map(PsiClass.class::cast)
        .filter(c -> PsiManager.getInstance(element.getProject()).isInProject(c))
        .getOrNull();
    if (psiElement == null) {
      return List.empty();
    }
    if (psiElement.hasModifierProperty(PsiModifier.FINAL)) {
      return List.of(MeldQuickFix.forAnyModifierOwner(
          "Make " + renderElement(psiElement) + " not final", psiElement, classSrcElement,
          ctx -> Option.of(ctx.psi().getModifierList()).forEach(m -> m.setModifierProperty(PsiModifier.FINAL, false))));
    } else if (psiElement.hasModifierProperty(PsiModifier.SEALED)) {
      var requireNonSealed = Stream.concat(
          Option.of(psiElement.getSuperClass()).toJavaStream(), Stream.of(psiElement.getInterfaces()))
          .anyMatch(e -> e.hasModifierProperty(PsiModifier.SEALED));
      return List.of(MeldQuickFix.forClass(
          "Make " + renderElement(psiElement) + (requireNonSealed ? " non-sealed" : " not sealed"),
          psiElement, classSrcElement,
          ctx -> {
            Option.of(ctx.psi().getModifierList()).forEach(m -> {
              m.setModifierProperty(PsiModifier.SEALED, false);
              if (requireNonSealed) {
                m.setModifierProperty(PsiModifier.NON_SEALED, true);
              }
            });
            Option.of(ctx.psi().getPermitsList()).forEach(PsiElement::delete);
          }));
    } else {
      return List.empty();
    }
  }
}
