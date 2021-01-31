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

package ch.raffael.meldioc.model;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import ch.raffael.meldioc.model.config.ElementConfig;
import ch.raffael.meldioc.model.config.ModelAnnotationType;
import ch.raffael.meldioc.model.config.MountConfig;
import ch.raffael.meldioc.model.config.ProvisionConfig;
import ch.raffael.meldioc.model.messages.Message;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.function.Function;

import static ch.raffael.meldioc.util.VavrX.touch;
import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;
import static java.util.function.Function.identity;

/**
 * A rich representation of a Meld model type.
 *
 * <p>This class also does all the analysis and validations and reports
 * errors to the model's {@link
 * ch.raffael.meldioc.model.messages.MessageSink MessageSink}. If there are
 * validation errors, it tries to keep the model as consistent as possible,
 * errors are only reported but won't cause building the model type to fail.
 *
 * <p>Consequently, its contents may cause a code generator to generate BS.
 * It's up to the code generator to watch the message sink for errors. A
 * generator will usually not generate any code if there are model errors.
 *
 * <p>OTOH, this way, we can collect as many errors as possible in one
 * analysis run.
 */
public final class ModelType<S, T> {

  private final Model<S, T> model;
  private final T type;
  private final CElement<S, T> element;

  private final Seq<ModelType<S, T>> superTypes;
  private final Seq<ModelType<S, T>> importedSuperTypes;
  private final Seq<ModelMethod<S, T>> allMethods;
  private final Seq<ModelMethod<S, T>> provisionMethods;
  private final Seq<ModelMethod<S, T>> extensionPointMethods;
  private final Seq<ModelMethod<S, T>> mountMethods;
  private final Seq<ModelMethod<S, T>> setupMethods;
  private final Seq<ModelMethod<S, T>> parameterMethods;

  public ModelType(Model<S, T> model, T type) {
    this.model = model;
    this.type = type;
    this.element = model.adaptor().classElement(type);
    Adaptor<S, T> adaptor = model.adaptor();
    var rawSuperTypes = adaptor.superTypes(type);
    this.superTypes = rawSuperTypes.map(Adaptor.SuperType::type).map(model::modelOf);
    this.importedSuperTypes = rawSuperTypes.filter(Adaptor.SuperType::imported)
        .map(Adaptor.SuperType::type).map(model::modelOf);
    if (element.configurationConfigOption().isDefined() || element.featureConfigOption().isDefined()) {
      // no need to check constructors for features, they might be extended
      validateExtendable(element.configurationConfigOption().isDefined(), element, none());
      validateImports();
    }
    Map<Tuple2<String, Seq<CElement<?, T>>>, Seq<ModelMethod<S, T>>> superMethods = superTypes
        .flatMap(cm -> cm.allMethods)
        .groupBy(m -> m.element().methodSignature())
        .mapValues(candidates -> candidates.sorted((left, right) -> {
          boolean lSubR = adaptor.isSubtypeOf(left.element().type(), right.element().type());
          boolean rSubR = adaptor.isSubtypeOf(right.element().type(), left.element().type());
          if (lSubR && rSubR) {
            return 0;
          } else if (lSubR) {
            return -1;
          } else if (rSubR) {
            return 1;
          } else {
            return 0;
          }
        }));
    Seq<ModelMethod<S, T>> declaredMethods = adaptor.declaredMethods(type)
        .map(m -> ModelMethod.of(m, this).withOverrides(superMethods.get(m.methodSignature()).getOrElse(List.empty())));
    validateClassAnnotations();
    declaredMethods.forEach(this::validateDeclaredMethodAnnotations);
    this.allMethods = findAllMethods(superMethods, declaredMethods);
    this.mountMethods = findMountMethods(adaptor);
    this.provisionMethods = findProvisionMethods();
    validateProvisionImplementationCandidates(model, adaptor);
    this.extensionPointMethods = findExtensionPointMethods(model);
    this.parameterMethods = findParameterMethods(model, adaptor);
    this.setupMethods = findSetupMethods(model, adaptor);
  }

  private Seq<ModelMethod<S, T>> findAllMethods(Map<Tuple2<String, Seq<CElement<?, T>>>, Seq<ModelMethod<S, T>>> superMethods, Seq<ModelMethod<S, T>> declaredMethods) {
    return declaredMethods
        .appendAll(superMethods
            .filter(sm -> !declaredMethods.exists(dm -> dm.element().methodSignature().equals(sm._1)))
            .map(Tuple2::_2)
            .map(sm -> sm.size() == 1
                       ? sm.head()
                       : ModelMethod.of(sm.head().element(), this)
                           .withImplied(true)
                           .withOverrides(sm)))
        .filter(m1 -> {
          boolean include = true;
          //noinspection ConstantConditions
          include &= validateObjectMethods(m1);
          include &= excludeStaticMethods(m1);
          include &= validateConflictingSuperCompositionRoles(m1);
          if (element.configs().exists(c1 -> c1.type().annotationType().equals(Configuration.class))) {
            include &= validateAbstractMethodImplementable(element, m1);
          }
          if (!m1.element().configs().isEmpty()) {
            include &= validateOverridableMethod(m1);
            include &= validateMethodAccessibility(element, m1, false);
            include &= validateProvisionOverrides(m1);
          }
          return include;
        });
  }

  private Seq<ModelMethod<S, T>> findMountMethods(Adaptor<S, T> adaptor) {
    return this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Feature.Mount.class)))
        .map(touch(m ->
            model.modelOf(m.element().type()).allMethods()
                .filter(mm -> validateAbstractMethodImplementable(m.element(), mm))
                .filter(mm -> mm.element().configs().exists(c -> c.type().role()))
                .forEach(mm -> validateMethodAccessibility(m.element(), mm, true))
        )).appendAll(
            element.configurationConfigOption()
                .map(mc -> synthesizeMountMethods(mc.mount(), adaptor, mc.source()))
                .getOrElse(List.empty()))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .map(touch(m -> {
          if (!m.element().isAbstract()) {
            model.message(Message.mountMethodMustBeAbstract(m.element()));
          }
        }))
        .map(touch(m -> {
          var ret = adaptor.classElement(m.element().type());
          if (adaptor.classElement(m.element().type()).isInnerClass()) {
            model.message(Message.illegalInnerClass(m.element(), ret));
          }
        }))
        .filter(m -> {
          if (element.configurationConfigOption().isEmpty()) {
            model.message(Message.mountMethodsAllowedInConfigurationsOnly(m.element()));
            return false;
          } else {
            return true;
          }
        })
        .filter(m -> {
          CElement<S, T> cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Feature.class) || t.equals(Configuration.class))) {
            model.message(Message.mountMethodMustReturnFeature(m.element(), cls));
            return false;
          }
          return true;
        })
        .map(touch(m -> {
          if (!m.element().mountConfig().injected()) {
            validateExtendable(true, model.adaptor().classElement(m.element().type()), some(m.element()));
          }
        }));
  }

  private Seq<ModelMethod<S, T>> synthesizeMountMethods(Seq<ClassRef> classRefs, Adaptor<S, T> adaptor, S source) {
    return classRefs
        .filter(cr -> {
          var t = adaptor.typeOf(cr);
          if (adaptor.hasTypeParameters(t)) {
            model.message(Message.mountAttributeClassMustNotBeParametrized(element, adaptor.classElement(t)));
            return false;
          } else {
            return true;
          }
        })
        .map(cr -> ModelMethod.<S, T>builder()
            .implied(true)
            .modelType(this)
            .element(CElement.<S, T>builder()
                .synthetic(true)
                .parent(element)
                .source(source)
                .kind(CElement.Kind.METHOD)
                .name("synthetic$$" + cr.canonicalName().replace('.', '$'))
                .type(adaptor.typeOf(cr))
                .accessPolicy(AccessPolicy.LOCAL)
                .isStatic(false)
                .isFinal(false)
                .isAbstract(true)
                .addConfigs(MountConfig.<S>builder()
                    .injected(false)
                    .source(source)
                    .build())
                .build())
            .build());
  }

  private Seq<ModelMethod<S, T>> findProvisionMethods() {
    return this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .map(m -> mapToMounts(m, ModelType::provisionMethods))
        .filter(this::validateThrows);
  }

  private Seq<ModelMethod<S, T>> findExtensionPointMethods(Model<S, T> model) {
    return this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateThrows)
        .map(touch(m -> {
          CElement<S, T> cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.Acceptor.class))) {
            model.message(Message.extensionPointAcceptorReturnRecommended(m.element(), cls));
          }
        }))
        .map(m -> mapToMounts(m, ModelType::extensionPointMethods));
  }

  private Seq<ModelMethod<S, T>> findParameterMethods(Model<S, T> model, Adaptor<S, T> adaptor) {
    return this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Parameter.class)))
        .filter(this::validateNoParameters)
        .map(touch(m -> {
          if (!model.configType().isDefined() && m.element().isAbstract()) {
            model.message(Message.typesafeConfigNotOnClasspath(m.element()));
          } else if (m.element().parameterConfig().value().equals(Parameter.ALL)) {
            if (!adaptor.isSubtypeOf(model.configType().get(), m.element().type())) {
              model.message(Message.configTypeNotSupported(m.element()));
            }
          } else {
            if (model.configSupportedTypeOption(m.element().type()).isEmpty()) {
              model.message(Message.configTypeNotSupported(m.element()));
            }
          }
        }))
//        .filter(this::validateReferenceType)
        .appendAll(collectMounted(ModelType::parameterMethods));
  }

  private Seq<ModelMethod<S, T>> findSetupMethods(Model<S, T> model, Adaptor<S, T> adaptor) {
    return this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Setup.class)))
        .map(touch(m -> {
          if (!adaptor.isNoType(m.element().type())) {
            model.message(Message.returnValueIgnored(m.element()));
          }
        }))
        .appendAll(collectMounted(ModelType::setupMethods))
        .map(element.configs().exists(c -> c.type().annotationType().equals(Configuration.class))
             ? m -> m.withArguments(mapSetupParameters(m))
             : identity());
  }

  private ModelMethod<S, T> mapToMounts(ModelMethod<S, T> method, Function<ModelType<S, T>, Seq<ModelMethod<S, T>>> mounted) {
    if (element.configurationConfigOption().isEmpty() || !method.element().isAbstract() || method.via().isDefined()) {
      return method;
    }
    Seq<Tuple2<ModelMethod<S, T>, ModelMethod<S, T>>> forwards = mountMethods
        .map(m -> Tuple.of(m, model.modelOf(m.element().type())))
        .map(tpl -> tpl.map2(mounted))
        .flatMap(tpl -> tpl._2().map(m -> Tuple.of(tpl._1(), m.withVia(tpl._1()))))
        .filter(tpl -> tpl._1().element().mountConfig().injected() || !tpl._2().element().isAbstract())
        .filter(tpl -> tpl._2().element().name().equals(method.element().name()))
        .filter(tpl -> tpl._2().element().canOverride(method.element(), model.adaptor()));
    if (forwards.isEmpty()) {
      model.message(Message.unresolvedProvision(element, method.element()));
      return method;
    } else if (forwards.size() > 1) {
      model.message(Message.conflictingProvisions(
          method.element().withSource(element.source()), forwards.map(tp -> tp._2().element())));
      return method;
    } else {
      return method.withVia(forwards.head()._1());
    }
  }

  private void validateExtendable(boolean checkConstructors, CElement<S, T> type, Option<CElement<S, T>> from) {
    if (type.isInnerClass()) {
      model.message(Message.illegalInnerClass(type));
      checkConstructors = false;
    }
    var outer = element.findOutermost();
    for (var c = type; c.parentOption().isDefined(); c = c.parent()) {
      if (c.accessPolicy() == AccessPolicy.PRIVATE || !c.accessibleTo(model.adaptor(), outer)) {
        model.message(Message.elementNotAccessible(element, type));
        checkConstructors = false;
      }
    }
    if (checkConstructors && !model.adaptor().isInterface(type.type())) {
      var ctors = model.adaptor().constructors(type.type());
      if (ctors.isEmpty()) {
        ctors = List.of(CElement.<S, T>builder()
            .kind(CElement.Kind.METHOD)
            .parent(type)
            .name(CElement.CONSTRUCTOR_NAME)
            .type(model.adaptor().noType())
            .accessPolicy(type.accessPolicy()).isStatic(false).isFinal(false).isAbstract(false)
            .parameters(List.empty())
            .source(type.source())
            .synthetic(true)
            .build());
      }
      ctors.find(e -> e.parameters().isEmpty())
          .filter(c -> c.accessPolicy() != AccessPolicy.PRIVATE)
          .filter(c -> c.accessibleTo(model.adaptor(), outer))
          .onEmpty(() -> model.message(from.fold(
              () -> Message.missingNoArgsConstructor(type),
              m -> Message.missingNoArgsConstructor(m, type))));
    }
  }

  private void validateImports() {
    superTypes
        .filter(t -> t.element().featureConfigOption().isEmpty())
        .filter(t -> t.element().configurationConfigOption().isEmpty())
        .forEach(t -> {
          if (!isImported(t)) {
            model.message(Message.missingFeatureImportAnnotation(element, t.element()));
          }
        });
  }

  private void validateProvisionImplementationCandidates(Model<S, T> model, Adaptor<S, T> adaptor) {
    mountMethods
        .filter(m -> !m.element().mountConfig().injected())
        .map(touch(m -> model.modelOf(m.element().type())
        .provisionMethods()
        .filter(mp -> mp.element().isAbstract())
        .forEach(mp -> {
          if (!provisionMethods
              .filter(p -> !p.element().isAbstract() || p.via().isDefined())
              .exists(p -> p.element().canOverride(mp.element(), adaptor))) {
            model.message(Message.mountedAbstractProvisionHasNoImplementationCandidate(m.element(), mp.element()));
          }
        })));
  }

  private void validateClassAnnotations() {
    if (!element.configs().exists(c -> c.type().featureRole()) && element.configs().exists(c -> !c.type().role())) {
      model.message(Message.meldAnnotationOutsideFeature(element()));
    }
  }

  private void validateDeclaredMethodAnnotations(ModelMethod<S, T> method) {
    if (!element.configs().exists(c -> c.type().featureRole()) && !method.element().configs().isEmpty()) {
      model.message(Message.meldAnnotationOutsideFeature(method.element()));
    }
    if (method.element().configs().count(c -> c.type().role()) > 1) {
      model.message(Message.conflictingCompositionRoles(method.element(), List.empty()));
    }
  }

  private boolean validateObjectMethods(ModelMethod<S, T> method) {
    if (model.objectMethods().exists(om -> method.element().canOverride(om, model.adaptor()))) {
      if (!method.element().configs().isEmpty()) {
        method.addMessage(model, Message.objectOverride(method.element()));
      }
      return false;
    }
    return true;
  }

  private boolean excludeStaticMethods(ModelMethod<S, T> method) {
    return !method.element().isStatic();
  }

  private boolean validateOverridableMethod(ModelMethod<S, T> method) {
    if (!method.element().isOverridable()) {
      method.addMessage(model, Message.nonOverridableMethod(method.element()));
    }
    return true;
  }

  private boolean validateConflictingSuperCompositionRoles(ModelMethod<S, T> method) {
    // TODO (2019-04-22) check the whole override tree to identify "holes"
    Seq<ModelMethod<S, T>> conflicts = method.overrides()
        .filter(s -> !s.element().configs().isEmpty())
        .filter(s -> !method.element().configs().map(ElementConfig::type)
            .equals(s.element().configs().map(ElementConfig::type)));
    if (!conflicts.isEmpty()) {
      model.message(Message.conflictingOverride(method.element(), conflicts.map(ModelMethod::element)));
    }
    return true;
  }

  private boolean validateMethodAccessibility(
      CElement<S, T> nonLocalMsgTarget, ModelMethod<S, T> method, boolean forOverride) {
    if (method.element().parent().equals(element)) {
      method.overrides()
          //.filter(s -> s.element().isOverridable()) // already checked and reported
          .forEach(s -> {
            if (!s.element().accessibleTo(model.adaptor(), method.element())) {
              model.message(Message.elementNotAccessible(method.element(), s.element()));
            }
          });
    } else if (!method.element().accessibleTo(model.adaptor(), element)) {
      if (!(forOverride && method.element().accessPolicy() == AccessPolicy.PROTECTED)) {
        model.message(Message.elementNotAccessible(nonLocalMsgTarget, method.element()));
      }
    }
    return true;
  }

  @SuppressWarnings("Convert2MethodRef")
  private boolean validateProvisionOverrides(ModelMethod<S, T> method) {
    method.element().configs()
        .filter(c -> c.type().annotationType().equals(Provision.class))
        .map(ProvisionConfig.class::cast)
        .filter(c -> !c.singleton() && !c.override())
        .headOption().forEach(__ -> method.overrides()
        .filter(s -> s.element().configs()
            .filter(c -> c.type().annotationType().equals(Provision.class))
            .map(ProvisionConfig.class::cast)
            .exists(c -> c.singleton())) // method reference causes a rawtypes warning
        .headOption()
        .forEach(s -> {
          if (method.element().parent().equals(element)) {
            model.message(Message.provisionOverrideMissing(method.element(), s.element()));
          } else {
            model.message(Message.conflictingProvisions(element, List.of(method.element(), s.element())));
          }
        }));
    return true;
  }

  private boolean validateNoParameters(ModelMethod<S, T> method) {
    if (!method.element().parameters().isEmpty()) {
      model.message(Message.noParametersAllowed(method.element()));
    }
    return true;
  }

  private boolean validateReferenceType(ModelMethod<S, T> method) {
    if (!model.adaptor().isReference(method.element().type())) {
      // TODO (2019-04-07) support primitive types?
      model.message(Message.mustReturnReference(method.element()));
    }
    return true;
  }

  private boolean validateThrows(ModelMethod<S, T> method) {
    method.via()
        .flatMap(v -> model.modelOf(v.element().type()).allProvisionMethods().find(
            m -> m.element().methodSignature().equals(method.element().methodSignature())))
        .forEach(
            v -> v.exceptions()
            .reject(e -> model.adaptor().isSubtypeOf(e, model.runtimeExceptionType())
                || model.adaptor().isSubtypeOf(e, model.errorType())
                || method.exceptions().exists(pe -> model.adaptor().isSubtypeOf(e, pe)))
            .forEach(e -> model.message(Message.incompatibleThrowsClause(
                method.via().get().element(), method.element(), model.adaptor().classElement(e)))));
    return true;
  }

  private boolean validateAbstractMethodImplementable(CElement<S, T> classElem, ModelMethod<S, T> m) {
    if (!m.element().isAbstract()) {
      return true;
    }
    if (!m.element().configs().exists(c -> c.type().willImplement())) {
      if (classElem.equals(element)) {
        model.message(Message.abstractMethodWillNotBeImplemented(m.element(), m.element()));
      } else {
        model.message(Message.abstractMethodWillNotBeImplemented(classElem, m.element()));
      }
    }
    return true;
  }

  private Seq<Either<ModelMethod<S, T>, BuiltinArgument>> mapSetupParameters(ModelMethod<S, T> method) {
    return method.element().parameters().map(param -> {
      Seq<Either<ModelMethod<S, T>, BuiltinArgument>> candidates = Vector.ofAll(model.configType()
          .filter(t -> model.adaptor().isSubtypeOf(t, param.type()))
          .map(__ -> BuiltinArgument.CONFIG.argument()));
      candidates = candidates.appendAll(
          extensionPointMethods
              .filter(epp -> model.adaptor().isSubtypeOf(epp.element().type(), param.type()))
              .map(Either::left));
      candidates = candidates.appendAll(mountMethods
          .map(m -> Tuple.of(m, model.modelOf(m.element().type()).extensionPointMethods()))
          .flatMap(tplViaEpp -> tplViaEpp._2().map(m -> Tuple.of(tplViaEpp._1(), m)))
          .filter(tplViaEpp -> model.adaptor().isSubtypeOf(tplViaEpp._2().element().type(), param.type()))
          .map(tplViaEpp -> tplViaEpp._2().withVia(tplViaEpp._1()))
          .map(Either::left));
      if (candidates.isEmpty()) {
        CElement<S, T> epType = model.adaptor().classElement(param.type());
        model.message(method.via()
            .map(via -> Message.unresolvedExtensionPoint(via.element(), param, epType))
            .getOrElse(() -> method.element().parentOption().eq(Option.of(element))
                             ? Message.unresolvedExtensionPoint(param, epType)
                             : Message.unresolvedExtensionPoint(element, param, epType)));
        return BuiltinArgument.NONE.argument();
      } else if (candidates.size() > 1) {
        model.message(Message.conflictingExtensionPoints(param,
            candidates.filter(Either::isLeft).map(c -> c.getLeft().element())));
        return BuiltinArgument.NONE.argument();
      } else {
        return candidates.head();
      }
    });
  }

  private Seq<ModelMethod<S, T>> collectMounted(Function<ModelType<S, T>, Seq<ModelMethod<S, T>>> mounted) {
    return mountMethods
        .flatMap(mount -> mounted.apply(model.modelOf(mount.element().type())).map(m -> m.withVia(mount)))
        .filter(m -> {
          boolean callable = m.element().accessibleTo(model.adaptor(), element)
              || m.element().accessPolicy() == AccessPolicy.PROTECTED;
          if (!callable) {
            model.message(Message.elementNotAccessible(m.element(), element));
          }
          return callable;
        });
  }

  public Model<S, T> model() {
    return model;
  }

  public T type() {
    return type;
  }

  public CElement<S, T> element() {
    return element;
  }

  public Seq<ModelType<S, T>> superTypes() {
    return superTypes;
  }

  public boolean isImported(ModelType<S, T> type) {
    return type.type().equals(model.objectType())
        || importedSuperTypes.exists(i -> model().adaptor().isSubtypeOf(i.type(), type.type()));
  }

  public Seq<ModelMethod<S, T>> allMethods() {
    return allMethods;
  }

  public Seq<ModelMethod<S, T>> provisionMethods() {
    return provisionMethods;
  }

  public Seq<ModelMethod<S, T>> extensionPointMethods() {
    return extensionPointMethods;
  }

  public Seq<ModelMethod<S, T>> allProvisionMethods() {
    return provisionMethods.appendAll(extensionPointMethods);
  }

  public Seq<ModelMethod<S, T>> mountMethods() {
    return mountMethods;
  }

  public Seq<ModelMethod<S, T>> setupMethods() {
    return setupMethods;
  }

  public Seq<ModelMethod<S, T>> parameterMethods() {
    return parameterMethods;
  }

  @Override
  public String toString() {
    return "ModelType{"
        + "type=" + type
        + ",roles=" + element.configs()
        .map(ElementConfig::type)
        .filter(ModelAnnotationType::role)
        .map(t -> t.annotationType().getSimpleName())
        .mkCharSeq("[", ",", "]")
        + '}';
  }
}
