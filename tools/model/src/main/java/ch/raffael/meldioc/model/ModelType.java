/*
 *  Copyright (c) 2022 Raffael Herzog
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
import ch.raffael.meldioc.model.messages.SimpleMessage;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.function.Function;

import static ch.raffael.meldioc.util.VavrX.tap;
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
  private final SrcElement<S, T> element;

  private final Seq<ModelType<S, T>> superTypes;
  private final Seq<ModelType<S, T>> importedSuperTypes;
  private final Seq<ModelMethod<S, T>> allMethods;
  private final Seq<ModelMethod<S, T>> provisionMethods;
  private final Seq<ModelMethod<S, T>> extensionPointMethods;
  private final Seq<ModelMethod<S, T>> mountMethods;
  private final Seq<ModelMethod<S, T>> setupMethods;
  private final Seq<ModelMethod<S, T>> parameterMethods;

  private final boolean isIllegalFeatureElement;

  public ModelType(Model<S, T> model, T type) {
    this.model = model;
    this.type = type;
    var checkFeatureElement = checkFeatureElement();
    this.element = checkFeatureElement._1();
    this.isIllegalFeatureElement = checkFeatureElement._2();
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
    Map<Tuple2<String, Seq<SrcElement<?, T>>>, Seq<ModelMethod<S, T>>> superMethods = superTypes.toStream()
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
    Seq<ModelMethod<S, T>> declaredMethods = adaptor.declaredMethods(type).toStream()
        .map(m -> ModelMethod.of(m, this).withOverrides(superMethods.get(m.methodSignature()).getOrElse(List.empty())))
        .toList();
    validateClassAnnotations();
    declaredMethods.forEach(this::validateDeclaredMethodAnnotations);
    this.allMethods = findAllMethods(superMethods, declaredMethods);
    this.mountMethods = findMountMethods(adaptor);
    this.provisionMethods = findProvisionMethods();
    this.extensionPointMethods = findExtensionPointMethods();
    this.parameterMethods = findParameterMethods(adaptor);
    this.setupMethods = findSetupMethods(adaptor);
  }

  void message(SimpleMessage<S, T> message) {
    if (isIllegalFeatureElement) {
      if (message.isId(Message.Id.MeldAnnotationOutsideFeature)) {
        return;
      }
      if (message.isId(Message.Id.ConflictingOverride) && message.element().configs().isEmpty()) {
        return;
      }
    }
    model.message(message);
  }

  private Tuple2<SrcElement<S, T>, Boolean> checkFeatureElement() {
    var element = model.adaptor().classElement(type);
    if (!element.configurationConfigOption().isDefined() && !element.featureConfigOption().isDefined()) {
      return Tuple.of(element, false);
    }
    if (model.adaptor().isEnumType(type)
        || model.adaptor().isRecordType(type)
        || model.adaptor().isAnnotationType(type)) {
      message(Message.illegalFeatureClass(element));
      return Tuple.of(element.withConfigs(element.configs().reject(
          c -> c.isConfigType(Feature.class) || c.isConfigType(Configuration.class))),
          true);
    }
    return Tuple.of(element, false);
  }

  private Seq<ModelMethod<S, T>> findAllMethods(Map<Tuple2<String, Seq<SrcElement<?, T>>>, Seq<ModelMethod<S, T>>> superMethods, Seq<ModelMethod<S, T>> declaredMethods) {
    return declaredMethods.toStream()
        .appendAll(superMethods.toStream()
            .filter(sm -> !declaredMethods.exists(dm -> dm.element().methodSignature().equals(sm._1)))
            .map(Tuple2::_2)
            .map(sm -> sm.size() == 1
                       ? sm.head()
                       : ModelMethod.of(sm.head().element(), this)
                           .withImplyReason(ModelMethod.ImplyReason.HIERARCHY)
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
            include &= validateProvisionOverrideAttribute(m1);
          }
          return include;
        })
        .map(element.featureConfigOption().isDefined() && model.adaptor().isInterface(element.type()) ? tap(m -> {
          if (m.element().provisionConfigOption().isEmpty()) {
            message(Message.featureInterfacesShouldDeclareProvisionsOnly(m.element(), element));
          }
        }) : identity());
  }

  private Seq<ModelMethod<S, T>> findMountMethods(Adaptor<S, T> adaptor) {
    return this.allMethods.toStream()
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Feature.Mount.class)))
        .map(tap(m ->
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
        .map(tap(m -> {
          if (!m.element().isAbstract()) {
            message(Message.mountMethodMustBeAbstract(m.element()));
          }
        }))
        .map(tap(m -> {
          var ret = adaptor.classElement(m.element().type());
          if (adaptor.classElement(m.element().type()).isInnerClass()) {
            message(Message.illegalInnerClass(m.element(), ret));
          }
        }))
        .filter(m -> {
          if (element.configurationConfigOption().isEmpty()) {
            message(Message.mountMethodsAllowedInConfigurationsOnly(m.element()));
            return false;
          } else {
            return true;
          }
        })
        .filter(m -> {
          SrcElement<S, T> cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Feature.class) || t.equals(Configuration.class))) {
            message(Message.mountMethodMustReturnFeature(m.element(), cls));
            return false;
          }
          return true;
        })
        .map(tap(m -> {
          if (!m.element().mountConfig().injected()) {
            validateExtendable(true, model.adaptor().classElement(m.element().type()), some(m.element()));
          }
        }))
        .toList();
  }

  private Seq<ModelMethod<S, T>> synthesizeMountMethods(Seq<ClassRef> classRefs, Adaptor<S, T> adaptor, S source) {
    return classRefs.toStream()
        .filter(cr -> {
          var t = adaptor.typeOf(cr);
          if (adaptor.hasTypeParameters(t)) {
            message(Message.mountAttributeClassMustNotBeParametrized(element, adaptor.classElement(t)));
            return false;
          } else {
            return true;
          }
        })
        .map(cr -> ModelMethod.<S, T>builder()
            .implyReason(ModelMethod.ImplyReason.SYNTHESIZED)
            .modelType(this)
            .element(SrcElement.<S, T>builder()
                .synthetic(true)
                .parent(element)
                .source(source)
                .kind(SrcElement.Kind.METHOD)
                .name("synthetic$$" + cr.canonicalName().replace('.', '$'))
                .type(adaptor.typeOf(cr))
                .accessPolicy(AccessPolicy.LOCAL)
                .isStatic(false)
                .isFinal(false)
                .isSealed(false)
                .isAbstract(true)
                .addConfigs(MountConfig.<S>builder()
                    .injected(false)
                    .source(source)
                    .build())
                .build())
            .build())
        .toList();
  }

  private Seq<ModelMethod<S, T>> findProvisionMethods() {
    var declaredLocal = this.allMethods.toStream()
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .toList();
    if (!element().configurationConfigOption().isDefined()) {
      return declaredLocal;
    }
    // `via` points to the mount method
    var declaredMounted = mountMethods.toStream()
        .flatMap(m -> model.modelOf(m.element().type()).provisionMethods().map(p -> Tuple.of(m, p)))
        .map(m -> ModelMethod.<S, T>builder()
            .modelType(this)
            .element(m._2().element())
            .via(m._1())
            .build())
        .toList();
    var allDeclared = declaredLocal.appendAll(declaredMounted);
    // determine implied
    var impliedLocal = declaredMounted.toStream()
        .reject(p -> declaredLocal.exists(l -> l.element().name().equals(p.element().name())))
        .filter(p -> {
          // report mounted provisions that will cause a conflict with methods defined in this class
          allMethods
              .filter(m -> m.element().provisionConfigOption().isEmpty())
              .filter(m -> m.element().methodSignature().equals(p.element().methodSignature()))
              .forEach(m -> message(Message.mountedProvisionOverridesMethod(
                  findErrorReportElement(m), m.element(), p.via().get().element())));
          return true;
        })
        .groupBy(p -> p.element().name())
        .mapValues(v -> Tuple.of(v,
            ModelMethod.<S, T>builder()
                .modelType(this)
                .via(none())
                .implyReason(ModelMethod.ImplyReason.MOUNT)
                .element(SrcElement.<S, T>builder()
                    .kind(SrcElement.Kind.METHOD)
                    .name(v.head().element().name())
                    .type(model.objectType())
                    .exceptions(List.of(model.throwableType()))
                    .addConfigs(ProvisionConfig.<S>builder()
                        .singleton(v.exists(
                            p -> p.element().provisionConfigOption().map(ProvisionConfig::singleton).getOrElse(false)))
                        .override(false)
                        .source(element.source())
                        .build())
                    .source(element().source())
                    .parentOption(element)
                    .accessPolicy(AccessPolicy.LOCAL)
                    .isAbstract(true)
                    .isStatic(false)
                    .isFinal(false)
                    .isSealed(false)
                    .build())
                .build()))
        .values().toStream()
        .map(Tuple2::_2)
        .toList();
    var allLocal = declaredLocal.appendAll(impliedLocal);
    // resolve implementations
    // IDEA has problems with inferring types, we're being explicit here
    var implementations = allDeclared.toStream()
        .groupBy(p -> p.element().name())
        .values().toStream()
        .<Option<ModelMethod<S, T>>>map(v -> {
          var i = v.filter(this::isConcreteProvision);
          if (i.isEmpty()) {
            v.forEach(p -> message(Message.unresolvedProvision(findErrorReportElement(p),
                p.element())));
            return none();
          } else if (i.length() > 1) {
            v.forEach(p ->
                message(Message.conflictingProvisions(findErrorReportElement(p),
                    v.remove(p).map(ModelMethod::element))));
            return none();
          } else {
            return i.headOption();
          }
        })
        .flatMap(Option::toStream)
        .toMap(p -> p.element().name(), identity());
    var resolved = allLocal.toStream()
        .map(p -> implementations.get(p.element().name())
            .filter(i -> validateImplementationCompatibility(p, i, declaredMounted))
            .map(i -> p.withVia(i.via()))
            // error has been reported
            .getOrElse(p));
    return resolved;
  }

  private boolean validateImplementationCompatibility(ModelMethod<S, T> provision, ModelMethod<S, T> impl, List<ModelMethod<S, T>> mounted) {
    // check type compatibility
    mounted.toStream()
        .filter(m -> m.element().name().equals(provision.element().name()))
        .filter(m -> !model.adaptor().isSubtypeOf(impl.element().type(), m.element().type()))
        .forEach(m ->
            message(Message.incompatibleProvisionTypes(findErrorReportElement(m), m.element(),
                impl.element(), impl.via().map(ModelMethod::element))));
    if (!model.adaptor().isSubtypeOf(impl.element().type(), provision.element().type())) {
      message(Message.incompatibleProvisionTypes(
          provision.via().map(ModelMethod::element).getOrElse(() -> findErrorReportElement(provision)),
          provision.element(), impl.element(), impl.via().map(ModelMethod::element)));
    }
    // check throws clause compatibility
    mounted.toStream()
        .filter(m -> m.element().name().equals(provision.element().name()))
        .forEach(m ->
            incompatibleExceptions(m.exceptions(), impl.exceptions()).forEach(e ->
                message(Message.incompatibleProvisionThrows(findErrorReportElement(m), provision.element(),
                    model.adaptor().classElement(e)))));
    incompatibleExceptions(provision.exceptions(), impl.exceptions()).forEach(e ->
        message(Message.incompatibleProvisionThrows(findErrorReportElement(provision), provision.element(),
            model.adaptor().classElement(e))));
    return true;
  }

  private boolean isConcreteProvision(ModelMethod<S, T> provision) {
    return !provision.element().isAbstract()
        || provision.via().flatMap(v -> v.element().mountConfigOption().map(MountConfig::injected)).getOrElse(false)
        || provision.overrides().exists(this::isConcreteProvision);
  }

  private Stream<T> incompatibleExceptions(Seq<T> declared, Seq<T> check) {
    var thrown = declared.append(model.runtimeExceptionType()).append(model.errorType());
    return check.toStream().filter(e -> !thrown.exists(t -> model.adaptor().isSubtypeOf(e, t)));
  }

  private Seq<ModelMethod<S, T>> findExtensionPointMethods() {
    return this.allMethods.toStream()
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .map(tap(m -> {
          SrcElement<S, T> cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.class))) {
            message(Message.extensionPointReturnRecommended(m.element(), cls));
          }
        }))
        .toList();
  }

  private Seq<ModelMethod<S, T>> findParameterMethods(Adaptor<S, T> adaptor) {
    var params = this.allMethods.toStream()
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Parameter.class)))
        .filter(this::validateNoParameters)
        .map(tap(m -> {
          if (m.element().parameterConfig().value().equals(Parameter.ALL)) {
            if (!adaptor.isSubtypeOf(model.configType().get(), m.element().type())) {
              message(Message.configTypeNotSupported(m.element()));
            }
          } else {
            if (model.configSupportedTypeOption(m.element().type()).isEmpty()) {
              message(Message.configTypeNotSupported(m.element()));
            }
          }
        }))
        .appendAll(collectMounted(ModelType::parameterMethods))
        .toList();
    if (element.configurationConfigOption().isDefined() && !supportsParameters()) {
      params
          .filter(p -> p.element().isAbstract())
          .forEach(p -> message(Message.typesafeConfigNotOnClasspath(findErrorReportElement(p))));
    }
    return params;
  }

  private Seq<ModelMethod<S, T>> findSetupMethods(Adaptor<S, T> adaptor) {
    return this.allMethods.toStream()
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Setup.class)))
        .map(tap(m -> {
          if (!adaptor.isNoType(m.element().type())) {
            message(Message.returnValueIgnored(m.element()));
          }
        }))
        .appendAll(collectMounted(ModelType::setupMethods))
        .map(element.configs().exists(c -> c.type().annotationType().equals(Configuration.class))
             ? m -> m.withArguments(mapSetupParameters(m))
             : identity())
        .toList();
  }

  private void validateExtendable(boolean direct, SrcElement<S, T> type, Option<SrcElement<S, T>> from) {
    if (direct && (type.isFinal() || type.isSealed())) {
      message(Message.typeNotExtendable(from.getOrElse(type), type));
    }
    if (type.isInnerClass()) {
      message(Message.illegalInnerClass(type));
      direct = false;
    }
    var outer = element.findOutermost();
    for (var c = type; c.parentOption().isDefined(); c = c.parent()) {
      if (c.accessPolicy() == AccessPolicy.PRIVATE || !c.accessibleTo(model.adaptor(), outer)) {
        message(Message.elementNotAccessible(element, type));
        direct = false;
      }
    }
    if (direct && !model.adaptor().isInterface(type.type())) {
      var ctors = model.adaptor().constructors(type.type());
      if (ctors.isEmpty()) {
        ctors = List.of(SrcElement.<S, T>builder()
            .kind(SrcElement.Kind.METHOD)
            .parent(type)
            .name(SrcElement.CONSTRUCTOR_NAME)
            .type(model.adaptor().noType())
            .accessPolicy(type.accessPolicy()).isStatic(false).isFinal(false).isSealed(false).isAbstract(false)
            .parameters(List.empty())
            .source(type.source())
            .synthetic(true)
            .build());
      }
      var ac = ctors.find(e -> e.parameters().isEmpty())
          .filter(c -> c.accessPolicy() != AccessPolicy.PRIVATE)
          .filter(c -> c.accessibleToByExtension(model.adaptor(), outer));
          ac.onEmpty(() -> message(from.fold(
              () -> Message.missingNoArgsConstructor(type),
              m -> Message.missingNoArgsConstructor(m, type))));
    }
  }

  private void validateImports() {
    superTypes.toStream()
        .filter(t -> t.element().featureConfigOption().isEmpty())
        .filter(t -> t.element().configurationConfigOption().isEmpty())
        .forEach(t -> {
          if (!isImported(t)) {
            message(Message.missingFeatureImportAnnotation(element, t.element()));
          }
        });
  }

  private void validateClassAnnotations() {
    if (!element.configs().exists(c -> c.type().featureRole()) && element.configs().exists(c -> !c.type().role())) {
      message(Message.meldAnnotationOutsideFeature(element()));
    }
  }

  private void validateDeclaredMethodAnnotations(ModelMethod<S, T> method) {
    if (!element.configs().exists(c -> c.type().featureRole()) && !method.element().configs().isEmpty()) {
      message(Message.meldAnnotationOutsideFeature(method.element()));
    }
    if (method.element().configs().count(c -> c.type().role()) > 1) {
      message(Message.conflictingCompositionRoles(method.element(), List.empty()));
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
    Seq<ModelMethod<S, T>> conflicts = method.overrides().toStream()
        .filter(s -> !s.element().configs().isEmpty())
        .filter(s -> !method.element().configs().map(ElementConfig::type)
            .equals(s.element().configs().map(ElementConfig::type)))
        .toList();
    if (!conflicts.isEmpty()) {
      message(Message.conflictingOverride(method.element(), conflicts.map(ModelMethod::element)));
    }
    return true;
  }

  private boolean validateMethodAccessibility(
      SrcElement<S, T> nonLocalMsgTarget, ModelMethod<S, T> method, boolean forOverride) {
    if (method.element().parent().equals(element)) {
      method.overrides()
          //.filter(s -> s.element().isOverridable()) // already checked and reported
          .forEach(s -> {
            if (!s.element().accessibleTo(model.adaptor(), method.element())) {
              message(Message.elementNotAccessible(method.element(), s.element()));
            }
          });
    } else if (!method.element().accessibleTo(model.adaptor(), element, forOverride)) {
      message(Message.elementNotAccessible(nonLocalMsgTarget, method.element()));
    }
    return true;
  }

  @SuppressWarnings("Convert2MethodRef")
  private boolean validateProvisionOverrideAttribute(ModelMethod<S, T> method) {
    method.element().configs().toStream()
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
            message(Message.provisionOverrideMissing(method.element(), s.element()));
          } else {
            message(Message.conflictingProvisions(element, List.of(method.element(), s.element())));
          }
        }));
    return true;
  }

  private boolean validateNoParameters(ModelMethod<S, T> method) {
    if (!method.element().parameters().isEmpty()) {
      message(Message.noParametersAllowed(method.element()));
    }
    return true;
  }

  private boolean validateReferenceType(ModelMethod<S, T> method) {
    if (!model.adaptor().isReference(method.element().type())) {
      // TODO (2019-04-07) support primitive types?
      message(Message.mustReturnReference(method.element()));
    }
    return true;
  }

  private boolean validateAbstractMethodImplementable(SrcElement<S, T> classElem, ModelMethod<S, T> m) {
    if (!m.element().isAbstract()) {
      return true;
    }
    if (!m.element().configs().exists(c -> c.type().willImplement())) {
      if (classElem.equals(element)) {
        message(Message.abstractMethodWillNotBeImplemented(m.element(), m.element()));
      } else {
        message(Message.abstractMethodWillNotBeImplemented(classElem, m.element()));
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
        SrcElement<S, T> epType = model.adaptor().classElement(param.type());
        message(method.via()
            .map(via -> Message.unresolvedExtensionPoint(via.element(), param, epType))
            .getOrElse(() -> method.element().parentOption().eq(Option.of(element))
                             ? Message.unresolvedExtensionPoint(param, epType)
                             : Message.unresolvedExtensionPoint(element, param, epType)));
        return BuiltinArgument.NONE.argument();
      } else if (candidates.size() > 1) {
        message(Message.conflictingExtensionPoints(param,
            candidates.filter(Either::isLeft).map(c -> c.getLeft().element())));
        return BuiltinArgument.NONE.argument();
      } else {
        return candidates.head();
      }
    });
  }

  private Seq<ModelMethod<S, T>> collectMounted(Function<ModelType<S, T>, Seq<ModelMethod<S, T>>> mounted) {
    return mountMethods.toStream()
        .flatMap(mount -> mounted.apply(model.modelOf(mount.element().type())).map(m -> m.withVia(mount)))
        .filter(m -> {
          boolean callable = m.element().accessibleToByExtension(model.adaptor(), element);
          if (!callable) {
            message(Message.elementNotAccessible(m.element(), element));
          }
          return callable;
        })
        .toList();
  }

  private SrcElement<S, T> findErrorReportElement(ModelMethod<S, T> method) {
    return findErrorReportElement(method.via().getOrElse(method).element());
  }
  private SrcElement<S, T> findErrorReportElement(SrcElement<S, T> element) {
    return Stream.iterate(some(element), e -> e.flatMap(SrcElement::parentOption))
        .takeWhile(Option::isDefined).flatMap(identity())
        .exists(e -> e.equals(this.element))
        ? element : this.element;
  }

  public Model<S, T> model() {
    return model;
  }

  public T type() {
    return type;
  }

  public SrcElement<S, T> element() {
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

  public Seq<ModelMethod<S, T>> mountMethods() {
    return mountMethods;
  }

  public Seq<ModelMethod<S, T>> setupMethods() {
    return setupMethods;
  }

  public Seq<ModelMethod<S, T>> parameterMethods() {
    return parameterMethods;
  }

  public boolean supportsParameters() {
    // maybe add a flag to the Configuration annotation to force-disable parameters
    return model.configType().isDefined();
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
