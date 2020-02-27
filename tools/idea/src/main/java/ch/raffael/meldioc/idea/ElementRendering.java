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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;

import javax.annotation.Nullable;
import java.util.Arrays;

public final class ElementRendering {

  private static final Logger LOG = Logger.getInstance(ElementRendering.class);

  private ElementRendering() {
  }

  public static String renderElement(PsiElement e) {
    return renderElement(new StringBuilder(), e).toString();
  }

  public static String renderClassName(@Nullable PsiClass psiClass) {
    return renderClassName(new StringBuilder(), psiClass).toString();
  }

  public static StringBuilder renderElement(StringBuilder buf, PsiElement e) {
    if (e instanceof PsiClass) {
      var c = (PsiClass) e;
      if (c.isEnum()) {
        buf.append("enum ");
      } else if (c.isAnnotationType()) {
        buf.append("annotation type ");
      } else if (c.isInterface()) {
        buf.append("interface ");
      } else {
        buf.append("class ");
      }
      renderClassName(buf, c);
    } else if (e instanceof PsiMethod){
      var m = (PsiMethod) e;
      buf.append(m.getName());
      buf.append("()");
      buf.append(" in ");
      renderClassName(buf, m.getContainingClass());
    } else if (e instanceof PsiParameter) {
      buf.append("parameter ");
      var p = (PsiParameter) e;
      var m = (PsiMethod) PsiTreeUtil.findFirstParent(p, PsiMethod.class::isInstance);
      if (m == null) {
        LOG.warn("Parameter " + p + ": could not get method");
      }
      if (p.getName() != null) {
        buf.append(p.getName());
      } else if (m != null) {
        int i = Arrays.asList(m.getParameterList().getParameters()).indexOf(p);
        if (i < 0) {
          LOG.warn("Parameter " + p + " of method " + m + ": could not get parameter index");
        }
        buf.append(i + 1);
      } else {
        buf.append("?");
      }
      if (m != null) {
        buf.append(" of ");
        renderElement(buf, m);
      }
    } else {
      LOG.warn("No renderer for type " + e.getClass() + " (" + e + ")");
      return buf.append(e);
    }
    return buf;
  }

  public static StringBuilder renderClassName(StringBuilder buf, @Nullable PsiClass psiClass) {
    int offset = buf.length();
    var c = psiClass;
    while (c != null) {
      if (buf.length() > offset) {
        buf.insert(offset, '.');
      }
      buf.insert(offset, c.getName());
      c = c.getContainingClass();
    }
    return buf;
  }
}
