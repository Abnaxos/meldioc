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

public final class ConflictingProvisionsInspection extends AbstractMeldInspection {

  // TODO (2019-04-20) quick fix: override and choose; or show candidates -- how?


//  @Override
//  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
//    SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(element.getProject());
//    Seq<Tuple2<CElement<None, None>, Option<SmartPsiElementPointer<PsiElement>>>> conflictsBackup =
//        msg.conflicts()
//            .map(e -> Tuple(e.detach(), Option(smartPointerManager.createSmartPsiElementPointer(e.source()))));
//    if (conflictsBackup.isEmpty()) {
//      return Seq();
//    }
//    return Seq(
//        MeldQuickFix.forClass("Override '" + conflictsBackup.head()._1.name() + "'", element, msg.element(),
//            ctx -> {
//              Seq<Tuple2<CElement<None, None>, PsiMethod>> conflicts = conflictsBackup
//                  .map(tpl -> tpl.map2(r -> r.flatMap(e -> Option(e.getElement()))))
//                  .map(tpl -> tpl.map2(o -> o.filter(PsiMethod.class::isInstance).map(PsiMethod.class::cast)))
//                  .filter(tpl -> tpl._2.isDefined())
//                  .map(tpl -> tpl.map2(Option::get));
//              if (conflicts.isEmpty()) {
//                return;
//              }
//              Tuple2<PsiMethod, PsiType> mostSpecific = conflicts.map(tpl -> Tuple(tpl._2, tpl._2.getReturnType()))
//                  .filter(tpl -> tpl._2 != null)
//                  .reduceOption((left, right) ->
//                      left._2.isAssignableFrom(right._2) ? right : left)
//                  .getOrNull();
//              if (mostSpecific == null) {
//                return;
//              }
//              OverrideImplementUtil.overrideOrImplementMethod(ctx.psi(), mostSpecific._1, PsiSubstitutor.EMPTY, false, true);
//            }));
//  }
}
