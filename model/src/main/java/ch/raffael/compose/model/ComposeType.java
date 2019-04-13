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
import ch.raffael.compose.model.config.ModuleConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import ch.raffael.compose.model.messages.Message;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public final class ComposeType<S, T> {

  private final Model<S, T> model;
  private final T type;
  private final CElement<S, T> element;
  private final Role role;

  private final Seq<ComposeType<S, T>> superClasses;
  private final Seq<ComposeMethod<S, T>> allMethods;
  private final Seq<ComposeMethod<S, T>> provisionMethods;
  private final Seq<ComposeMethod<S, T>> extensionPointProvisionMethods;
  private final Seq<ComposeMethod<S, T>> mountMethods;
  private final Seq<ComposeMethod<S, T>> composeMethods;
  private final Seq<ComposeMethod<S, T>> configurationMethods;
  private final Option<String> configPrefix;

  // TODO FIXME (2019-04-07) validate assembly config
  // TODO FIXME (2019-04-07) reject inner (non-static) classes

  public ComposeType(Model<S, T> model, T type) {
    this.model = model;
    this.type = type;
    var adaptor = model.adaptor();
    superClasses = adaptor.superTypes(type, model).map(model::modelOf);
    var superMethods = superClasses.flatMap(cm -> cm.allMethods);
    var declaredMethods = adaptor.declaredMethods(type, model)
        .map(m -> ComposeMethod.of(m).withOverrides(superMethods.filter(s -> adaptor.canOverride(m, s.method()))));
    this.element = validateClassElement(declaredMethods);
    this.role = Role.ofElement(element);
    // TODO (2019-04-07) short-circuit if !role.isModule()
    this.allMethods = declaredMethods
        .filter(m -> {
          boolean include = true;
          //noinspection ConstantConditions
          include &= validateObjectMethods(m);
          include &= excludeStaticMethods(m);
          include &= validateConflictingSuperCompositionRoles(m);
          if (!m.method().configs().isEmpty()) {
            include &= validateSingleCompositionRole(m);
            include &= validateOverridableMethod(m);
            include &= validateOverrideVisibility(m);
            include &= validateProvisionOverrides(m);
          }
          return include;
        });
    this.provisionMethods = allMethods
        .filter(m -> m.method().configs().exists(c -> c.type().annotationType().equals(Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateNoModuleReturn);
    this.extensionPointProvisionMethods = allMethods
        .filter(m -> m.method().configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.Provision.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .filter(this::validateNoModuleReturn)
        .peek(m -> {
          var cls = model.adaptor().classElement(m.method().type());
          if (!cls.configs().exists(c -> c.type().annotationType().equals(ExtensionPoint.Api.class))) {
            model.message(Message.extensionPointApiReturnRecommended(m.method(), cls));
          }
        });
    this.mountMethods = allMethods
        .filter(m -> m.method().configs().exists(c -> c.type().annotationType().equals(Module.Mount.class)))
        .filter(this::validateNoParameters)
        .filter(this::validateReferenceType)
        .peek(m -> {
          if (!m.method().isAbstract()) {
            model.message(Message.mountMustBeAbstract(m.method()));
          }
        })
        .filter(m -> {
          if (role != Role.ASSEMBLY) {
            model.message(Message.mountRequiresAssembly(m.method()));
            return false;
          } else {
            return true;
          }
        })
        .filter(m -> {
          var cls = model.adaptor().classElement(m.method().type());
          if (!cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Module.class) || t.equals(Assembly.class))) {
            model.message(Message.mustReturnModule(m.method(), cls));
          }
          return false;
        });
    this.composeMethods = allMethods
        .filter(m -> m.method().configs().exists(c -> c.type().annotationType().equals(Compose.class)))
        .peek(m -> {
          if (adaptor.isNoType(m.method().type())) {
            model.message(Message.returnValueIgnored(m.method()));
          }
        });
    this.configurationMethods = allMethods
        .filter(m -> m.method().configs().exists(c -> c.type().annotationType().equals(Configuration.class)))
        .filter(this::validateNoParameters)
        .peek(m -> {
          if (model.configSupportedType(m.method().type()).isEmpty()) {
            model.message(Message.configTypeNotSupported(m.method()));
          }
        })
        .filter(this::validateReferenceType);
    this.configPrefix = element.configs()
        .filter(ConfigurationPrefixConfig.class::isInstance)
        .map(ConfigurationPrefixConfig.class::cast)
        .headOption()
        .peek(p -> {
          if (role.isModule()) {
            Message.configValueOutsideModule(element);
          }
        })
        .map(ConfigurationPrefixConfig::value);
  }

  private CElement<S, T> validateClassElement(Seq<ComposeMethod<S, T>> selfMethods) {
    // TODO (2019-04-07) incomplete, doesn't handle extension point APIs
    var element = model.adaptor().classElement(type);
    if (!element.configs().exists(c -> Role.ofConfig(c) != Role.NONE)) {
      if (selfMethods.exists(m -> !m.method().configs().isEmpty())
          || superClasses.exists(c -> !c.element.configs().isEmpty())) {
        model.message(Message.missingComposeClassAnnotation(element));
      }
      element.withConfigs(Set(ModuleConfig.builder().build()));
    } else if (element.configs().size() > 1) {
      model.message(Message.multipleCompositionRoles(element));
      element.withConfigs(Set(ModuleConfig.builder().build()));
    }
    return element;
  }

  private boolean validateObjectMethods(ComposeMethod<S, T> m) {
    if (model.objectMethods().exists(om -> model.adaptor().canOverride(m.method(), om))) {
      if (!m.method().configs().isEmpty()) {
        m.addMessage(model, Message.objectOverride(m.method()));
      }
      return false;
    }
    return true;
  }

  private boolean excludeStaticMethods(ComposeMethod<S, T> m) {
    return !m.method().isStatic();
  }

  private boolean validateSingleCompositionRole(ComposeMethod<S, T> m) {
    if (m.method().configs().size() > 1) {
      m.addMessage(model, Message.multipleCompositionRoles(m.method()));
    }
    return true;
  }

  private boolean validateOverridableMethod(ComposeMethod<S, T> m) {
    if (!m.method().isOverridable()) {
      m.addMessage(model, Message.nonOverridableMethod(m.method()));
    }
    return true;
  }

  private boolean validateConflictingSuperCompositionRoles(ComposeMethod<S, T> m) {
    m.overrides()
        .filter(s -> s.method().configs().isEmpty())
        .filter(s -> !m.method().configs().map(ElementConfig::type)
            .equals(s.method().configs().map(ElementConfig::type)))
        .forEach(s -> m.addMessage(model, Message.conflictingCompositionRoles(m.method(), s.method())));
    return true;
  }

  private boolean validateOverrideVisibility(ComposeMethod<S, T> method) {
    method.overrides()
        .filter(s -> !s.method().isOverridable()) // already checked and reported
        .forEach(s -> {
          assert s.method().accessPolicy() != AccessPolicy.PRIVATE;
          if (s.method().accessPolicy() == AccessPolicy.LOCAL) {
            if (!model.adaptor().packageOf(s.method()).equals(model.adaptor().packageOf(s.method()))) {
              model.message(Message.methodNotAccessible(s.method(), method.method()));
            }
          }
        });
    return true;
  }

  private boolean validateProvisionOverrides(ComposeMethod<S, T> m) {
    m.method().configs()
        .filter(c -> c.type().annotationType().equals(Provision.class))
        .map(ProvisionConfig.class::cast)
        .filter(c -> !c.shared() && !c.override())
        .headOption().forEach(__ -> m.overrides()
            .filter(s -> s.method().configs()
                .filter(c -> c.type().annotationType().equals(Provision.class))
                .map(ProvisionConfig.class::cast)
                .exists(ProvisionConfig::shared))
            .headOption()
            .forEach(s -> model.message(Message.provisionOverrideMissing(m.method(), s.method()))));
    return true;
  }

  private boolean validateNoParameters(ComposeMethod<S, T> m) {
    if (!m.method().parameters().isEmpty()) {
      model.message(Message.parametersNotAllowed(m.method()));
    }
    return true;
  }

  private boolean validateReferenceType(ComposeMethod<S, T> m) {
    if (!model.adaptor().isReference(m.method().type())) {
      // TODO (2019-04-07) support primitive types?
      model.message(Message.mustReturnReference(m.method()));
    }
    return true;
  }

  private boolean validateNoModuleReturn(ComposeMethod<S, T> m) {
    var cls = model.adaptor().classElement(m.method().type());
    if (cls.configs().map(c -> c.type().annotationType()).exists(t -> t.equals(Module.class) || t.equals(Assembly.class))) {
      model.message(Message.shouldNotReturnModule(m.method(), cls));
    }
    return true;
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

  public Seq<ComposeType<S, T>> superClasses() {
    return superClasses;
  }

  public Seq<ComposeMethod<S, T>> allMethods() {
    return allMethods;
  }

  public Seq<ComposeMethod<S, T>> provisionMethods() {
    return provisionMethods;
  }

  public Seq<ComposeMethod<S, T>> extensionPointProvisionMethods() {
    return extensionPointProvisionMethods;
  }

  public Seq<ComposeMethod<S, T>> mountMethods() {
    return mountMethods;
  }

  public Seq<ComposeMethod<S, T>> composeMethods() {
    return composeMethods;
  }

  public Seq<ComposeMethod<S, T>> configurationMethods() {
    return configurationMethods;
  }

  public Option<String> configPrefix() {
    return configPrefix;
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
