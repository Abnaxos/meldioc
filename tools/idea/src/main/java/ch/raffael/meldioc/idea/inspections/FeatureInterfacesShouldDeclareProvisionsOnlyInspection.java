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

import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.idea.AbstractMeldInspection;
import ch.raffael.meldioc.idea.Context;
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.model.config.ModelAnnotationType;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.lang.annotation.Annotation;

public final class FeatureInterfacesShouldDeclareProvisionsOnlyInspection extends AbstractMeldInspection {

  private static final List<Class<? extends Annotation>> REMOVABLE_ANNOTATIONS = ModelAnnotationType.all().toStream()
      .<Class<? extends Annotation>>map(ModelAnnotationType::annotationType)
      .reject(t -> t.equals(Provision.class))
      .toList();

  @Override
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    if (element instanceof PsiMethod) {
      return List.of(Annotations.addAnnotationFix((PsiMethod) element,
          Provision.class, REMOVABLE_ANNOTATIONS));
    } else {
      return List.of(msg.conflicts().headOption()
          .map(SrcElement::source)
          .filter(PsiMethod.class::isInstance).map(PsiMethod.class::cast)
          .flatMap(psi -> Annotations.addAnnotationFix(psi, Provision.class, REMOVABLE_ANNOTATIONS)));
    }
  }
}
