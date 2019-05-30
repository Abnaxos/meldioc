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

package ch.raffael.compose.idea;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public final class NavigateQuickFix extends AbstractComposeQuickFix implements Iconable, PriorityAction {

  private final SmartPsiElementPointer<NavigatablePsiElement> target;

  private NavigateQuickFix(String msg, NavigatablePsiElement target) {
    super(msg);
    this.target = SmartPointerManager.getInstance(target.getProject()).createSmartPsiElementPointer(target);
  }

  public static Option<NavigateQuickFix> toOrigin(String originMessage, PsiElement target) {
    return findNavigatable(target)
        .map(t -> new NavigateQuickFix("Go to origin: " + originMessage, t));
  }

  private static Option<NavigatablePsiElement> findNavigatable(PsiElement element) {
    return Option((NavigatablePsiElement) PsiTreeUtil.findFirstParent(
        element, NavigatablePsiElement.class::isInstance));
  }

  @Override
  public Icon getIcon(int flags) {
    return AllIcons.Actions.Play_forward;
  }

  @NotNull
  @Override
  public Priority getPriority() {
    return Priority.HIGH;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    NavigatablePsiElement resolvedTarget = target.getElement();
    if (EditSourceUtil.canNavigate(resolvedTarget)) {
      EditSourceUtil.navigate(resolvedTarget, true, true);
    }
  }
}
