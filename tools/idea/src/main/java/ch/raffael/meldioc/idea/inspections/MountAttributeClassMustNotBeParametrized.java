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

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.idea.AbstractMeldInspection;
import ch.raffael.meldioc.idea.Context;
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.model.config.ConfigurationConfig;
import ch.raffael.meldioc.model.messages.Message;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import io.vavr.collection.Vector;
import io.vavr.control.Option;

public class MountAttributeClassMustNotBeParametrized extends AbstractMeldInspection {

  @Override
  protected Option<PsiElement> findClassProblemElement(PsiClass element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return Option.of(element.getAnnotation(Configuration.class.getName()))
        .flatMap(a -> Option.of(a.findAttributeValue(ConfigurationConfig.MOUNT)))
        .flatMap(v -> Option.of(Vector.ofAll(PsiTreeUtil.findChildrenOfAnyType(v, false, PsiClassObjectAccessExpression.class))))
        .getOrElse(Vector.empty())
        .find(v -> v.getOperand().getType().equals(msg.conflicts().headOption().map(SrcElement::type).getOrNull()))
        .map(PsiElement.class::cast)
        .orElse(super.findClassProblemElement(element, msg, inspectionContext));
  }
}
