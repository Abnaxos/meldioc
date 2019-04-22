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
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Setup;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.IconLayerProvider;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import io.vavr.Tuple3;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.annotation.Annotation;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public class ComposeIcons extends IconProvider implements IconLayerProvider, LineMarkerProvider {

  static final Color COLOR = Color.decode("#7d3e00");
  static final Option<Icon> CONFIGURATION =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.fillOval(9, 9, 6, 6);
            return new ImageIcon(img);
          });
  static final Option<Icon> MODULE =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.drawOval(9, 9, 6, 6);
            return new ImageIcon(img);
          });
  static final Option<Icon> EP_API =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.drawPolygon(
                new int[] { 15, 9, 15},
                new int[] { 15, 15, 9},
                3);
            return new ImageIcon(img);
          });
  static final Option<Icon> EP_PROVISION =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.fillPolygon(
                new int[] { 15, 9, 15},
                new int[] { 15, 15, 9},
                3);
            return new ImageIcon(img);
          });
  static final Option<Icon> PROVISION =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.drawRect(9, 9, 6, 6);
            return new ImageIcon(img);
          });
  static final Option<Icon> MOUNT =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.fillRect(9, 9, 6, 6);
            return new ImageIcon(img);
          });
  static final Option<Icon> SETUP =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.fillPolygon(
                new int[] { 12, 9, 12, 15, 12},
                new int[] { 15, 12, 9, 12, 15},
                5);
            return new ImageIcon(img);
          });
  static final Option<Icon> PARAMETER =
      Some(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR))
          .map(img -> {
            Graphics2D gfx = gfx(img);
            gfx.drawPolygon(
                new int[] { 12, 9, 12, 15, 12},
                new int[] { 15, 12, 9, 12, 15},
                5);
            return new ImageIcon(img);
          });

  @Nonnull
  private static Graphics2D gfx(BufferedImage img) {
    Graphics2D gfx = (Graphics2D) img.getGraphics();
    gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gfx.setColor(COLOR);
    gfx.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    return gfx;
  }

  private static final Set<Tuple3<Class<? extends Annotation>, String, Option<Icon>>> COMPOSE_CLASS_ANNOTATIONS = Set(
      Tuple(Configuration.class, CONFIGURATION),
      Tuple(Module.class, MODULE),
      Tuple(ExtensionPoint.Api.class, EP_API),
      Tuple(ExtensionPoint.Provision.class, EP_PROVISION),
      Tuple(Module.Mount.class, MOUNT),
      Tuple(Provision.class, PROVISION),
      Tuple(Setup.class, SETUP),
      Tuple(Parameter.class, PARAMETER))
      .map(tpl -> Tuple(tpl._1, tpl._1.getCanonicalName(), tpl._2));

  @Nullable
  @Override
  public Icon getLayerIcon(@NotNull Iconable element, boolean isLocked) {
    if (element instanceof PsiClass || element instanceof PsiMethod) {
      return COMPOSE_CLASS_ANNOTATIONS
          .find(i -> AnnotationUtil.isAnnotated((PsiModifierListOwner) element, i._2, 0))
          .flatMap(i -> i._3)
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
