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

import ch.raffael.compose.meta.Version;
import ch.raffael.compose.model.messages.Message;
import com.google.common.collect.MapMaker;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static io.vavr.API.*;

public abstract class AbstractComposeInspection extends LocalInspectionTool /* TODO (2019-04-19) CustomSuppressableInspectionTool */ {

  private static final Logger LOG = Logger.getInstance(AbstractComposeInspection.class);

  private static final String AVAILABILITY_MARKER_CLASS = Version.class.getCanonicalName();

  protected final Logger log = Logger.getInstance(getClass());

  private static final Key<Context> MODEL_KEY =
      Key.create(AbstractComposeInspection.class.getName() + ".MODEL_KEY");
  private static final Key<Boolean> AVAILABLE_KEY =
      Key.create(AbstractComposeInspection.class.getName() + ".AVAILABLE_KEY");

  private static final ClassValue<Predicate<? super Message<? super PsiElement, ? super PsiType>>> AUTO_ID_FILTERS =
      new ClassValue<Predicate<? super Message<? super PsiElement, ? super PsiType>>>() {
        private final String inspection = "Inspection";
        @Override
        protected Predicate<? super Message<? super PsiElement, ? super PsiType>> computeValue(@Nonnull Class<?> type) {
          String name = type.getSimpleName();
          if (name.endsWith(inspection)) {
            name = name.substring(0, name.length() - inspection.length());
          }
          return filterById(Message.Id.valueOf(name));
        }
      };

  protected final Predicate<? super Message<? super PsiElement, ? super PsiType>> messageFilter;

  private static ConcurrentMap<LocalInspectionToolSession, Session> SESSIONS =
      new MapMaker()
          .concurrencyLevel(1)
          .weakKeys()
          .makeMap();


  protected AbstractComposeInspection(Predicate<? super Message<? super PsiElement, ? super PsiType>> messageFilter) {
    this.messageFilter = messageFilter;
  }

  protected AbstractComposeInspection(Message.Id id) {
    this.messageFilter = filterById(id);
  }

  protected AbstractComposeInspection() {
    messageFilter = AUTO_ID_FILTERS.get(getClass());
  }

  protected static Predicate<Message<?, ?>> filterById(Message.Id id) {
    return m -> m.id().map(i -> i.equals(id)).getOrElse(false);
  }

  protected PsiElement findProblemElement(PsiElement element) {
    if (element instanceof PsiClass) {
      return orDefault(findClassProblemElement((PsiClass) element), element);
    } else if (element instanceof PsiMethod) {
      return orDefault(findMethodProblemElement((PsiMethod) element), element);
    } else if (element instanceof PsiParameter) {
      return orDefault(findParameterProblemElement((PsiParameter) element), element);
    }
    return element;
  }

  @Nullable
  protected PsiElement findClassProblemElement(PsiClass element) {
    return element.getNameIdentifier();
  }

  @Nullable
  private PsiIdentifier findMethodProblemElement(PsiMethod element) {
    return element.getNameIdentifier();
  }

  @Nullable
  protected PsiElement findParameterProblemElement(PsiParameter element) {
    return element;
  }

  protected Seq<? extends LocalQuickFix> quickFixes(Message problem) {
    return Seq();
  }

  protected void inspect(ProblemsHolder holder, PsiElement element, Context ctx) {
    log.trace("Inspecting: " + element);
    PsiClass enclosing = (PsiClass) PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance);
    if (enclosing == null) {
      log.debug("No enclosing class found, skipping inspection");
      return;
    }
    ctx.inspect(enclosing);
    //ctx.messages().forEach(m -> log.debug("Compose message: " + m));
    ctx.messages().filter(messageFilter)
        .filter(m -> element.equals(m.element().source()))
        .forEach(m -> {
          PsiElement problemElement = findProblemElement(element);
          log.debug("Registering problem: " + m + " on " + problemElement);
          holder.registerProblem(problemElement,
              m.renderMessage(PsiElement::toString),
              Seq.<LocalQuickFix>narrow(quickFixes(m)).toJavaArray(LocalQuickFix[]::new));
        });
  }

  private static <T> T orDefault(@Nullable T nullable, T fallback) {
    return nullable != null ? nullable : fallback;
  }

  @Override
  public void inspectionFinished(@NotNull LocalInspectionToolSession session, @NotNull ProblemsHolder problemsHolder) {
    // try to clean up everything
    // unfortunately, this doesn't always get called as many times as the buildVisitor()
    // to avoid memory leaks, the map has weak keys
    Option.of(SESSIONS.get(session)).forEach(Session::release);
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
    return getOrCreateSession(isOnTheFly, session).use().context.fold(
        () -> new JavaElementVisitor() {},
        (ctx) -> new JavaElementVisitor() {
          @Override
          public void visitClass(@Nonnull PsiClass aClass) {
            inspect(holder, aClass, ctx);
          }
          @Override
          public void visitMethod(@Nonnull PsiMethod method) {
            inspect(holder, method, ctx);
          }
          @Override
          public void visitParameter(@Nonnull PsiParameter parameter) {
            inspect(holder, parameter, ctx);
          }
         }
    );
  }

  private Session getOrCreateSession(boolean isOnTheFly, LocalInspectionToolSession session) {
    LOG.debug("Current session count: " + SESSIONS.size());
    return SESSIONS.computeIfAbsent(session, __ -> {
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(session.getFile().getProject());
      GlobalSearchScope resolveScope = session.getFile().getResolveScope();
      if (javaPsiFacade.findClass(AVAILABILITY_MARKER_CLASS, resolveScope) == null) {
        log.debug("ch.raffael.compose not available to " + session.getFile());
        return new Session(session, None());
      } else {
        log.trace("Creating new inspection context for " + session.getFile());
        PsiFile dummyElement = session.getFile();
        Context newCtx = new Context(
            isOnTheFly,
            new IdeaAdaptor(
                javaPsiFacade,
                resolveScope, dummyElement),
            dummyElement);
        return new Session(session, Some(newCtx));
      }
    });
  }

  private static class Session {
    final AtomicInteger counter = new AtomicInteger(0);
    final LocalInspectionToolSession toolSession;
    final Option<Context> context;
    Session(LocalInspectionToolSession toolSession, Option<Context> context) {
      this.toolSession = toolSession;
      this.context = context;
    }
    Session use() {
      counter.incrementAndGet();
      return this;
    }
    void release() {
      int count = counter.decrementAndGet();
      if (count == 0) {
        SESSIONS.remove(toolSession);
      } else if (count < 0) {
        LOG.warn("Too many session releases, count == " + count);
      }
    }
  }

}
