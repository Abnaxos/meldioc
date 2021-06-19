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

package ch.raffael.meldioc.idea;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import com.google.common.base.Suppliers;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.IconLayerProvider;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.ui.IconManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.function.Supplier;

/**
 * TODO JavaDoc
 */
public class MeldIcons extends IconProvider implements IconLayerProvider, LineMarkerProvider {

  private final IconManager iconManager;

  private final List<Tuple2<String, Supplier<? extends Icon>>> typeIcons = List.of(
      Tuple.of(Feature.class.getCanonicalName(), iconLoader("feature.svg")),
      Tuple.of(Configuration.class.getCanonicalName(), iconLoader("configuration.svg")),
      Tuple.of(ExtensionPoint.class.getCanonicalName(), iconLoader("extension-point-acceptor.svg")));
  private final List<Tuple2<String, Supplier<? extends Icon>>> memberIcons = List.of(
      Tuple.of(Provision.class.getCanonicalName(), iconLoader("provision.svg")),
      Tuple.of(Feature.Mount.class.getCanonicalName(), iconLoader("mount.svg")),
      Tuple.of(ExtensionPoint.class.getCanonicalName(), iconLoader("extension-point.svg")),
      Tuple.of(Setup.class.getCanonicalName(), iconLoader("setup.svg")),
      Tuple.of(Parameter.class.getCanonicalName(), iconLoader("parameter.svg")));

  public MeldIcons() {
    this.iconManager = IconManager.getInstance();
    new Thread(() -> typeIcons.appendAll(memberIcons).forEach(t -> t._2().get()),
        getClass().getName()+" preloader")
        .start();
  }

  private Supplier<Icon> iconLoader(String name) {
    return Suppliers.memoize(() -> iconManager.getIcon(name, getClass()));
  }

  @Nullable
  @Override
  public Icon getLayerIcon(@NotNull Iconable element, boolean isLocked) {
    if (element instanceof PsiClass) {
      return findIcon((PsiClass) element, typeIcons);
    } else if (element instanceof PsiMethod) {
      return findIcon((PsiMember) element, memberIcons);
    } else {
      return null;
    }
  }

  private Icon findIcon(
      PsiModifierListOwner element, List<Tuple2<String, Supplier<? extends Icon>>> icons) {
    return icons
        .find(t -> AnnotationUtil.isAnnotated(element, t._1, 0))
        .map(t -> t._2.get())
        .getOrNull();
  }

  @SuppressWarnings("rawtypes")
  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
    return null;
  }

  @Nullable
  @Override
  public Icon getIcon(@NotNull PsiElement element, int flags) {
    return null;
  }

  @NotNull
  @Override
  public String getLayerDescription() {
    return "ch.raffael.meldioc";
  }
}
