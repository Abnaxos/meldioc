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

import ch.raffael.compose.model.CElement;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import io.vavr.control.Option;
import io.vavr.control.Option.None;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static io.vavr.API.*;

public class ComposeQuickFix<T extends PsiElement> implements LocalQuickFix {

  private final Class<T> elementType;
  private final String name;
  private final SmartPsiElementPointer<?> elementPtr;
  private final CElement<None, None> celement;
  private final Consumer<Context<T>> fix;

  protected ComposeQuickFix(Class<T> elementType, String name, SmartPsiElementPointer<?> elementPtr, CElement<?, ?> celement, Consumer<Context<T>> fix) {
    this.elementType = elementType;
    this.name = name;
    this.elementPtr = elementPtr;
    this.celement = celement.detach();
    this.fix = fix;
  }

  public static Option<ComposeQuickFix<PsiClass>> forClass(String name, PsiElement element, CElement<?, ?> celement, Consumer<Context<PsiClass>> fix) {
    return create(PsiClass.class, name, element, celement, fix);
  }

  public static Option<ComposeQuickFix<PsiClass>> forClass(String name, PsiClass element, CElement<?, ?> celement, Consumer<Context<PsiClass>> fix) {
    return createKnownType(PsiClass.class, name, element, celement, fix);
  }

  public static Option<ComposeQuickFix<PsiMethod>> forMethod(String name, PsiElement element, CElement<?, ?> celement, Consumer<Context<PsiMethod>> fix) {
    return create(PsiMethod.class, name, element, celement, fix);
  }

  public static Option<ComposeQuickFix<PsiMethod>> forMethod(String name, PsiMethod element, CElement<?, ?> celement, Consumer<Context<PsiMethod>> fix) {
    return createKnownType(PsiMethod.class, name, element, celement, fix);
  }

  public static Option<ComposeQuickFix<PsiParameter>> forParameter(String name, PsiElement element, CElement<?, ?> celement, Consumer<Context<PsiParameter>> fix) {
    return create(PsiParameter.class, name, element, celement, fix);
  }

  public static Option<ComposeQuickFix<PsiParameter>> forParameter(String name, PsiParameter element, CElement<?, ?> celement, Consumer<Context<PsiParameter>> fix) {
    return createKnownType(PsiParameter.class, name, element, celement, fix);
  }

  public static Option<ComposeQuickFix<PsiModifierListOwner>> forAnyModifierOwner(String name, PsiElement element, CElement<?, ?> celement, Consumer<Context<PsiModifierListOwner>> fix) {
    return create(PsiModifierListOwner.class, name, element, celement, fix);
  }

  public static <T extends PsiElement> Option<ComposeQuickFix<T>> create(Class<T> type, String name, PsiElement element, CElement<?, ?> celement, Consumer<Context<T>> fix) {
    if (type.isInstance(element)) {
      return Some(new ComposeQuickFix<>(type, name,
          SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element),
          celement, fix));
    } else {
      return None();
    }
  }

  private static <T extends PsiElement> Option<ComposeQuickFix<T>> createKnownType(Class<T> type, String name, T element, CElement<?, ?> celement, Consumer<Context<T>> fix) {
    return Some(new ComposeQuickFix<>(type, name,
        SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element),
        celement, fix));
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getFamilyName() {
    return "Compose";
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = elementPtr.getElement();
    if (elementType.isInstance(element)) {
      fix.accept(new Context<>((T) element, celement, descriptor));
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  public static final class Context<T extends PsiElement> {

    private final T psi;
    private final CElement<None, None> celement;
    private final ProblemDescriptor problemDescriptor;

    private Context(T psi, CElement<None, None> celement, ProblemDescriptor problemDescriptor) {
      this.psi = psi;
      this.celement = celement;
      this.problemDescriptor = problemDescriptor;
    }

    public CElement<None, None> element() {
      return celement;
    }

    public T psi() {
      return psi;
    }

    public ProblemDescriptor problemDescriptor() {
      return problemDescriptor;
    }
  }

}
