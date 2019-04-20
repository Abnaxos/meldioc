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

import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.IconLayerProvider;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import io.vavr.Tuple2;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.annotation.Annotation;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public class ComposeIcons extends IconProvider implements IconLayerProvider, LineMarkerProvider {

  private static final Set<Tuple2<Class<? extends Annotation>, String>> COMPOSE_CLASS_ANNOTATIONS = Set(
      Configuration.class,
      Module.class,
      ExtensionPoint.Api.class)
      .map(c -> Tuple(c, c.getCanonicalName()));
//  private static final Set<Tuple3<Class<? extends Annotation>, String, String>> COMPOSE_METHOD_ANNOTATIONS = Set(
//      Tuple(ExtensionPoint.Provision.class, ">Extension point provision"),
//      Tuple(Module.Mount.class, ">Mount point"),
//      Tuple(Parameter.class, "Configuration parameter"),
//      Tuple(Provision.class, ">Provision"),
//      Tuple(Setup.class, "Setup method"))
//      .map(tpl -> Tuple(tpl._1, tpl._1.getCanonicalName(), tpl._2));

  static final Option<Icon> COMPOSE_CLASS_LAYER =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics gfx = img.getGraphics();
            gfx.setColor(JBColor.DARK_GRAY);
            gfx.fillRect(3, 15, 10, 1);
            return new ImageIcon(img);
          });

  @Nullable
  @Override
  public Icon getLayerIcon(@NotNull Iconable element, boolean isLocked) {
    if (element instanceof PsiClass) {
      return COMPOSE_CLASS_ANNOTATIONS
          .find(a -> AnnotationUtil.isAnnotated((PsiClass) element, a._2, 0))
          .flatMap(__ -> COMPOSE_CLASS_LAYER)
          .getOrNull();
    }
    return null;
  }

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
    return "ch.raffael.compose";
  }
}
