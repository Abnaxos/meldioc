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

import ch.raffael.compose.Module;
import ch.raffael.compose.idea.AbstractComposeInspection;
import ch.raffael.compose.idea.ComposeQuickFix;
import ch.raffael.compose.idea.Context;
import ch.raffael.compose.idea.QuickFixes;
import ch.raffael.compose.model.messages.Message;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.Set;
import java.util.stream.Stream;

import static io.vavr.API.*;

public class ComposeAnnotationOutsideModuleInspection extends AbstractComposeInspection {

  @Override
  protected Seq<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message msg, Context inspectionContext) {
    return Seq(
        ComposeQuickFix.forAnyModifierOwner("Remove compose annotations", element, msg.element(), ctx -> {
          Set<String> annotationNames = ctx.element().configs()
              .filter(c -> !c.type().auxiliaryRole())
              .map(c -> c.type().annotationType().getCanonicalName()).toJavaSet();
          Stream.of(AnnotationUtil.findAnnotations(ctx.psi(), annotationNames))
              .forEach(PsiElement::delete);
        }),
        Option(PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance))
            .map(PsiClass.class::cast)
            .flatMap(c -> Annotations.addAnnotationFix(c, Module.class))
            .map(QuickFixes::lowPriority)
    );
  }

}
