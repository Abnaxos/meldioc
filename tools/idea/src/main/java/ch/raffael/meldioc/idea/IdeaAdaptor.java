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
import ch.raffael.meldioc.model.AccessPolicy;
import ch.raffael.meldioc.model.Adaptor;
import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.model.config.ConfigurationConfig;
import ch.raffael.meldioc.model.config.DependsOnConfig;
import ch.raffael.meldioc.model.config.ExtensionPointAcceptorConfig;
import ch.raffael.meldioc.model.config.ExtensionPointConfig;
import ch.raffael.meldioc.model.config.FeatureConfig;
import ch.raffael.meldioc.model.config.MountConfig;
import ch.raffael.meldioc.model.config.ParameterConfig;
import ch.raffael.meldioc.model.config.ParameterPrefixConfig;
import ch.raffael.meldioc.model.config.ProvisionConfig;
import ch.raffael.meldioc.model.config.SetupConfig;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.intellij.lang.jvm.JvmAnnotation;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.types.JvmArrayType;
import com.intellij.lang.jvm.types.JvmPrimitiveTypeKind;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import io.vavr.Tuple;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Option;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;
import static java.util.Objects.nonNull;

@SuppressWarnings("UnstableApiUsage")
public class IdeaAdaptor implements Adaptor<PsiElement, PsiType> {

  private static final Logger LOG = Logger.getInstance(IdeaAdaptor.class);

  private final BiMap<ClassRef, PsiType> PRIMITIVE_MAPPINGS =
      ImmutableBiMap.<ClassRef, PsiType>builder()
          .put(ClassRef.Primitives.INT, PsiType.INT)
          .put(ClassRef.Primitives.LONG, PsiType.LONG)
          .put(ClassRef.Primitives.SHORT, PsiType.SHORT)
          .put(ClassRef.Primitives.BYTE, PsiType.BYTE)
          .put(ClassRef.Primitives.DOUBLE, PsiType.DOUBLE)
          .put(ClassRef.Primitives.FLOAT, PsiType.FLOAT)
          .put(ClassRef.Primitives.BOOLEAN, PsiType.BOOLEAN)
          .put(ClassRef.Primitives.CHAR, PsiType.CHAR)
          .put(ClassRef.Primitives.VOID, PsiType.VOID)
          .build();

  private final JavaPsiFacade javaPsiFacade;
  private final GlobalSearchScope searchScope;

  private final PsiElement dummyElement;
  private final SrcElement<PsiElement, PsiType> noTypeSrcElement;

  public IdeaAdaptor(JavaPsiFacade javaPsiFacade, GlobalSearchScope searchScope, PsiElement dummyElement) {
    this.javaPsiFacade = javaPsiFacade;
    this.searchScope = searchScope;
    this.dummyElement = dummyElement;
    noTypeSrcElement = SrcElement.<PsiElement, PsiType>builder()
        .kind(SrcElement.Kind.CLASS)
        .source(dummyElement)
        .type(NoType.INSTANCE)
        .name(dummyElement.getText())
        .accessPolicy(AccessPolicy.PRIVATE)
        .isStatic(false)
        .isAbstract(false)
        .isFinal(false)
        .build();
  }

  @Override
  public boolean isSubtypeOf(PsiType left, PsiType right) {
    if (!left.isValid() || !right.isValid()) {
      return false;
    } else if (left instanceof PsiPrimitiveType) {
      return left.equals(right);
    } else if (right instanceof PsiPrimitiveType) {
      return false;
    } else {
      return right.isAssignableFrom(left);
    }
  }

  @Override
  public boolean isReference(PsiType type) {
    return type.isValid() && (type instanceof JvmReferenceType) || (type instanceof JvmArrayType);
  }

  @Override
  public boolean isInterface(PsiType type) {
    if (type instanceof PsiClass) {
      return ((PsiClass) type).isInterface();
    } else {
      return false;
    }
  }

  @Override
  public boolean hasTypeParameters(PsiType type) {
    if (isNoType(type)) {
      return false;
    }
    return Option.of(PsiTypesUtil.getPsiClass(type))
        .flatMap(t -> Option.of(t.getTypeParameterList()))
        .map(pl -> pl.getTypeParameters().length > 0)
        .getOrElse(false);
  }

  @Override
  public boolean isPrimitive(PsiType type) {
    return type.isValid() && (type instanceof PsiPrimitiveType)
        && !((PsiPrimitiveType) type).getKind().equals(JvmPrimitiveTypeKind.VOID);
  }

  @Override
  public boolean isEnumType(PsiType type) {
    if (type instanceof PsiClassType) {
      return Option.of(((PsiClassType) type).resolve()).map(PsiClass::isEnum).getOrElse(false);
    } else {
      return false;
    }
  }

  @Override
  public PsiType typeOf(ClassRef ref) {
    if (ref.isPrimitive()) {
      return PRIMITIVE_MAPPINGS.getOrDefault(ref, NoType.INSTANCE);
    }
    return Option.of(javaPsiFacade.findClass(ref.canonicalName(), searchScope))
        .map(c -> (PsiType) PsiTypesUtil.getClassType(c))
        .getOrElse(NoType.INSTANCE);
  }

  @Override
  public SrcElement<PsiElement, PsiType> classElement(PsiType type) {
    return Option.of(type)
        .filter(PsiClassType.class::isInstance).map(PsiClassType.class::cast)
        .flatMap(c -> Option.of(c.resolve()))
        .map(t -> classElement(t, none()))
        .getOrElse(() -> noTypeSrcElement);
  }

  @Override
  public Seq<SuperType<PsiType>> superTypes(PsiType type) {
    return Vector.ofAll(Stream.of(type.getSuperTypes()))
        .map(t -> new SuperType<>(t,
            t.findAnnotation(Feature.Import.class.getCanonicalName()) != null,
            t.findAnnotation(Feature.DependsOn.class.getCanonicalName()) != null));
  }

  @Override
  public Seq<SrcElement<PsiElement, PsiType>> declaredMethods(PsiType type) {
    return executables(type, c -> io.vavr.collection.List.of(c.getMethods()).filter(m -> !m.isConstructor()));
  }

  @Override
  public Seq<SrcElement<PsiElement, PsiType>> constructors(PsiType type) {
    return executables(type, c -> io.vavr.collection.List.of(c.getConstructors()))
        .map(m -> m.withName(SrcElement.CONSTRUCTOR_NAME).withType(NoType.INSTANCE));
  }

  private Seq<SrcElement<PsiElement, PsiType>> executables(
      PsiType type, Function<? super PsiClass, ? extends Seq<? extends  PsiMethod>> methodsFun) {
    return Option.of(type)
        .filter(PsiClassType.class::isInstance).map(PsiClassType.class::cast)
        .map(c -> Tuple.of(c, Option.of(c.resolveGenerics()).map(JavaResolveResult::getSubstitutor)))
        .map(tpl -> tpl.map1(PsiClassType::resolve))
        .filter(tpl -> nonNull(tpl._1))
        .map(tpl -> tpl.append(methodsFun.apply(tpl._1)))
        .map(tpl -> tpl.apply((c, substitutor, methods) -> methods.map(m -> methodElement(c, m, substitutor))))
        .getOrElse(io.vavr.collection.List.empty());
  }

  @Override
  public String packageOf(SrcElement<PsiElement, PsiType> element) {
    return Option.of((PsiClass) PsiTreeUtil.findFirstParent(element.source(), false, PsiClass.class::isInstance))
        .map(PsiUtil::getPackageName)
        .getOrElse("");
  }


  @Override
  public PsiType iterableOf(PsiType componentType) {
    return substituteFirstTypeParameter(Iterable.class, componentType);
  }

  @Override
  public PsiType collectionOf(PsiType componentType) {
    return substituteFirstTypeParameter(Collection.class, componentType);
  }

  @Override
  public PsiType listOf(PsiType componentType) {
    return substituteFirstTypeParameter(java.util.List.class, componentType);
  }

  @Override
  public PsiType componentTypeOfIterable(PsiType iterableType) {
    if (iterableType instanceof PsiClassType) {
      return psiClassFor(Iterable.class)
          .filter(c -> c.getTypeParameters().length == 1)
          .map(c -> Tuple.of(PsiTypesUtil.getClassType(c), c.getTypeParameters()[0]))
          .filter(tpl -> tpl._1.isAssignableFrom(iterableType))
          .map(tpl -> ((PsiClassType) iterableType).resolveGenerics().getSubstitutor().substitute(tpl._2))
          .getOrElse(NoType.INSTANCE);
    } else {
      return NoType.INSTANCE;
    }
  }

  @Override
  public PsiType noType() {
    return NoType.INSTANCE;
  }

  private Option<PsiClass> psiClassFor(Class<?> javaClass) {
    return Option.of(javaPsiFacade.findClass(javaClass.getCanonicalName(), searchScope));
  }

  private PsiType substituteFirstTypeParameter(Class<?> javaClass, PsiType typeArg) {
    return psiClassFor(javaClass)
        .map(c -> substituteFirstTypeParameter(c, typeArg))
        .getOrElse(NoType.INSTANCE);
  }

  private PsiType substituteFirstTypeParameter(PsiClass aClass, PsiType typeArg) {
    PsiClassType raw = PsiTypesUtil.getClassType(aClass);
    return Option.of(aClass.getTypeParameterList())
        .map(PsiTypeParameterList::getTypeParameters)
        .filter(p -> p.length == 1)
        .map(p -> raw.resolveGenerics().getSubstitutor().put(p[0], typeArg))
        .map(s -> s.substitute(raw))
        .getOrElse(NoType.INSTANCE);
  }

  private SrcElement<PsiElement, PsiType> classElement(PsiClass aClass, Option<PsiSubstitutor> substitutor) {
    return commonElement(aClass, elementBuilder(SrcElement.Kind.CLASS)
        .source(aClass)
        .type(Option.of((PsiType) PsiTypesUtil.getClassType(aClass))
            .map(t -> substituteType(substitutor, t))
            .getOrElse(NoType.INSTANCE))
        // TODO (2019-04-19) I probably need a new substitutor
        .parentOption(Option.of(aClass.getContainingClass()).map(p -> classElement(p, substitutor)))
        .name(Option.of(aClass.getName()).getOrElse("$Unnamed")))
        .build();
  }

  private SrcElement<PsiElement, PsiType> methodElement(PsiClass enclosing, PsiMethod method,
                                                        Option<PsiSubstitutor> substitutor) {
    return commonElement(method, elementBuilder(SrcElement.Kind.METHOD)
        .source(method)
        .type(Option.of(method.getReturnType())
            .map(t -> substituteType(substitutor, t))
            .getOrElse(NoType.INSTANCE))
        // TODO FIXME (2019-04-19) what do I do with that stupid substitutor?
        .parent(classElement(enclosing, substitutor))
        .name(method.getName()))
        .parameters(Array.of(method.getParameterList().getParameters())
            .zip(Array.range(1, method.getParameterList().getParametersCount() + 1))
            .map(pi -> parameterElement(pi._1, pi._2, substitutor)))
        .exceptions(List.of(method.getThrowsList().getReferencedTypes()))
        .build();
  }

  private SrcElement<PsiElement, PsiType> parameterElement(PsiParameter parameter, int index, Option<PsiSubstitutor> substitutor) {
    return commonElement(parameter,
        elementBuilder(SrcElement.Kind.PARAMETER)
            .source(parameter)
            .type(substituteType(substitutor, parameter.getType()))
            .name(Option.of(parameter.getName()).getOrElse(() -> "arg" + index)))
        .build();
  }

  private SrcElement.Builder<PsiElement, PsiType> elementBuilder(SrcElement.Kind kind) {
    return SrcElement.<PsiElement, PsiType>builder().kind(kind);
  }

  private SrcElement.Builder<PsiElement, PsiType> commonElement(PsiModifierListOwner element, SrcElement.Builder<PsiElement, PsiType> builder) {
    if (element.hasModifier(JvmModifier.PUBLIC)) {
      builder.accessPolicy(AccessPolicy.PUBLIC);
    } else if (element.hasModifier(JvmModifier.PROTECTED)) {
      builder.accessPolicy(AccessPolicy.PROTECTED);
    } else if (element.hasModifier(JvmModifier.PRIVATE)) {
      builder.accessPolicy(AccessPolicy.PRIVATE);
    } else {
      builder.accessPolicy(AccessPolicy.LOCAL);
    }
    builder.isStatic(element.hasModifier(JvmModifier.STATIC));
    builder.isAbstract(element.hasModifier(JvmModifier.ABSTRACT));
    builder.isFinal(element.hasModifier(JvmModifier.FINAL) || element.hasModifier(JvmModifier.NATIVE));
    return loadConfigurations(element, builder);
  }

  private SrcElement.Builder<PsiElement, PsiType> loadConfigurations(PsiModifierListOwner element, SrcElement.Builder<PsiElement, PsiType> builder) {
    for (PsiAnnotation a : element.getAnnotations()) {
      if (isOfType(a, Configuration.class)) {
        try {
          var confBuilder = ConfigurationConfig.<PsiElement>builder()
              .source(a)
              .shellName(annotationValue(a, ConfigurationConfig.SHELL_NAME, String.class))
              .packageLocal(annotationValue(a, ConfigurationConfig.PACKAGE_LOCAL, Boolean.class));
          Option.of(a.findAttributeValue(ConfigurationConfig.MOUNT))
              .<Seq<ClassRef>>map(v -> {
                if (v instanceof PsiClassObjectAccessExpression) {
                  return toClassRef(((PsiClassObjectAccessExpression) v).getOperand().getType())
                      .map(io.vavr.collection.List::of).getOrElse(io.vavr.collection.List.empty());
                } else if (v instanceof PsiArrayInitializerMemberValue) {
                  return List.of(((PsiArrayInitializerMemberValue) v).getInitializers())
                      .filter(PsiClassObjectAccessExpression.class::isInstance)
                      .map(c -> ((PsiClassObjectAccessExpression) c).getOperand().getType())
                      .flatMap(this::toClassRef);
                } else {
                  return List.empty();
                }
              })
              .forEach(confBuilder::mount);
          builder.addConfigs(confBuilder.build());
        } catch (AnnotationValueNotAvailableException e) {
          e.handle();
        }
      } else if (isOfType(a, ExtensionPoint.Acceptor.class)) {
        builder.addConfigs(ExtensionPointAcceptorConfig.<PsiElement>builder()
            .source(a)
            .build());
      } else if (isOfType(a, ExtensionPoint.class)) {
        builder.addConfigs(ExtensionPointConfig.<PsiElement>builder()
            .source(a)
            .build());
      } else if (isOfType(a, Feature.class)) {
        builder.addConfigs(FeatureConfig.<PsiElement>builder()
            .source(a)
            .build());
      } else if (isOfType(a, Feature.Mount.class)) {
        try {
          builder.addConfigs(MountConfig.<PsiElement>builder()
              .source(a)
              .injected(annotationValue(a, MountConfig.INJECTED, Boolean.class))
              .build());
        } catch (AnnotationValueNotAvailableException e) {
          e.handle();
        }
      } else if (isOfType(a, Feature.DependsOn.class)) {
        builder.addConfigs(DependsOnConfig.<PsiElement>builder()
            .source(a)
            .build());
      } else if (isOfType(a, Parameter.class)) {
        try {
          builder.addConfigs(ParameterConfig.<PsiElement>builder()
              .source(a)
              .value(annotationValue(a, ParameterConfig.VALUE, String.class))
              .absolute(annotationValue(a, ParameterConfig.ABSOLUTE, Boolean.class))
              .build());
        } catch (AnnotationValueNotAvailableException e) {
          e.handle();
        }
      } else if (isOfType(a, Parameter.Prefix.class)) {
        try {
          builder.addConfigs(ParameterPrefixConfig.<PsiElement>builder()
              .source(a)
              .value(annotationValue(a, ParameterPrefixConfig.VALUE, String.class))
              .build());
        } catch (AnnotationValueNotAvailableException e) {
          e.handle();
        }
      } else if (isOfType(a, Provision.class)) {
        try {
          builder.addConfigs(ProvisionConfig.<PsiElement>builder()
              .source(a)
              .singleton(annotationValue(a, ProvisionConfig.SINGLETON, Boolean.class)
                  || annotationValue(a, ProvisionConfig.SHARED, Boolean.class))
              .override(annotationValue(a, ProvisionConfig.OVERRIDE, Boolean.class))
              .build());
        } catch (AnnotationValueNotAvailableException e) {
          e.handle();
        }
      } else if (isOfType(a, Setup.class)) {
        builder.addConfigs(SetupConfig.<PsiElement>builder()
            .source(a)
            .build());
      }
    }
    return builder;
  }

  private boolean isOfType(JvmAnnotation annotation, Class<? extends Annotation> type) {
    return type.getName().replace('$', '.').equals(annotation.getQualifiedName());
  }

  private <T> T annotationValue(PsiAnnotation a, String n, Class<T> type) throws AnnotationValueNotAvailableException {
    return Option.of(a.findAttributeValue(n))
        .flatMap(v -> {
          if (v instanceof PsiLiteralExpression) {
            return Option.of(((PsiLiteralExpression) v).getValue());
          } else {
            return Option.of(javaPsiFacade.getConstantEvaluationHelper().computeConstantExpression(v));
          }
        })
        .filter(type::isInstance)
        .map(type::cast)
        .getOrElseThrow(() -> new AnnotationValueNotAvailableException("No attribute '" + n + "' found in " + a));
  }

  private PsiType substituteType(Option<PsiSubstitutor> substitutor, PsiType type) {
    return substitutor.map(s -> s.substitute(type)).getOrElse(type);
  }

  private Option<ClassRef> toClassRef(PsiType psiType) {
    Option<ClassRef> baseRef = none();
    if (!psiType.isValid()) {
      return none();
    }
    if (psiType instanceof PsiPrimitiveType) {
      baseRef = Option.of(PRIMITIVE_MAPPINGS.inverse().get(psiType));
    } else if (psiType instanceof PsiClassType) {
      var c = PsiTypesUtil.getPsiClass(psiType);
      if (c == null) {
        return none();
      }
      if (PsiTreeUtil.findFirstParent(c, true, PsiMethod.class::isInstance) != null) {
        // local classes not supported, it's syntactically impossible anyway, but you never know in IDEA ...
        return none();
      }
      var name = new StringBuilder();
      while (true) {
        if (c.getName() == null) {
          // anonymous classes not supported, it's syntactically impossible anyway, but you never know in IDEA ...
          return none();
        }
        if (name.length() > 0) {
          name.insert(0, '.');
        }
        name.insert(0, c.getName());
        var outer = PsiTreeUtil.findFirstParent(c, true, PsiClass.class::isInstance);
        if (outer == null) {
          break;
        } else {
          c = (PsiClass) outer;
        }
      }
      baseRef = some(ClassRef.of(
          Option.of(c.getContainingFile())
              .filter(PsiClassOwner.class::isInstance).map(PsiClassOwner.class::cast)
              .map(PsiClassOwner::getPackageName)
              .getOrElse(""),
          name.toString()));
    }
    return baseRef.map(ref -> ref.withArrayDimensions(psiType.getArrayDimensions()));
  }

  static class AnnotationValueNotAvailableException extends Exception {
    public AnnotationValueNotAvailableException(String message) {
      super(message);
    }

    public void handle() {
      LOG.debug(this);
    }
  }
}
