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

import ch.raffael.meldioc.meta.Version;
import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.model.config.ElementConfig;
import ch.raffael.meldioc.model.messages.Message;
import com.google.common.collect.MapMaker;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.RemoveAnnotationQuickFix;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static io.vavr.API.*;
import static java.util.function.Function.identity;

public abstract class AbstractComposeInspection extends LocalInspectionTool /* TODO (2019-04-19) CustomSuppressableInspectionTool */ {

  private static final Logger LOG = Logger.getInstance(AbstractComposeInspection.class);

  private static final String AVAILABILITY_MARKER_CLASS = Version.class.getCanonicalName();

  protected final Logger log = Logger.getInstance(getClass());

  private static final ClassValue<Predicate<? super Message<? super PsiElement, ? super PsiType>>> AUTO_ID_FILTERS =
      new ClassValue<>() {
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

  protected PsiElement findProblemElement(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    if (element instanceof PsiClass) {
      return findClassProblemElement((PsiClass) element, msg, inspectionContext).getOrElse(element);
    } else if (element instanceof PsiMethod) {
      return findMethodProblemElement((PsiMethod) element, msg, inspectionContext).getOrElse(element);
    } else if (element instanceof PsiParameter) {
      return findParameterProblemElement((PsiParameter) element, msg, inspectionContext).getOrElse(element);
    }
    return element;
  }

  protected Option<PsiElement> findClassProblemElement(PsiClass element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return findNameIdentifier(element);
  }

  protected Option<PsiElement> findMethodProblemElement(PsiMethod element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return findNameIdentifier(element);
  }

  protected Option<PsiElement> findParameterProblemElement(PsiParameter element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return findNameIdentifier(element);
  }

  protected Option<PsiElement> findNameIdentifier(PsiNameIdentifierOwner element) {
    return Option(element.getNameIdentifier());
  }

  protected Option<PsiElement> findMethodReturnType(PsiMethod element) {
    return Option(element)
        .map(PsiMethod::getReturnTypeElement);
  }

  protected Option<PsiElement> findAnnotationElement(PsiModifierListOwner element, Class<? extends Annotation> annotationType) {
    return Option(element.getModifierList())
        .flatMap(mods -> Option(mods.findAnnotation(annotationType.getCanonicalName())));
  }

  protected Option<PsiElement> findExtendsElement(PsiClass element, PsiType type) {
    return Option(element.getExtendsList())
        .map(PsiReferenceList::getReferenceElements)
        .flatMap(ext -> Array(ext)
            .find(t -> Option(t.resolve())
                .filter(PsiClass.class::isInstance).map(PsiClass.class::cast)
                .flatMap(c -> Option(PsiTypesUtil.getClassType(c)))
                .filter(type::isAssignableFrom).isDefined())
            .flatMap(t -> Option(t.getReferenceNameElement())));
  }

  public static Option<PsiClass> findReturnTypeClass(PsiElement element) {
    return Option(element).filter(PsiMethod.class::isInstance).map(PsiMethod.class::cast)
        .flatMap(m -> Option(m.getReturnType()))
        .filter(PsiClassType.class::isInstance).map(PsiClassType.class::cast)
        .flatMap(rt -> Option(rt.resolve()));
  }
  protected Traversable<Option<? extends LocalQuickFix>> quickFixes(PsiElement element, Message<PsiElement, PsiType> msg, Context inspectionContext) {
    return Seq();
  }

  private void inspect(ProblemsHolder problems, PsiElement element, Context ctx) {
    log.trace("Inspecting: " + element);
    PsiClass enclosing = (PsiClass) PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance);
    if (enclosing == null) {
      log.debug("No enclosing class found, skipping inspection");
      return;
    }
    ctx.inspect(enclosing);
    ctx.messages().filter(messageFilter)
        .filter(m -> element.equals(m.element().source()))
        .forEach(msg -> handle(problems, element, msg, ctx));
  }

  protected void handle(ProblemsHolder problems, PsiElement element, Message<PsiElement, PsiType> msg, Context ctx) {
    PsiElement problemElement = findProblemElement(element, msg, ctx);
    log.debug("Registering problem: " + msg + " on " + problemElement);
    Traversable<LocalQuickFix> quickFixes = quickFixes(element, msg, ctx).flatMap(identity());
    if (!msg.origins().isEmpty()) {
      quickFixes = quickFixes.toList().appendAll(
          msg.origins()
              .filter(o -> o.element().source() instanceof NavigatablePsiElement)
              .map(o -> NavigateQuickFix.toOrigin(renderMessage(o), o.element().source()))
              .flatMap(identity()));
    }
    registerProblem(problems, msg, problemElement, quickFixes);
  }

  private void registerProblem(ProblemsHolder problems, Message<PsiElement, PsiType> msg, PsiElement problemElement, Traversable<LocalQuickFix> quickFixes) {
    problems.registerProblem(problemElement,
        renderMessage(msg),
        quickFixes.toJavaArray(LocalQuickFix[]::new));
  }

  private String renderMessage(Message<PsiElement, PsiType> msg) {
    return msg.renderMessage(ElementRendering::renderElement);
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
        log.debug("ch.raffael.meldioc not available to " + session.getFile());
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

  protected static class Annotations {
    private Annotations() {
    }

    public static Option<LocalQuickFix> addAnnotationFix(
    PsiModifierListOwner elem,
    Class<? extends Annotation> annotationType) {
      return addAnnotationFix(elem, annotationType, Seq());
    }

    public static Option<LocalQuickFix> addAnnotationFix(
        PsiModifierListOwner elem,
        Class<? extends Annotation> annotationType,
        Seq<Class<? extends Annotation>> remove) {
      return Some(new AddAnnotationFix(annotationType.getCanonicalName(), elem,
          remove.map(Class::getCanonicalName).toJavaArray(String[]::new)));
    }

    public static Option<LocalQuickFix> removeAnnotationFix(
        PsiModifierListOwner elem,
        Class<? extends Annotation> annotationType) {
      return Option(elem.getModifierList())
          .flatMap(e -> Option(e.findAnnotation(annotationType.getCanonicalName())))
          .map(a -> new RemoveAnnotationQuickFix(a, elem));
    }

    public static Option<LocalQuickFix> annotateReturnTypeClass(PsiElement element,
                                                                Class<? extends Annotation> annotationType) {
      return findReturnTypeClass(element)
          .filter(rt -> PsiManager.getInstance(element.getProject()).isInProject(rt))
          .flatMap(rt -> addAnnotationFix(rt, annotationType));
    }

    public static PsiAnnotation annotationFromConfig(ElementConfig<?> config, PsiElement ctx) {
      return JavaPsiFacade.getInstance(ctx.getProject()).getElementFactory().createAnnotationFromText(
          "@" + config.type().annotationType().getCanonicalName()
              + config.valueMapWithoutDefaults()
                  .mapValues(Annotations::annotationValueAsJava)
                  .foldLeft(Seq(), (s, e) -> s.append(e._1() + " = " + e._2()))
                  .mkString("(", ", ", ")"),
          ctx);
    }

    private static String annotationValueAsJava(Object value) {
      if (value.getClass().isArray()) {
        return Array((Object[]) value).map(Annotations::annotationValueAsJava).mkString(", ");
      } else if (value instanceof ClassRef) {
          ClassRef c = (ClassRef) value;
          return (c.packageName().isEmpty()) ? c.className() : c.packageName() + "." + c.className();
      } else if (value instanceof String) {
        return '"' + StringEscapeUtils.escapeJava((String) value) + '"';
      } else if (value instanceof Long) {
        return value.toString() + "L";
      } else if (value instanceof Float) {
        return value.toString() + "f";
      } else if (value instanceof Double) {
        return value.toString() + "d";
      } else if (value instanceof Option) {
        return ((Option<?>) value).map(Annotations::annotationValueAsJava).getOrElse("");
      } else {
        return value.toString();
      }
    }

  }

}
