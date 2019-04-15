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

import ch.raffael.compose.Assembly;
import ch.raffael.compose.Compose;
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.model.config.ConfigurationPrefixConfig;
import ch.raffael.compose.model.config.ElementConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import ch.raffael.compose.model.messages.Message;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

import java.util.function.Function;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
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
  private final Seq<ModelMethod<S, T>> composeMethods;
  private final Seq<ModelMethod<S, T>> configurationMethods;

  // TODO FIXME (2019-04-07) validate assembly config
  // TODO FIXME (2019-04-07) reject inner (non-static) classes

  public ModelType(Model<S, T> model, T type) {
    this.model = model;
    this.type = type;
    var adaptor = model.adaptor();
    superTypes = adaptor.superTypes(type, model).map(model::modelOf);
    var superMethods = superTypes.flatMap(cm -> cm.allMethods);
    superMethods = Vector.ofAll(superMethods.crossProduct()
        .flatMap(tp -> tp.apply((lm, rm) -> {
          if (lm.equals(rm)) {
            return Seq(lm);
          }
          var le = lm.element();
          var re = rm.element();
          if (le.canOverride(re, adaptor)) {
            if (re.canOverride(le, adaptor)) {
              if (adaptor.isSubtypeOf(le.type(), re.type())) {
                return Seq(lm);
              } else if (adaptor.isSubtypeOf(re.type(), le.type())) {
                return Seq(rm);
              } else {
                // TODO (2019-04-15) this will be a compiler error
                return Seq(lm, rm);
              }
            } else {
              return Seq(lm);
            }
          } else if (re.canOverride(le, adaptor)) {
            return Seq(rm);
          }
          return Seq(rm, lm);
        })).distinct());
    var finalSuperMethods = superMethods;
    var declaredMethods = adaptor.declaredMethods(type, model)
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
          if (role != Role.ASSEMBLY) {
            model.message(Message.mountRequiresAssembly(m.element()));
            return false;
          } else {
            return true;
          }
        })
        .filter(m -> {
          var cls = model.adaptor().classElement(m.element().type());
          if (!cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Module.class) || t.equals(Assembly.class))) {
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
    this.configurationMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Configuration.class)))
        .filter(this::validateNoParameters)
        .peek(m -> {
          if (!model.configType().isDefined() && m.element().isAbstract()) {
            model.message(Message.typesafeConfigNotOnClasspath(m.element()));
          }
          else if (m.element().configurationConfig().path().map(p -> p.equals(Configuration.ALL)).getOrElse(false)) {
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
        .appendAll(collectMounted(ModelType::configurationMethods));
    this.composeMethods = allMethods
        .filter(m -> m.element().configs().exists(c -> c.type().annotationType().equals(Compose.class)))
        .peek(m -> {
          if (!adaptor.isNoType(m.element().type())) {
            model.message(Message.returnValueIgnored(m.element()));
          }
        })
        .appendAll(collectMounted(ModelType::composeMethods))
        .map(m -> m.withArguments(mapComposeParameters(m)));
  }

  private ModelMethod<S, T> mapToMounts(ModelMethod<S, T> method, Function<ModelType<S, T>, Seq<ModelMethod<S, T>>> mounted) {
    if (element.assemblyConfigOption().isEmpty() || !method.element().isAbstract() || method.via().isDefined()) {
      return method;
    }
    var forwards = mountMethods
        .map(m -> Tuple(m, model.modelOf(m.element().type())))
        .map(tp -> tp.map2(mounted))
        .flatMap(tp -> tp._2().map(m -> Tuple(tp._1(), m.withVia(tp._1()))))
        .filter(tp -> tp._1().element().mountConfig().external() || !tp._2().element().isAbstract())
        .filter(tp -> tp._2().element().name().equals(method.element().name()))
        .filter(tp -> tp._2().element().canOverride(method.element(), model.adaptor()));
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
    } else if (element.configs().reject(ConfigurationPrefixConfig.class::isInstance).size() > 1) {
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

  private boolean validateOverrideVisibility(ModelMethod<S, T> method) {
    method.overrides()
        .filter(s -> !s.element().isOverridable()) // already checked and reported
        .forEach(s -> {
          if (!s.element().accessibleTo(model.adaptor(), method.element())) {
            model.message(Message.methodNotAccessible(s.element(), method.element()));
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
    if (cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Module.class) || t.equals(Assembly.class))) {
      model.message(Message.shouldNotReturnModule(m.element(), cls));
    }
    return true;
  }

  private Seq<Either<ModelMethod<S, T>, BuiltinArgument>> mapComposeParameters(ModelMethod<S, T> method) {
    return method.element().parameters().map(p -> {
      Seq<Either<ModelMethod<S, T>, BuiltinArgument>> candidates = Vector.ofAll(model.configType()
          .filter(ct -> model.adaptor().isSubtypeOf(ct, p.type()))
          .map(__ -> BuiltinArgument.CONFIG.argument()));
      candidates = candidates.appendAll(
          extensionPointProvisionMethods
              .filter(epp -> model.adaptor().isSubtypeOf(epp.element().type(), p.type()))
              .map(Either::left));
      candidates = candidates.appendAll(mountMethods
          .map(m -> Tuple(m, model.modelOf(m.element().type()).extensionPointProvisionMethods()))
          .flatMap(tp -> tp._2().map(m -> Tuple(tp._1(), m)))
          .filter(tp -> model.adaptor().isSubtypeOf(tp._2().element().type(), p.type()))
          .map(tp -> tp._2().withVia(tp._1()))
          .map(Either::left));
      if (candidates.isEmpty()) {
        model.message(Message.noMatchingExtensionPointProvision(p));
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

  public Seq<ModelMethod<S, T>> composeMethods() {
    return composeMethods;
  }

  public Seq<ModelMethod<S, T>> configurationMethods() {
    return configurationMethods;
  }

  @Override
  public String toString() {
    return "ModelType{" +
        "type=" + type +
        '}';
  }

  public enum Role {
    MODULE, ASSEMBLY, EXTENSION_POINT_API, NONE;

    public boolean isModule() {
      return this == MODULE || this == ASSEMBLY;
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
      } else if (c.type().annotationType().equals(Assembly.class)) {
        return ASSEMBLY;
      } else if (c.type().annotationType().equals(ExtensionPoint.Api.class)) {
        return EXTENSION_POINT_API;
      } else {
        return NONE;
      }
    }
  }

}
