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

package ch.raffael.meldioc.idea;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuickFixes {

  private QuickFixes() {
  }

  public static LocalQuickFix withPriority(LocalQuickFix quickFix, PriorityAction.Priority priority) {
    return new PriorityLocalQuickFix(quickFix, priority);
  }

  public static LocalQuickFix topPriority(LocalQuickFix quickFix) {
    return withPriority(quickFix, PriorityAction.Priority.TOP);
  }

  public static LocalQuickFix highPriority(LocalQuickFix quickFix) {
    return withPriority(quickFix, PriorityAction.Priority.HIGH);
  }

  public static LocalQuickFix normalPriority(LocalQuickFix quickFix) {
    return withPriority(quickFix, PriorityAction.Priority.NORMAL);
  }

  public static LocalQuickFix lowPriority(LocalQuickFix quickFix) {
    return withPriority(quickFix, PriorityAction.Priority.LOW);
  }

  private static final class PriorityLocalQuickFix implements LocalQuickFix, PriorityAction {
    private final LocalQuickFix quickFix;
    private final Priority priority;

    public PriorityLocalQuickFix(LocalQuickFix quickFix, Priority priority) {
      this.quickFix = quickFix;
      this.priority = priority;
    }

    @NotNull
    @Override
    public Priority getPriority() {
      return priority;
    }

    @NotNull
    @Nls(capitalization = Nls.Capitalization.Sentence)
    @Override
    public String getName() {
      return quickFix.getName();
    }

    @NotNull
    @Nls(capitalization = Nls.Capitalization.Sentence)
    @Override
    public String getFamilyName() {
      return quickFix.getFamilyName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      quickFix.applyFix(project, descriptor);
    }

    @Override
    public boolean startInWriteAction() {
      return quickFix.startInWriteAction();
    }

    @Nullable
    @Override
    public PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
      return quickFix.getElementToMakeWritable(currentFile);
    }
  }

}
