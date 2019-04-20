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

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static io.vavr.API.*;

public abstract class AbstractComposeInspection extends LocalInspectionTool /* TODO (2019-04-19) CustomSuppressableInspectionTool */ {

  private static final Key<Context> MODEL_KEY =
      Key.create(AbstractComposeInspection.class.getName() + ".MODEL_KEY");

  protected AbstractComposeInspection() {
  }

  protected void inspectClass(PsiClass aClass, Context ctx) {
  }

  protected void inspectMethod(PsiMethod method, Context ctx) {
  }

  protected PsiElement findInspectionElement(PsiElement element) {
    if (element instanceof PsiClass) {
      return orDefault(((PsiClass) element).getNameIdentifier(), element);
    }
    return element;
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
    return new JavaElementVisitor() {
      private final Context context = Option(session.getUserData(MODEL_KEY))
          .getOrElse(() -> {
            PsiFile dummyElement = session.getFile();
            Context newCtx = new Context(
                holder, isOnTheFly,
                new IdeaAdaptor(
                    JavaPsiFacade.getInstance(session.getFile().getProject()),
                    session.getFile().getResolveScope(), dummyElement),
                dummyElement);
//                Option(session.getFile().getVirtualFile())
//                    .flatMap(f -> Option(FileIndexFacade.getInstance(session.getFile().getProject()).getModuleForFile(f)))
//                    .map(m -> GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(m, false))
//                    .getOrElse(GlobalSearchScope.EMPTY_SCOPE)));
            session.putUserData(MODEL_KEY, newCtx);
            return newCtx;
          });

      @Override
      public void visitClass(@Nonnull PsiClass aClass) {
        inspectClass(aClass, context);
      }

      @Override
      public void visitMethod(@Nonnull PsiMethod method) {
        inspectMethod(method, context);
      }
    };
  }

  private static <T> T orDefault(@Nullable T nullable, T fallback) {
    return nullable != null ? nullable : fallback;
  }

}
