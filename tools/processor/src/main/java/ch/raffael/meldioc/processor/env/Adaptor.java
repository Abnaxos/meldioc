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

package ch.raffael.meldioc.processor.env;

import ch.raffael.meldioc.model.AccessPolicy;
import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.model.config.ConfigurationConfig;
import ch.raffael.meldioc.model.config.ElementConfig;
import ch.raffael.meldioc.model.config.ExtensionPointConfig;
import ch.raffael.meldioc.model.config.FeatureConfig;
import ch.raffael.meldioc.model.config.MountConfig;
import ch.raffael.meldioc.model.config.ParameterConfig;
import ch.raffael.meldioc.model.config.ParameterPrefixConfig;
import ch.raffael.meldioc.model.config.ProvisionConfig;
import ch.raffael.meldioc.model.config.SetupConfig;
import ch.raffael.meldioc.model.messages.Message;
import ch.raffael.meldioc.model.messages.MessageSink;
import ch.raffael.meldioc.processor.Diagnostics;
import ch.raffael.meldioc.processor.TypeRef;
import ch.raffael.meldioc.processor.util.Elements;
import io.vavr.Tuple;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static ch.raffael.meldioc.processor.util.Elements.asDeclaredType;
import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public final class Adaptor extends Environment.WithEnv
    implements ch.raffael.meldioc.model.Adaptor<Element, TypeRef>, MessageSink<Element, TypeRef> {

  private static final Pattern INIT_RE = Pattern.compile("<(cl)?init>");
  private static final String CONSTRUCTOR_NAME = "<init>";
  private static final Option<ElementKind> RECORD_ELEMENT_KIND = Try.of(() -> some(ElementKind.valueOf("RECORD")))
      .recover(IllegalArgumentException.class, none())
      .get();

  private static final Option<Modifier> OPT_MOD_SEALED = Try.of(() -> some(Modifier.valueOf("SEALED")))
      .recover(IllegalArgumentException.class, none())
      .get();

  private final boolean includeMessageId;
  private final TypeRef noneTypeRef;

  private int errorCount = 0;
  private int warningCount = 0;

  Adaptor(Environment env, boolean includeMessageId) {
    super(env);
    this.includeMessageId = includeMessageId;
    noneTypeRef = typeRef(env.types().getNoType(TypeKind.NONE));
  }

  public int errorCount() {
    return errorCount;
  }

  public int warningCount() {
    return warningCount;
  }

  @Override
  public boolean isSubtypeOf(TypeRef left, TypeRef right) {
    if (left.mirror().getKind().isPrimitive() || right.mirror().getKind().isPrimitive()) {
      return left.mirror().getKind() == right.mirror().getKind();
    } else {
      return env.types().isSubtype(left.mirror(), right.mirror());
    }
  }

  @Override
  public boolean isReference(TypeRef type) {
    return type.mirror() instanceof DeclaredType || type.mirror() instanceof TypeVariable;
  }

  @Override
  public boolean isInterface(TypeRef type) {
    if (type.mirror() instanceof DeclaredType) {
      return ((DeclaredType) type.mirror()).asElement().getKind() == ElementKind.INTERFACE;
    } else {
      return false;
    }
  }

  @Override
  public boolean hasTypeParameters(TypeRef type) {
    if (isNoType(type)) {
      return false;
    }
    return type.mirror() instanceof DeclaredType
        && !((TypeElement) ((DeclaredType) type.mirror()).asElement()).getTypeParameters().isEmpty();
  }

  @Override
  public boolean isPrimitive(TypeRef type) {
    return type.mirror() instanceof PrimitiveType;
  }

  @Override
  public boolean isEnumType(TypeRef type) {
    return isElementKind(type, ElementKind.ENUM);
  }

  @Override
  public boolean isAnnotationType(TypeRef type) {
    return isElementKind(type, ElementKind.ANNOTATION_TYPE);
  }

  @Override
  public boolean isRecordType(TypeRef type) {
    return RECORD_ELEMENT_KIND.map(k -> isElementKind(type, k)).getOrElse(false);
  }

  private boolean isElementKind(TypeRef type, ElementKind kind) {
    var mirror = env.types().erasure(type.mirror());
    if (!(mirror instanceof DeclaredType)) {
      return false;
    }
    return ((DeclaredType) mirror).asElement().getKind() == kind;
  }

  @Override
  public TypeRef typeOf(ClassRef ref) {
    if (ref.isPrimitive()) {
      switch (ref.className()) {
        case "int":
          return typeRef(env.types().getPrimitiveType(TypeKind.INT));
        case "long":
          return typeRef(env.types().getPrimitiveType(TypeKind.LONG));
        case "short":
          return typeRef(env.types().getPrimitiveType(TypeKind.SHORT));
        case "byte":
          return typeRef(env.types().getPrimitiveType(TypeKind.BYTE));
        case "double":
          return typeRef(env.types().getPrimitiveType(TypeKind.DOUBLE));
        case "float":
          return typeRef(env.types().getPrimitiveType(TypeKind.FLOAT));
        case "char":
          return typeRef(env.types().getPrimitiveType(TypeKind.CHAR));
        case "boolean":
          return typeRef(env.types().getPrimitiveType(TypeKind.BOOLEAN));
        case "void":
          return typeRef(env.types().getPrimitiveType(TypeKind.VOID));
        default:
          return noneTypeRef;
      }
    } else {
      return Option.of(env.elements().getTypeElement(ref.canonicalName()))
          .map(Element::asType)
          .map(this::typeRef)
          .getOrElse(() -> noneTypeRef);
    }
  }

  @Nonnull
  private TypeRef typeRef(TypeMirror t) {
    return env.typeRef(t);
  }

  @Override
  public SrcElement<Element, TypeRef> classElement(TypeRef type) {
    var declaredType = asDeclaredType(type.mirror());
    var element = Elements.asElement(declaredType);
    var classElem = srcElement(SrcElement.Kind.CLASS, declaredType, element);
    if (element.getEnclosingElement() instanceof TypeElement) {
      classElem.parent(classElement(typeRef(element.getEnclosingElement().asType())));
    } else if (element.getEnclosingElement() instanceof ExecutableElement) {
      classElem.parent(methodSrcElement(asDeclaredType(element.getEnclosingElement().getEnclosingElement().asType()),
          (ExecutableElement) element));
    }
    return classElem.build();
  }

  @Override
  public Seq<SuperType<TypeRef>> superTypes(TypeRef type) {
    return Vector.ofAll(env.types().directSupertypes(type.mirror()))
        .map(t -> {
          var annotations = t.getAnnotationMirrors();
          return new SuperType<>(typeRef(t),
              annotations.stream().anyMatch(a -> env.types().isSameType(
                  a.getAnnotationType(), env.known().importAnnotation())),
              annotations.stream().anyMatch(a -> env.types().isSameType(
                  a.getAnnotationType(), env.known().dependsOnAnnotation())));
        });
  }

  @Override
  public Seq<SrcElement<Element, TypeRef>> declaredMethods(TypeRef type) {
    return executables(type, e -> !INIT_RE.matcher(e.getSimpleName().toString()).matches());
  }

  @Override
  public Seq<SrcElement<Element, TypeRef>> constructors(TypeRef type) {
    return executables(type, e -> e.getSimpleName().toString().equals(CONSTRUCTOR_NAME))
        .map(m -> m.withType(noneTypeRef));
  }

  private Seq<SrcElement<Element, TypeRef>> executables(TypeRef type, Predicate<? super ExecutableElement> filter) {
    var declaredType = asDeclaredType(type.mirror());
    return declaredType.asElement().getEnclosedElements().stream()
        .filter(ExecutableElement.class::isInstance)
        .map(Elements::asExecutableElement)
        .filter(filter)
        .map(e -> methodSrcElement(declaredType, e))
        .collect(Vector.collector());
  }

  @Override
  public String packageOf(SrcElement<Element, TypeRef> element) {
    return env.elements().getPackageOf(element.source()).getQualifiedName().toString();
  }

  @Override
  public TypeRef iterableOf(TypeRef componentType) {
    return typeRef(env.types().getDeclaredType(Elements.asTypeElement(env.known().iterable().asElement()),
        componentType.mirror()));
  }

  @Override
  public TypeRef collectionOf(TypeRef componentType) {
    return typeRef(env.types().getDeclaredType(Elements.asTypeElement(env.known().collection().asElement()),
        componentType.mirror()));
  }

  @Override
  public TypeRef listOf(TypeRef componentType) {
    return typeRef(env.types().getDeclaredType(Elements.asTypeElement(env.known().list().asElement()),
        componentType.mirror()));
  }

  @Override
  public TypeRef componentTypeOfIterable(TypeRef iterableType) {
    return some(iterableType.mirror())
        .filter(DeclaredType.class::isInstance)
        .map(DeclaredType.class::cast)
        .flatMap(this::findIterable)
        .map(t -> typeRef(t.getTypeArguments().get(0)))
        .getOrElse(() -> noneTypeRef);
  }

  @Override
  public TypeRef noType() {
    return noneTypeRef;
  }

  @Override
  public void message(Message<Element, TypeRef> message) {
    message(0, message);
  }

  private void message(int depth, Message<Element, TypeRef> message) {
    if (isSuppressed(message)) {
      return;
    }
    StringBuilder buf = new StringBuilder();
    if (includeMessageId) {
      Diagnostics.appendMessageId(buf, message);
    }
    if (depth == 1) {
      buf.append("Origin of previous message: ");
    } else if (depth > 1) {
      buf.append("Origin (").append(depth).append(") of previous message: ");
    }
    buf.append(message.renderMessage(Element::toString));
    Iterator.unfoldLeft(message.element().source(), e -> {
      if (e == null) {
        return none();
      } else if (e instanceof TypeElement) {
        buf.append("\nClass: ").append(((TypeElement) e).getQualifiedName());
        return none();
      } else if (e instanceof ExecutableElement) {
        buf.append("\nMethod: ").append(e.getSimpleName()).append("()");
      } else if (e instanceof VariableElement) {
        buf.append("\nParameter: ").append(e.getSimpleName());
      }
      return some(Tuple.of(e.getEnclosingElement(), e));
    });
    Diagnostic.Kind diagnosticKind =
        message.id().map(id ->id.warning() ? Diagnostic.Kind.WARNING : Diagnostic.Kind.ERROR)
            .getOrElse(Diagnostic.Kind.OTHER);
    if (diagnosticKind == Diagnostic.Kind.ERROR) {
      errorCount++;
    } else if (diagnosticKind == Diagnostic.Kind.WARNING || diagnosticKind == Diagnostic.Kind.MANDATORY_WARNING) {
      warningCount++;
    }
    env.procEnv().getMessager().printMessage(diagnosticKind, buf, message.element().source());
    message.origins().forEach(o -> message(depth + 1, o));
  }

  private Option<DeclaredType> findIterable(DeclaredType from) {
    if (env.types().erasure(from).equals(env.types().erasure(env.known().iterable()))) {
      return some(from);
    }
    var superTypes = Vector.ofAll(env.types().directSupertypes(from)).map(Elements::asDeclaredType);
    return superTypes
        .reject(t -> t.equals(env.known().object()))
        .filter(t -> env.types().erasure(t).equals(env.types().erasure(env.known().iterable())))
        .map(Elements::asDeclaredType)
        .headOption()
        .orElse(() -> superTypes.map(this::findIterable)
            .filter(Option::isDefined)
            .headOption()
            .getOrElse(none()));
  }


  private SrcElement<Element, TypeRef> methodSrcElement(DeclaredType enclosing, ExecutableElement element) {
    var methodType = Elements.isStatic(element)
        ? Elements.asExecutableType(element.asType())
        : Elements.asExecutableType(env.types().asMemberOf(enclosing, element));
    return some(srcElement(SrcElement.Kind.METHOD, methodType.getReturnType(), element))
        .peek(e -> e.parent(classElement(typeRef(enclosing))))
        .peek(e -> e.exceptions(element.getThrownTypes().stream()
            .map(t -> new TypeRef(env.types(), t)).collect(io.vavr.collection.List.collector())))
        .map(SrcElement.Builder::build)
        .map(m -> m.withParameters(Vector.ofAll(methodType.getParameterTypes())
            .zip(element.getParameters())
            .map(t -> srcElement(SrcElement.Kind.PARAMETER, t._1(), t._2()))
            .map(SrcElement.Builder::build)))
        .get();
  }

  private SrcElement.Builder<Element, TypeRef> srcElement(SrcElement.Kind kind, TypeMirror type, Element element) {
    var builder = SrcElement.<Element, TypeRef>builder()
        .kind(kind)
        .source(element)
        .type(typeRef(type));
    if (kind == SrcElement.Kind.CLASS) {
      builder.name(Iterator.unfoldLeft(element, e -> some(e)
          .filter(TypeElement.class::isInstance)
          .map(e2 -> Tuple.of(e.getEnclosingElement(), e2.getSimpleName().toString())))
          .mkString("."));
    } else {
      builder.name(element.getSimpleName().toString());
    }
    srcElementModifiers(builder, element);
    srcElementConfigs(builder, element);
    return builder;
  }

  private SrcElement.Builder<Element, TypeRef> srcElementModifiers(SrcElement.Builder<Element, TypeRef> builder, Element element) {
    var mods = element.getModifiers();
    if (mods.contains(Modifier.PRIVATE)) {
      builder.accessPolicy(AccessPolicy.PRIVATE);
    } else if (mods.contains(Modifier.PROTECTED)) {
      builder.accessPolicy(AccessPolicy.PROTECTED);
    } else if (mods.contains(Modifier.PUBLIC)) {
      builder.accessPolicy(AccessPolicy.PUBLIC);
    } else {
      builder.accessPolicy(AccessPolicy.LOCAL);
    }
    builder.isStatic(mods.contains(Modifier.STATIC));
    builder.isFinal(mods.contains(Modifier.FINAL) || mods.contains(Modifier.NATIVE));
    builder.isSealed(OPT_MOD_SEALED.map(mods::contains).getOrElse(false));
    builder.isAbstract(mods.contains(Modifier.ABSTRACT));
    return builder;
  }

  private void srcElementConfigs(SrcElement.Builder<Element, TypeRef> builder, Element element) {
    var allConfigs = element.getAnnotationMirrors().stream()
        .map(a -> {
          ElementConfig<Element> config = null;
          var v = env.elements().getElementValuesWithDefaults(a);
          var t = a.getAnnotationType().asElement();
          if (t.equals(env.known().configuration().asElement())) {
            config = ConfigurationConfig.<Element>builder()
                .source(element)
                .mount(requireListArg(v, env.known().configurationMount(), TypeMirror.class).map(env::classRef))
                .packageLocal((boolean) requireArg(v, env.known().configurationPackageLocal()))
                .shellName((String) requireArg(v, env.known().configurationShellName()))
                .build();
          } else if (t.equals(env.known().setup().asElement())) {
            config = SetupConfig.<Element>builder()
                .source(element)
                .build();
          } else if (t.equals(env.known().parameter().asElement())) {
            config = ParameterConfig.<Element>builder()
                .source(element)
                .value((String) requireArg(v, env.known().parameterValue()))
                .absolute((boolean) requireArg(v, env.known().parameterAbsolute()))
                .build();
          } else if (t.equals(env.known().parameterPrefix().asElement())) {
            config = ParameterPrefixConfig.<Element>builder()
                .source(element)
                .value((String) requireArg(v, env.known().parameterPrefixValue()))
                .build();
          } else if (t.equals(env.known().extensionPointAcceptor().map(DeclaredType::asElement).getOrNull())) {
            config = ExtensionPointConfig.<Element>builder()
                .source(element)
                .fromAcceptorAnnotation(true)
                .build();
          } else if (t.equals(env.known().extensionPoint().asElement())) {
            config = ExtensionPointConfig.<Element>builder()
                .source(element)
                .fromAcceptorAnnotation(false)
                .build();
          } else if (t.equals(env.known().feature().asElement())) {
            config = FeatureConfig.<Element>builder()
                .source(element)
                .build();
          } else if (t.equals(env.known().featureMount().asElement())) {
            config = MountConfig.<Element>builder()
                .source(element)
                .injected((boolean) requireArg(v, env.known().featureMountInjected()))
                .build();
          } else if (t.equals(env.known().provision().asElement())) {
            if (a.getElementValues().containsKey(env.known().provisionShared())) {
              // TODO (2020-12-21) workaround because javac doesn't warn about deprecated annotation attributes
              env.procEnv().getMessager().printMessage(Diagnostic.Kind.WARNING,
                  "The `shared` attribute is deprecated and will be removed, use `singleton` instead",
                  element, a, a.getElementValues().get(env.known().provisionShared()));
            }
            config = ProvisionConfig.<Element>builder()
                .source(element)
                .singleton((boolean) requireArg(v, env.known().provisionSingleton())
                    || (boolean) requireArg(v, env.known().provisionShared()))
                .override((boolean) requireArg(v, env.known().provisionOverride()))
                .build();
          }
          return Option.of(config);
        })
        .flatMap(Option::toJavaStream)
        .collect(io.vavr.collection.List.collector());
    // deprecation of ExtensionPoint.Acceptor: if both ExtensionPoint and Acceptor are present, keep the former only
    allConfigs = ExtensionPointConfig.removeFromAcceptorIfApplicable(allConfigs);
    builder.addAllConfigs(allConfigs);
  }

  private Object requireArg(Map<? extends ExecutableElement, ? extends AnnotationValue> values, ExecutableElement arg) {
    return Objects.requireNonNull(values.get(arg), "values.get(" + arg + ")").getValue();
  }

  @SuppressWarnings("unchecked")
  private <T> Seq<T> requireListArg(Map<? extends ExecutableElement, ? extends AnnotationValue> values,
                                    ExecutableElement arg, Class<T> elementType) {
    return ((List<AnnotationValue>) Objects.requireNonNull(values.get(arg), "values.get(" + arg + ")").getValue())
        .stream()
        .map(v -> elementType.cast(Objects.requireNonNull(v.getValue(), "v.getValue()")))
        .collect(Vector.collector());
  }

  private boolean isSuppressed(Message<Element, TypeRef> message) {
    if (message.id().isEmpty() || !message.id().get().warning()) {
      return false;
    }
    return isSuppressed(message.id().get(), message.element().source());
  }

  private boolean isSuppressed(Message.Id id, Element element) {
    var suppressed = element.getAnnotationMirrors().stream()
        .filter(am -> am.getAnnotationType().equals(env.known().suppressWarnings()))
        .map(am -> env.elements().getElementValuesWithDefaults(am))
        .flatMap(v -> requireListArg(v, env.known().suppressWarningsValue(), String.class).toJavaStream())
        .map(Message.Suppression::meldId)
        .anyMatch(v -> Message.Suppression.all().equals(v) || id.equals(Message.Id.forName(v).getOrNull()));
    if (suppressed) {
      return true;
    } else if (element.getKind() == ElementKind.PACKAGE) {
      return false;
    } else {
      var enclosing = element.getEnclosingElement();
      return enclosing != null && isSuppressed(id, enclosing);
    }
  }
}
