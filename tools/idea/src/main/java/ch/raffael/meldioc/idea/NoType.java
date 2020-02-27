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

import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeVisitor;
import com.intellij.psi.TypeAnnotationProvider;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class NoType extends PsiType {

  public static final NoType INSTANCE = new NoType();

  private NoType() {
    super(TypeAnnotationProvider.EMPTY);
  }

  @NotNull
  @Override
  public String getPresentableText() {
    return getCanonicalText();
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public boolean equalsToText(@NotNull String text) {
    return false;
  }

  @Override
  @Nullable
  public <A> A accept(@NotNull PsiTypeVisitor<A> visitor) {
    return visitor.visitType(this);
  }

  @Nullable
  @Override
  public GlobalSearchScope getResolveScope() {
    return GlobalSearchScope.EMPTY_SCOPE;
  }

  @NotNull
  @Override
  public PsiType[] getSuperTypes() {
    return EMPTY_ARRAY;
  }

}
