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
import ch.raffael.compose.Setup;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.model.config.ParameterPrefixConfig;
import ch.raffael.compose.model.config.ElementConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import ch.raffael.compose.model.messages.Message;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

import java.util.function.Function;

import static io.vavr.API.*;

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
  private final Seq<ModelMethod<S, T>> extensionPointProvisionMethods;
  private final Seq<ModelMethod<S, T>> mountMethods;
  private final Seq<ModelMethod<S, T>> setupMethods;
  private final Seq<ModelMethod<S, T>> parameterMethods;

  // TODO FIXME (2019-04-07) validate configuration config
  // TODO FIXME (2019-04-07) reject inner (non-static) classes

  public ModelType(Model<S, T> model, T type) {
    this.model = model;
    this.type = type;
    var adaptor = model.adaptor();
    superTypes = adaptor.superTypes(type).map(model::modelOf);
    var superMethods = superTypes.flatMap(cm -> cm.allMethods);
    superMethods = Vector.ofAll(superMethods.crossProduct()
        .flatMap(tpl -> tpl.apply((leftMethod, rightMethod) -> {
          if (leftMethod.equals(rightMethod)) {
            return Seq(leftMethod);
          }
          var le = leftMethod.element();
          var re = rightMethod.element();
          if (le.canOverride(re, adaptor)) {
            if (re.canOverride(le, adaptor)) {
              if (adaptor.isSubtypeOf(le.type(), re.type())) {
                return Seq(leftMethod);
              } else if (adaptor.isSubtypeOf(re.type(), le.type())) {
                return Seq(rightMethod);
              } else {
                // TODO (2019-04-15) this will be a compiler error
                return Seq(leftMethod, rightMethod);
              }
            } else {
              return Seq(leftMethod);
            }
          } else if (re.canOverride(le, adaptor)) {
            return Seq(rightMethod);
          }
          return Seq(rightMethod, leftMethod);
        })).distinct());
    var finalSuperMethods = superMethods;
    var declaredMethods = adaptor.declaredMethods(type)
        .map(m -> ModelMethod.of(m, this).withOverrides(finalSuperMethods.filter(s -> m.canOverride(s.element(), adaptor))));
    this.element = validateClassElement(declaredMethods);
    this.role = Role.ofElement(element);
    // TODO (2019-04-07) short-circuit if !role.isModule()
    var allMethods = declaredMethods
//        .appendAll(superMethods.filter(sm -> !declaredMethods.exists(dm -> adaptor.canOverride(dm.element(), sm.element()))))
        .appendAll(superMethods.filter(sm -> !declaredMethods.flatMap(ModelMethod::overrides).exists(m -> m.equals(sm))));
    allMethods = allMethods
        .filter(m -> {
          boolean include = true;
          //noinspection ConstantConditions
          include &= validateObjectMethods(m);
          include &= excludeStaticMethods(m);
          include &= validateConflictingSuperCompositionRoles(m);
          if (!m.element().configs().isEmpty()) {
            include &= validateSingleCompositionRole(m);
            include &= validateOverridableMethod(m);
            include &= validateOverrideVisibility(m);
            include &= validateProvisionOverrides(m);
          }
          return include;
        });
    this.allMethods = allMethods;
    this.mountMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Module.Mount.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .peek(m -> {
          if (!m.element().isAbstract()) {
            model.message(Message.mountMustBeAbstract(m.element()));
          }
        })
        .filter(m -> {
          if (role != Role.CONFIGURATION) {
            model.message(Message.mountRequiresConfiguration(m.element()));
            return false;
          } else {
            return true;
          }
        })
        .filter(m -> {
          var cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Module.class) || t.equals(Configuration.class))) {
            model.message(Message.mustReturnModule(m.element(), cls));
            return false;
          }
          return true;
        });
    this.provisionMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateNoModuleReturn)
        .map(m -> mapToMounts(m, ModelType::provisionMethods));
    this.extensionPointProvisionMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateNoModuleReturn)
        .peek(m -> {
          var cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.Api.class))) {
            model.message(Message.extensionPointApiReturnRecommended(m.element(), cls));
          }
        })
        .map(m -> mapToMounts(m, ModelType::extensionPointProvisionMethods));
    this.parameterMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Parameter.class)))
        .filter(this::validateNoParameters)
        .peek(m -> {
          if (!model.configType().isDefined() && m.element().isAbstract()) {
            model.message(Message.typesafeConfigNotOnClasspath(m.element()));
          }
          else if (m.element().parameterConfig().path().map(p -> p.equals(Parameter.ALL)).getOrElse(false)) {
            if (adaptor.isSubtypeOf(model.configType().get(), m.element().type())) {
              model.message(Message.configTypeNotSupported(m.element()));
            }
          } else {
            if (model.configSupportedTypeOption(m.element().type()).isEmpty()) {
              model.message(Message.configTypeNotSupported(m.element()));
            }
          }
        })
//        .filter(this::validateReferenceType)
        .appendAll(collectMounted(ModelType::parameterMethods));
    this.setupMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Setup.class)))
        .peek(m -> {
          if (!adaptor.isNoType(m.element().type())) {
            model.message(Message.returnValueIgnored(m.element()));
          }
        })
        .appendAll(collectMounted(ModelType::setupMethods))
        .map(m -> m.withArguments(mapSetupParameters(m)));
  }

  private ModelMethod<S, T> mapToMounts(ModelMethod<S, T> method, Function<ModelType<S, T>, Seq<ModelMethod<S, T>>> mounted) {
    if (element.configurationConfigOption().isEmpty() || !method.element().isAbstract() || method.via().isDefined()) {
      return method;
    }
    var forwards = mountMethods
        .map(m -> Tuple(m, model.modelOf(m.element().type())))
        .map(tpl -> tpl.map2(mounted))
        .flatMap(tpl -> tpl._2().map(m -> Tuple(tpl._1(), m.withVia(tpl._1()))))
        .filter(tpl -> tpl._1().element().mountConfig().injected() || !tpl._2().element().isAbstract())
        .filter(tpl -> tpl._2().element().name().equals(method.element().name()))
        .filter(tpl -> tpl._2().element().canOverride(method.element(), model.adaptor()));
    if (forwards.isEmpty()) {
      model.message(Message.noImplementationCandidate(element, method.element()));
      return method;
    } else if (forwards.size() > 1) {
      model.message(Message.multipleImplementationCandidates(method.element().withSource(element.source()), forwards.map(tp -> tp._2().element())));
      return method;
    } else {
      return method.withVia(forwards.head()._1());
    }
  }

  private CElement<S, T> validateClassElement(Seq<ModelMethod<S, T>> selfMethods) {
    // TODO (2019-04-07) incomplete, doesn't handle extension point APIs
    var element = model.adaptor().classElement(type);
    if (!element.configs().exists(c -> Role.ofConfig(c) != Role.NONE)) {
      if (selfMethods.exists(m -> !m.element().configs().isEmpty())
          || superTypes.exists(c -> !c.element.configs().isEmpty())) {
        model.message(Message.missingComposeClassAnnotation(element));
      }
    } else if (element.configs().reject(ParameterPrefixConfig.class::isInstance).size() > 1) {
      model.message(Message.conflictingComposeAnnotations(element));
    }
    return element;
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

  private boolean validateSingleCompositionRole(ModelMethod<S, T> m) {
    if (m.element().configs().size() > 1) {
      m.addMessage(model, Message.conflictingComposeAnnotations(m.element()));
    }
    return true;
  }

  private boolean validateOverridableMethod(ModelMethod<S, T> m) {
    if (!m.element().isOverridable()) {
      m.addMessage(model, Message.nonOverridableMethod(m.element()));
    }
    return true;
  }

  private boolean validateConflictingSuperCompositionRoles(ModelMethod<S, T> m) {
    m.overrides()
        .filter(s -> s.element().configs().isEmpty())
        .filter(s -> !m.element().configs().map(ElementConfig::type)
            .equals(s.element().configs().map(ElementConfig::type)))
        .forEach(s -> m.addMessage(model, Message.conflictingCompositionRoles(m.element(), s.element())));
    return true;
  }

  private boolean validateOverrideVisibility(ModelMethod<S, T> m) {
    m.overrides()
        .filter(s -> !s.element().isOverridable()) // already checked and reported
        .forEach(s -> {
          if (!s.element().accessibleTo(model.adaptor(), m.element())) {
            model.message(Message.methodNotAccessible(s.element(), m.element()));
          }
        });
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
      model.message(Message.parametersNotAllowed(m.element()));
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

  private boolean validateNoModuleReturn(ModelMethod<S, T> m) {
    var cls = model.adaptor().classElement(m.element().type());
    if (cls.configs().map(c -> c.type().annotationType()).exists(
        t -> t.equals(Module.class) || t.equals(Configuration.class))) {
      model.message(Message.shouldNotReturnModule(m.element(), cls));
    }
    return true;
  }

  private Seq<Either<ModelMethod<S, T>, BuiltinArgument>> mapSetupParameters(ModelMethod<S, T> method) {
    return method.element().parameters().map(param -> {
      Seq<Either<ModelMethod<S, T>, BuiltinArgument>> candidates = Vector.ofAll(model.configType()
          .filter(t -> model.adaptor().isSubtypeOf(t, param.type()))
          .map(__ -> BuiltinArgument.CONFIG.argument()));
      candidates = candidates.appendAll(
          extensionPointProvisionMethods
              .filter(epp -> model.adaptor().isSubtypeOf(epp.element().type(), param.type()))
              .map(Either::left));
      candidates = candidates.appendAll(mountMethods
          .map(m -> Tuple(m, model.modelOf(m.element().type()).extensionPointProvisionMethods()))
          .flatMap(tplViaEpp -> tplViaEpp._2().map(m -> Tuple(tplViaEpp._1(), m)))
          .filter(tplViaEpp -> model.adaptor().isSubtypeOf(tplViaEpp._2().element().type(), param.type()))
          .map(tplViaEpp -> tplViaEpp._2().withVia(tplViaEpp._1()))
          .map(Either::left));
      if (candidates.isEmpty()) {
        model.message(Message.noMatchingExtensionPointProvision(param));
        return BuiltinArgument.NONE.argument();
      } else if (candidates.size() > 1) {
        model.message(Message.ambiguousExtensionPointProvisions(element,
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
        .filter(m -> m.element().accessibleTo(model.adaptor(), element));
  }

  public Model<S, T> model() {
    return model;
  }

  public T type() {
    return type;
  }

  public <E extends T> E type(Class<E> expected) {
    return element.type(expected);
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

  public Seq<ModelMethod<S, T>> extensionPointProvisionMethods() {
    return extensionPointProvisionMethods;
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
    MODULE, CONFIGURATION, EXTENSION_POINT_API, NONE;

    public boolean isModule() {
      return this == MODULE || this == CONFIGURATION;
    }

    public static Role ofElement(CElement<?, ?> element) {
      return element.configs()
          .map(Role::ofConfig)
          .filter(r -> r != NONE)
          .singleOption()
          .getOrElse(NONE);
    }

    private static Role ofConfig(ElementConfig<?> c) {
      if (c.type().annotationType().equals(Module.class)) {
        return MODULE;
      } else if (c.type().annotationType().equals(Configuration.class)) {
        return CONFIGURATION;
      } else if (c.type().annotationType().equals(ExtensionPoint.Api.class)) {
        return EXTENSION_POINT_API;
      } else {
        return NONE;
      }
    }
  }

}
