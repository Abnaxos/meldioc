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

package ch.raffael.compose.processor.env;

import ch.raffael.compose.model.AccessPolicy;
import ch.raffael.compose.model.Adaptor;
import ch.raffael.compose.model.CElement;
import ch.raffael.compose.model.ClassRef;
import ch.raffael.compose.model.config.AssemblyConfig;
import ch.raffael.compose.model.config.ComposeConfig;
import ch.raffael.compose.model.config.ConfigurationConfig;
import ch.raffael.compose.model.config.ConfigurationPrefixConfig;
import ch.raffael.compose.model.config.ElementConfig;
import ch.raffael.compose.model.config.ExtensionPointApiConfig;
import ch.raffael.compose.model.config.ExtensionPointProvisionConfig;
import ch.raffael.compose.model.config.ModuleConfig;
import ch.raffael.compose.model.config.MountConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import ch.raffael.compose.model.messages.MessageSink;
import ch.raffael.compose.processor.util.Elements;
import io.vavr.collection.Iterator;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Option;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Objects;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public final class AnnotationProcessingModelAdaptor extends Environment.WithEnv implements Adaptor<Element, TypeMirror> {

  AnnotationProcessingModelAdaptor(Environment env) {
    super(env);
  }

  @Override
  public boolean isSubtypeOf(TypeMirror left, TypeMirror right) {
    return env.types().isSubtype(left, right);
  }

  @Override
  public boolean isReference(TypeMirror type) {
    return type instanceof DeclaredType;
  }

  @Override
  public boolean isPrimitive(TypeMirror type) {
    return type instanceof PrimitiveType;
  }

  @Override
  public boolean isEnumType(TypeMirror type) {
    type = env.types().erasure(type);
    var e = env.types().erasure(env.known().enumeration());
    return env.types().isSubtype(type, e) && !e.equals(type);
  }

  @Override
  public boolean canOverride(CElement<Element, TypeMirror> left, CElement<Element, TypeMirror> right) {
    return env.types().isSubsignature(resolveType(left), resolveType(right));
  }

  public ExecutableType resolveType(CElement<Element, TypeMirror> left) {
    return (ExecutableType) env.types().asMemberOf(left.parent().narrow(null, DeclaredType.class).type(),
        left.narrow(CElement.Kind.METHOD, ExecutableElement.class, null).source());
  }

  public ExecutableType resolveType(CElement<Element, TypeMirror> left, CElement<Element, TypeMirror> type) {
    return (ExecutableType) env.types().asMemberOf(type.narrow(null, DeclaredType.class).type(),
        left.narrow(CElement.Kind.METHOD, ExecutableElement.class, null).source());
  }

  @Override
  public TypeMirror typeOf(ClassRef ref) {
    if (ref.isPrimitive()) {
      switch (ref.className()) {
        case "int":
          return env.types().getPrimitiveType(TypeKind.INT);
        case "long":
          return env.types().getPrimitiveType(TypeKind.LONG);
        case "short":
          return env.types().getPrimitiveType(TypeKind.SHORT);
        case "byte":
          return env.types().getPrimitiveType(TypeKind.BYTE);
        case "double":
          return env.types().getPrimitiveType(TypeKind.DOUBLE);
        case "float":
          return env.types().getPrimitiveType(TypeKind.FLOAT);
        case "char":
          return env.types().getPrimitiveType(TypeKind.CHAR);
        case "boolean":
          return env.types().getPrimitiveType(TypeKind.BOOLEAN);
        default:
          return env.types().getNoType(TypeKind.NONE);
      }
    } else {
      return env.elements().getTypeElement(ref.canonicalName()).asType();
    }
  }

  @Override
  public CElement<Element, TypeMirror> classElement(TypeMirror type) {
    var declaredType = Elements.asDeclaredType(type);
    var element = Elements.asElement(declaredType);
    return Some(celement(CElement.Kind.CLASS, declaredType, element))
        .peek(c -> Iterator.unfoldRight(declaredType.getEnclosingType(), t -> t instanceof DeclaredType
            ? Some(Tuple(classElement(t), ((DeclaredType) t).getEnclosingType()))
            : None())
            .forEach(c::parent))
        .map(CElement.Builder::build)
        .get();
  }
  @Override
  public Seq<? extends TypeMirror> superTypes(TypeMirror type, MessageSink messages) {
    return Vector.ofAll(env.types().directSupertypes(type));
  }

  @Override
  public Seq<CElement<Element, TypeMirror>> declaredMethods(TypeMirror type, MessageSink messages) {
    var declaredType = Elements.asDeclaredType(type);
    return declaredType.asElement().getEnclosedElements().stream()
        .filter(ExecutableElement.class::isInstance)
        .map(Elements::asExecutableElement)
        .map(e -> methodCElement(declaredType, e))
        .collect(Vector.collector());
  }

  @Override
  public String packageOf(CElement<Element, TypeMirror> element) {
    return env.elements().getPackageOf(element.source()).getQualifiedName().toString();
  }

  @Override
  public TypeMirror iterableOf(TypeMirror componentType) {
    return env.types().getDeclaredType(null, env.known().iterable(), componentType);
  }

  @Override
  public TypeMirror collectionOf(TypeMirror componentType) {
    return env.types().getDeclaredType(null, env.known().collection(), componentType);
  }

  @Override
  public TypeMirror listOf(TypeMirror componentType) {
    return env.types().getDeclaredType(null, env.known().list(), componentType);
  }

  @Override
  public TypeMirror componentTypeOfIterable(TypeMirror iterableType) {
    return findIterable(Elements.asDeclaredType(iterableType))
        .map(t -> t.getTypeArguments().get(0))
        .getOrElseThrow(() -> new IllegalArgumentException(iterableType + " is not an Iterable"));
  }

  private Option<DeclaredType> findIterable(DeclaredType from) {
    var superTypes = Vector.ofAll(env.types().directSupertypes(from)).map(Elements::asDeclaredType);
    return superTypes
        .reject(t -> t.equals(env.known().object()))
        .filter(t -> env.types().erasure(t).equals(env.types().erasure(env.known().iterable())))
        .map(Elements::asDeclaredType)
        .headOption()
        .orElse(() -> superTypes.map(this::findIterable)
            .filter(Option::isDefined)
            .headOption()
            .getOrElse(None()));
  }


  private CElement<Element, TypeMirror> methodCElement(DeclaredType enclosing, ExecutableElement element) {
    var type = Elements.isStatic(element)
        ? Elements.asExecutableType(element.asType())
        : Elements.asExecutableType(env.types().asMemberOf(enclosing, element));
    return Some(celement(CElement.Kind.METHOD, type, element))
        .peek(e -> e.parent(classElement(enclosing)))
        .map(b -> b.parentOption(None()))
        .map(CElement.Builder::build)
        .map(m -> m.withParameters(Vector.ofAll(type.getParameterTypes())
            .zip(element.getParameters())
            .map(t -> celement(CElement.Kind.PARAMETER, t._1(), t._2()))
            .peek(p -> p.parent(m))
            .map(CElement.Builder::build)))
        .get();
  }

  private CElement.Builder<Element, TypeMirror> celement(CElement.Kind kind, TypeMirror source, Element element) {
    CElement.Builder<Element, TypeMirror> builder = CElement.<Element, TypeMirror>builder().kind(kind);
    if (kind == CElement.Kind.CLASS) {
      builder.name(Iterator.unfoldLeft(element, e -> Some(e)
          .filter(TypeElement.class::isInstance)
          .map(e2 -> Tuple(e.getEnclosingElement(), e2.getSimpleName().toString())))
          .mkString("."));
    } else {
      builder.name(element.getSimpleName().toString());
    }
    celementModifiers(builder, element);
    celementConfigs(builder, element);
    return builder;
  }

  private CElement.Builder<Element, TypeMirror> celementModifiers(CElement.Builder<Element, TypeMirror> builder, Element element) {
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

  private CElement.Builder<Element, TypeMirror> celementConfigs(CElement.Builder<Element, TypeMirror> builder, Element element) {
    return builder.configs(element.getAnnotationMirrors().stream()
        .map(a -> {
          ElementConfig<AnnotationMirror> config = null;
          var v = env.elements().getElementValuesWithDefaults(a);
          var t = a.getAnnotationType().asElement();
          if (t.equals(env.known().assembly())) {
            config = AssemblyConfig.<AnnotationMirror>builder()
                .source(a)
                .packageLocal((boolean) requireArg(v, env.known().assemblyPackageLocal()))
                .shellName((String) requireArg(v, env.known().assemblyShellName()))
                .build();
          } else if (t.equals(env.known().compose())) {
            config = ComposeConfig.<AnnotationMirror>builder()
                .source(a)
                .build();
          } else if (t.equals(env.known().configuration())) {
            config = ConfigurationConfig.<AnnotationMirror>builder()
                .source(a)
                .path((String) requireArg(v, env.known().configurationPath()))
                .absolute((boolean) requireArg(v, env.known().configurationAbsolute()))
                .build();
          } else if (t.equals(env.known().configurationPath())) {
            config = ConfigurationPrefixConfig.<AnnotationMirror>builder()
                .source(a)
                .value((String) requireArg(v, env.known().configurationPrefixValue()))
                .build();
          } else if (t.equals(env.known().extensionPointApi())) {
            config = ExtensionPointApiConfig.<AnnotationMirror>builder()
                .source(a)
                .build();
          } else if (t.equals(env.known().extensionPointProvision())) {
            config = ExtensionPointProvisionConfig.<AnnotationMirror>builder()
                .source(a)
                .build();
          } else if (t.equals(env.known().module())) {
            config = ModuleConfig.<AnnotationMirror>builder()
                .source(a)
                .build();
          } else if (t.equals(env.known().moduleMount())) {
            config = MountConfig.<AnnotationMirror>builder()
                .source(a)
                .external((boolean) requireArg(v, env.known().moduleMountExternal()))
                .build();
          } else if (t.equals(env.known().provision())) {
            config = ProvisionConfig.<AnnotationMirror>builder()
                .source(a)
                .shared((boolean) requireArg(v, env.known().provisionShared()))
                .override((boolean) requireArg(v, env.known().provisionOverride()))
                .build();
          }
          return Option(config);
        })
        .filter(Option::isDefined)
        .map(Option::get)
        .collect(LinkedHashSet.collector()));
  }

  private Object requireArg(Map<? extends ExecutableElement, ? extends AnnotationValue> values, ExecutableElement arg) {
    return Objects.requireNonNull(values.get(arg), "values.get(" + arg + ")").getValue();
  }

}
