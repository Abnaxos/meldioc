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

package ch.raffael.compose.model;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Feature;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Setup;
import ch.raffael.compose.model.config.ElementConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import ch.raffael.compose.model.messages.Message;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.function.Function;

import static ch.raffael.compose.util.VavrX.touch;
import static io.vavr.API.*;
import static java.util.function.Function.identity;

/**
 * A rich representation of a compose model type.
 *
 * <p>This class also does all the analysis and validations and reports
 * errors to the model's {@link
 * ch.raffael.compose.model.messages.MessageSink MessageSink}. If there are
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
  private final Role role;

  private final Seq<ModelType<S, T>> superTypes;
  private final Seq<ModelMethod<S, T>> allMethods;
  private final Seq<ModelMethod<S, T>> provisionMethods;
  private final Seq<ModelMethod<S, T>> extensionPointMethods;
  private final Seq<ModelMethod<S, T>> mountMethods;
  private final Seq<ModelMethod<S, T>> setupMethods;
  private final Seq<ModelMethod<S, T>> parameterMethods;

  // TODO FIXME (2019-04-07) reject inner (non-static) classes

  public ModelType(Model<S, T> model, T type) {
    this.model = model;
    this.type = type;
    this.element = model.adaptor().classElement(type);
    this.role = Role.ofElement(element);
    Adaptor<S, T> adaptor = model.adaptor();
    this.superTypes = adaptor.superTypes(type).map(model::modelOf);
    Map<Tuple2<String, Seq<CElement<?, T>>>, Seq<ModelMethod<S, T>>> superMethods = superTypes.flatMap(cm -> cm.allMethods)
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
        .map(m -> ModelMethod.of(m, this).withOverrides(superMethods.get(m.methodSignature()).getOrElse(Seq())));
    validateClassAnnotations();
    declaredMethods.forEach(this::validateDeclaredMethodAnnotations);
    this.allMethods = declaredMethods
        .appendAll(superMethods
            .filter(sm -> !declaredMethods.exists(dm -> dm.element().methodSignature().equals(sm._1)))
            .map(Tuple2::_2)
            .map(sm -> sm.size() == 1
                       ? sm.head()
                       : ModelMethod.of(sm.head().element(), this).withImplied(true).withOverrides(sm)))
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
    this.mountMethods = this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Feature.Mount.class)))
        .map(touch(m ->
            model.modelOf(m.element().type()).allMethods()
                .filter(mm -> validateAbstractMethodImplementable(m.element(), mm))
                .filter(mm -> mm.element().configs().exists(c -> c.type().role()))
                .forEach(mm -> validateMethodAccessibility(m.element(), mm, true))
        ))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .map(touch(m -> {
          if (!m.element().isAbstract()) {
            model.message(Message.mountMethodMustBeAbstract(m.element()));
          }
        }))
        .filter(m -> {
          if (role != Role.CONFIGURATION) {
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
        });
    this.provisionMethods = this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateNoFeatureReturn)
        .map(m -> mapToMounts(m, ModelType::provisionMethods));
    this.extensionPointMethods = this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateNoFeatureReturn)
        .map(touch(m -> {
          CElement<S, T> cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.Acceptor.class))) {
            model.message(Message.extensionPointAcceptorReturnRecommended(m.element(), cls));
          }
        }))
        .map(m -> mapToMounts(m, ModelType::extensionPointMethods));
    this.parameterMethods = this.allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Parameter.class)))
        .filter(this::validateNoParameters)
        .map(touch(m -> {
          if (!model.configType().isDefined() && m.element().isAbstract()) {
            model.message(Message.typesafeConfigNotOnClasspath(m.element()));
          }
          else if (m.element().parameterConfig().value().map(p -> p.equals(Parameter.ALL)).getOrElse(false)) {
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
    this.setupMethods = this.allMethods
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
        .map(m -> Tuple(m, model.modelOf(m.element().type())))
        .map(tpl -> tpl.map2(mounted))
        .flatMap(tpl -> tpl._2().map(m -> Tuple(tpl._1(), m.withVia(tpl._1()))))
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

  private void validateClassAnnotations() {
    if (!element.configs().exists(c -> c.type().featureRole()) && element.configs().exists(c -> !c.type().role())) {
      model.message(Message.composeAnnotationOutsideFeature(element()));
    }
  }

  private void validateDeclaredMethodAnnotations(ModelMethod<S, T> m) {
    if (!element.configs().exists(c -> c.type().featureRole()) && !m.element().configs().isEmpty()) {
      model.message(Message.composeAnnotationOutsideFeature(m.element()));
    }
    if (m.element().configs().count(c -> c.type().role()) > 1) {
      model.message(Message.conflictingCompositionRoles(m.element(), Seq()));
    }
  }

  private boolean validateObjectMethods(ModelMethod<S, T> m) {
    if (model.objectMethods().exists(om -> m.element().canOverride(om, model.adaptor()))) {
      if (!m.element().configs().isEmpty()) {
        m.addMessage(model, Message.objectOverride(m.element()));
      }
      return false;
    }
    return true;
  }

  private boolean excludeStaticMethods(ModelMethod<S, T> m) {
    return !m.element().isStatic();
  }

  private boolean validateOverridableMethod(ModelMethod<S, T> m) {
    if (!m.element().isOverridable()) {
      m.addMessage(model, Message.nonOverridableMethod(m.element()));
    }
    return true;
  }

  private boolean validateConflictingSuperCompositionRoles(ModelMethod<S, T> m) {
    // TODO (2019-04-22) check the whole override tree to identify "holes"
    Seq<ModelMethod<S, T>> conflicts = m.overrides()
        .filter(s -> !s.element().configs().isEmpty())
        .filter(s -> !m.element().configs().map(ElementConfig::type)
            .equals(s.element().configs().map(ElementConfig::type)));
    if (!conflicts.isEmpty()) {
      model.message(Message.conflictingOverride(m.element(), conflicts.map(ModelMethod::element)));
    }
    return true;
  }

  private boolean validateMethodAccessibility(CElement<S, T> nonLocalMsgTarget, ModelMethod<S, T> m, boolean forOverride) {
    if (m.element().parent().equals(element)) {
      m.overrides()
          //.filter(s -> s.element().isOverridable()) // already checked and reported
          .forEach(s -> {
            if (!s.element().accessibleTo(model.adaptor(), m.element())) {
              model.message(Message.methodNotAccessible(m.element(), s.element()));
            }
          });
    } else if (!m.element().accessibleTo(model.adaptor(), element)) {
      if (!(forOverride && m.element().accessPolicy() == AccessPolicy.PROTECTED)) {
        model.message(Message.methodNotAccessible(nonLocalMsgTarget, m.element()));
      }
    }
    return true;
  }

  private boolean validateProvisionOverrides(ModelMethod<S, T> m) {
    m.element().configs()
        .filter(c -> c.type().annotationType().equals(Provision.class))
        .map(ProvisionConfig.class::cast)
        .filter(c -> !c.shared() && !c.override())
        .headOption().forEach(__ -> m.overrides()
            .filter(s -> s.element().configs()
                .filter(c -> c.type().annotationType().equals(Provision.class))
                .map(ProvisionConfig.class::cast)
                .exists(ProvisionConfig::shared))
            .headOption()
            .forEach(s -> model.message(Message.provisionOverrideMissing(m.element(), s.element()))));
    return true;
  }

  private boolean validateNoParameters(ModelMethod<S, T> m) {
    if (!m.element().parameters().isEmpty()) {
      model.message(Message.noParametersAllowed(m.element()));
    }
    return true;
  }

  private boolean validateReferenceType(ModelMethod<S, T> m) {
    if (!model.adaptor().isReference(m.element().type())) {
      // TODO (2019-04-07) support primitive types?
      model.message(Message.mustReturnReference(m.element()));
    }
    return true;
  }

  private boolean validateNoFeatureReturn(ModelMethod<S, T> m) {
    CElement<S, T> cls = model.adaptor().classElement(m.element().type());
    if (cls.configs().map(c -> c.type().annotationType()).exists(
        t -> t.equals(Feature.class) || t.equals(Configuration.class))) {
      model.message(Message.methodShouldNotReturnFeature(m.element(), cls));
    }
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
          .map(m -> Tuple(m, model.modelOf(m.element().type()).extensionPointMethods()))
          .flatMap(tplViaEpp -> tplViaEpp._2().map(m -> Tuple(tplViaEpp._1(), m)))
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
            model.message(Message.methodNotAccessible(m.element(), element));
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

  public Role role() {
    return role;
  }

  public Seq<ModelType<S, T>> superTypes() {
    return superTypes;
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

  @Override
  public String toString() {
    return "ModelType{" +
        "type=" + type +
        '}';
  }

  public enum Role {
    FEATURE, CONFIGURATION, EXTENSION_POINT_API, NONE;

    public boolean isFeature() {
      return this == FEATURE || this == CONFIGURATION;
    }

    public static Role ofElement(CElement<?, ?> element) {
      return element.configs()
          .map(Role::ofConfig)
          .filter(r -> r != NONE)
          .singleOption()
          .getOrElse(NONE);
    }

    private static Role ofConfig(ElementConfig<?> c) {
      if (c.type().annotationType().equals(Feature.class)) {
        return FEATURE;
      } else if (c.type().annotationType().equals(Configuration.class)) {
        return CONFIGURATION;
      } else if (c.type().annotationType().equals(ExtensionPoint.Acceptor.class)) {
        return EXTENSION_POINT_API;
      } else {
        return NONE;
      }
    }
  }

}
