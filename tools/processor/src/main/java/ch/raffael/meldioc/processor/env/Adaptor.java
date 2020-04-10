/*
 *  Copyright (c) 2020 Raffael Herzog
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

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.model.AccessPolicy;
import ch.raffael.meldioc.model.CElement;
import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.model.config.ConfigurationConfig;
import ch.raffael.meldioc.model.config.ElementConfig;
import ch.raffael.meldioc.model.config.ExtensionPointAcceptorConfig;
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
import java.util.regex.Pattern;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

@Configuration(mount={Object.class})
public final class Adaptor extends Environment.WithEnv
    implements ch.raffael.meldioc.model.Adaptor<Element, TypeRef>, MessageSink<Element, TypeRef> {

  private final static Pattern CONSTRUCTOR_RE = Pattern.compile("<(cl)?init>");

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
    var mirror = env.types().erasure(type.mirror());
    var e = env.types().erasure(env.known().enumeration());
    return env.types().isSubtype(mirror, e) && !e.equals(mirror);
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
  public CElement<Element, TypeRef> classElement(TypeRef type) {
    var declaredType = Elements.asDeclaredType(type.mirror());
    var element = Elements.asElement(declaredType);
    return some(celement(CElement.Kind.CLASS, declaredType, element))
        .peek(c -> Iterator.unfoldRight(declaredType.getEnclosingType(), t -> t instanceof DeclaredType
            ? some(Tuple.of(classElement(typeRef(t)), ((DeclaredType) t).getEnclosingType()))
            : none())
            .forEach(c::parent))
        .map(CElement.Builder::build)
        .get();
  }
  @Override
  public Seq<? extends TypeRef> superTypes(TypeRef type) {
    return Vector.ofAll(env.types().directSupertypes(type.mirror())).map(this::typeRef);
  }

  @Override
  public Seq<CElement<Element, TypeRef>> declaredMethods(TypeRef type) {
    var declaredType = Elements.asDeclaredType(type.mirror());
    var implyPublic = some(declaredType.asElement().getKind())
        .map(k -> k == ElementKind.INTERFACE || k == ElementKind.ANNOTATION_TYPE)
        .get();
    return declaredType.asElement().getEnclosedElements().stream()
        .filter(ExecutableElement.class::isInstance)
        .map(Elements::asExecutableElement)
        .filter(e -> !CONSTRUCTOR_RE.matcher(e.getSimpleName().toString()).matches())
        .map(e -> methodCElement(declaredType, e))
        .collect(Vector.collector());
  }

  @Override
  public String packageOf(CElement<Element, TypeRef> element) {
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
  public void message(Message<Element, TypeRef> message) {
    message(0, message);
  }

  private void message(int depth, Message<Element, TypeRef> message) {
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


  private CElement<Element, TypeRef> methodCElement(DeclaredType enclosing, ExecutableElement element) {
    var methodType = Elements.isStatic(element)
        ? Elements.asExecutableType(element.asType())
        : Elements.asExecutableType(env.types().asMemberOf(enclosing, element));
    return some(celement(CElement.Kind.METHOD, methodType.getReturnType(), element))
        .peek(e -> e.parent(classElement(typeRef(enclosing))))
        .map(CElement.Builder::build)
        .map(m -> m.withParameters(Vector.ofAll(methodType.getParameterTypes())
            .zip(element.getParameters())
            .map(t -> celement(CElement.Kind.PARAMETER, t._1(), t._2()))
            .map(CElement.Builder::build)))
        .get();
  }

  private CElement.Builder<Element, TypeRef> celement(CElement.Kind kind, TypeMirror type, Element element) {
    var builder = CElement.<Element, TypeRef>builder()
        .kind(kind)
        .source(element)
        .type(typeRef(type));
    if (kind == CElement.Kind.CLASS) {
      builder.name(Iterator.unfoldLeft(element, e -> some(e)
          .filter(TypeElement.class::isInstance)
          .map(e2 -> Tuple.of(e.getEnclosingElement(), e2.getSimpleName().toString())))
          .mkString("."));
    } else {
      builder.name(element.getSimpleName().toString());
    }
    celementModifiers(builder, element);
    celementConfigs(builder, element);
    return builder;
  }

  private CElement.Builder<Element, TypeRef> celementModifiers(CElement.Builder<Element, TypeRef> builder, Element element) {
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
    builder.isAbstract(mods.contains(Modifier.ABSTRACT));
    return builder;
  }

  private void celementConfigs(CElement.Builder<Element, TypeRef> builder, Element element) {
    element.getAnnotationMirrors().stream()
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
          } else if (t.equals(env.known().extensionPointAcceptor().asElement())) {
            config = ExtensionPointAcceptorConfig.<Element>builder()
                .source(element)
                .build();
          } else if (t.equals(env.known().extensionPoint().asElement())) {
            config = ExtensionPointConfig.<Element>builder()
                .source(element)
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
            config = ProvisionConfig.<Element>builder()
                .source(element)
                .shared((boolean) requireArg(v, env.known().provisionShared()))
                .override((boolean) requireArg(v, env.known().provisionOverride()))
                .build();
          }
          return Option.of(config);
        })
        .forEach(o -> o.forEach(builder::addConfigs));
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
}
