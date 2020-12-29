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

package ch.raffael.meldioc.model;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
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
import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import io.vavr.control.Option.None;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

import static io.vavr.control.Option.some;

/**
 * A raw AST-agnostic representation of the core elements making up a java
 * program (class, method, method parameter) and their configurations.
 */
@Immutable.Public
@SuppressWarnings("varargs") // Bug in immutables or immutables-vavr: the builder methods are not annotated correctly
abstract class _CElement<S, T> {

  public static final String CONSTRUCTOR_NAME = "<init>";

  private static final Object PSEUDO_SOURCE = new Object();

  _CElement() {
  }

  public abstract Kind kind();

  public abstract String name();

  public abstract T type();

  public abstract S source();

  public CElement<S, T> parent() {
    return parentOption().getOrElseThrow(() -> new InconsistentModelException("Element " + this + " has no parent", this));
  }

  @Value.Default
  public boolean synthetic() {
    return false;
  }

  public abstract Option<CElement<S, T>> parentOption();

  @Value.Redacted
  public abstract AccessPolicy accessPolicy();

  @Value.Redacted
  public abstract boolean isStatic();

  @Value.Redacted
  public abstract boolean isFinal();

  @Value.Redacted
  public abstract boolean isAbstract();

  @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "RedundantSuppression"})
  public boolean isOverridable() {
    return !isFinal() && !isStatic() && accessPolicy() != AccessPolicy.PRIVATE;
  }

  @Value.Redacted
  abstract Seq<CElement<S, T>> parameters();

  @Value.Redacted
  abstract Seq<T> exceptions();

  @Value.Redacted
  public abstract Set<ElementConfig<S>> configs();

  public Option<CElement<S, T>> findClass() {
    Option<CElement<S, T>> e = some((CElement<S, T>) this);
    while (e.isDefined() && e.get().kind() != Kind.CLASS) {
      e = e.get().parentOption();
    }
    return e;
  }

  public CElement<S, T> findOutermost() {
    var p = (CElement<S, T>) this;
    while (p.parentOption().isDefined()) {
      p = p.parent();
    }
    return p;
  }

  public boolean accessibleTo(Adaptor<S, T> adaptor, CElement<S, T> that) {
    if (kind() == Kind.PARAMETER || that.kind() == Kind.PARAMETER) {
      return false;
    }
    if (accessPolicy() == AccessPolicy.PUBLIC) {
      return true;
    }
    CElement<S, T> thisClass = findClass().getOrNull();
    if (thisClass == null) {
      return false;
    }
    CElement<S, T> thatClass = that.findClass().getOrNull();
    if (thatClass == null) {
      return false;
    }
    if (thisClass.equals(thatClass)) {
      // TODO FIXME (2019-04-22) erasure
      return true;
    }
    if ((accessPolicy() == AccessPolicy.LOCAL || accessPolicy() == AccessPolicy.PROTECTED)
        && adaptor.packageOf(thisClass).equals(adaptor.packageOf(thatClass))) {
      return true;
    }
    if (accessPolicy() == AccessPolicy.PROTECTED) {
      // TODO FIXME (2019-04-22) erasure
      return adaptor.isSubtypeOf(thatClass.type(), thisClass.type());
    }
    return false;
  }

  public boolean canOverride(CElement<S, T> that, Adaptor<S, T> adaptor) {
    if (kind() != Kind.METHOD || that.kind() != Kind.METHOD) {
      return false;
    }
    if (!methodSignature().equals(that.methodSignature())) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (!adaptor.isSubtypeOf(type(), that.type())) {
      return false;
    }
    return true;
  }

  @Value.Lazy
  public Tuple2<String, Seq<CElement<?, T>>> methodSignature() {
    return Tuple.of(
        name(),
        parameters().zipWithIndex()
            .map(tpl -> tpl.map1(p -> p.withoutSource().withName("arg" + tpl._2)))
            .map(Tuple2::_1));
  }

  @SuppressWarnings({"unchecked", "RedundantSuppression"})
  public CElement<?, T> withoutSource() {
    return ((CElement<Object, T>) this).withSource(PSEUDO_SOURCE);
  }

  public <ES extends S> ES source(Class<ES> type) throws InconsistentModelException {
    try {
      return type.cast(source());
    } catch (ClassCastException e) {
      throw new InconsistentModelException("Source of type " + type.getName() + " expected", this, e);
    }
  }

  public <ET extends T> ET type(Class<ET> type) throws InconsistentModelException {
    try {
      return type.cast(type());
    } catch (ClassCastException e) {
      throw new InconsistentModelException("Type reference of type " + type.getName() + " expected", this, e);
    }
  }

  public boolean isInnerClass() {
    if (kind() != Kind.CLASS) {
      return false;
    }
    var c = this;
    while (c.parentOption().isDefined()) {
      if (c.parent().kind() != CElement.Kind.CLASS) {
        return true;
      } else if (!c.isStatic()) {
        return true;
      }
      c = c.parent();
    }
    return false;
  }

  public CElement<S, T> narrow(Kind kind) {
    if (kind() != kind) {
      throw new InconsistentModelException("Expected kind " + kind + ", actual kind is " + kind(), this);
    }
    return (CElement<S, T>)this;
  }

  public <ES extends S, ET extends T> CElement<ES, ET> narrow(@Nullable Class<ES> sourceType, @Nullable Class<ET> typeType) {
    return narrow(null, sourceType, typeType);
  }

  @SuppressWarnings({"unchecked", "RedundantSuppression"})
  public <ES extends S, ET extends T> CElement<ES, ET> narrow(@Nullable Kind kind, @Nullable Class<ES> sourceType, @Nullable Class<ET> typeType) {
    if (kind != null && kind() != kind) {
      throw new InconsistentModelException("Expected kind " + kind + ", actual kind is " + kind(), this);
    }
    if (sourceType != null) {
      source(sourceType);
    }
    if (typeType != null) {
      type(typeType);
    }
    return (CElement<ES, ET>) this;
  }

  @SuppressWarnings("unchecked")
  public CElement<None<Void>, None<Void>> detach() {
    return ((CElement.Builder<None<Void>, None<Void>>) CElement.<S, T>builder().from(this))
        .source(voidNone()).type(voidNone())
        .parent(parentOption().map(CElement::detach))
        .parameters(parameters().map(CElement::detach))
        .build();
  }

  public ConfigurationConfig<S> configurationConfig() {
    return requireConfig(configurationConfigOption(), Configuration.class);
  }

  @SuppressWarnings("unchecked")
  public Option<ConfigurationConfig<S>> configurationConfigOption() {
    return configs().find(ConfigurationConfig.class::isInstance).map(ConfigurationConfig.class::cast);
  }

  public SetupConfig<S> setupConfig() {
    return requireConfig(setupConfigOption(), Setup.class);
  }

  @SuppressWarnings("unchecked")
  public Option<SetupConfig<S>> setupConfigOption() {
    return configs().find(SetupConfig.class::isInstance).map(SetupConfig.class::cast);
  }

  public ParameterConfig<S> parameterConfig() {
    return requireConfig(parameterConfigOption(), Parameter.class);
  }

  @SuppressWarnings("unchecked")
  public Option<ParameterConfig<S>> parameterConfigOption() {
    return configs().find(ParameterConfig.class::isInstance).map(ParameterConfig.class::cast);
  }

  public ParameterPrefixConfig<S> parameterPrefixConfig() {
    return requireConfig(parameterPrefixConfigOption(), Parameter.Prefix.class);
  }

  @SuppressWarnings("unchecked")
  public Option<ParameterPrefixConfig<S>> parameterPrefixConfigOption() {
    return configs().find(ParameterPrefixConfig.class::isInstance).map(ParameterPrefixConfig.class::cast);
  }

  public ExtensionPointAcceptorConfig<S> extensionPointAcceptorConfig() {
    return requireConfig(extensionPointAcceptorConfigOption(), ExtensionPoint.Acceptor.class);
  }

  @SuppressWarnings("unchecked")
  public Option<ExtensionPointAcceptorConfig<S>> extensionPointAcceptorConfigOption() {
    return configs().find(ExtensionPointAcceptorConfig.class::isInstance).map(ExtensionPointAcceptorConfig.class::cast);
  }

  public ExtensionPointConfig<S> extensionPointConfig() {
    return requireConfig(extensionPointConfigOption(), ExtensionPoint.class);
  }

  @SuppressWarnings("unchecked")
  public Option<ExtensionPointConfig<S>> extensionPointConfigOption() {
    return configs().find(ExtensionPointConfig.class::isInstance).map(ExtensionPointConfig.class::cast);
  }

  public FeatureConfig<S> featureConfig() {
    return requireConfig(featureConfigOption(), Feature.class);
  }

  @SuppressWarnings("unchecked")
  public Option<FeatureConfig<S>> featureConfigOption() {
    return configs().find(FeatureConfig.class::isInstance).map(FeatureConfig.class::cast);
  }

  public MountConfig<S> mountConfig() {
    return requireConfig(mountConfigOption(), Feature.Mount.class);
  }

  @SuppressWarnings("unchecked")
  public Option<MountConfig<S>> mountConfigOption() {
    return configs().find(MountConfig.class::isInstance).map(MountConfig.class::cast);
  }

  public ProvisionConfig<S> provisionConfig() {
    return requireConfig(provisionConfigOption(), Provision.class);
  }

  @SuppressWarnings("unchecked")
  public Option<ProvisionConfig<S>> provisionConfigOption() {
    return configs().find(ProvisionConfig.class::isInstance).map(ProvisionConfig.class::cast);
  }

  private <C> C requireConfig(Option<C> option, Class<? extends Annotation> type) {
    return option.getOrElseThrow(() -> new InconsistentModelException(
        "Expected configuration of type @" + type.getName() + "not present", this));
  }

  @Value.Check
  void verify() {
    kind().verify(this);
  }

  public enum Kind {
    CLASS {
      @Override
      public void verify(_CElement<?, ?> element) {
        super.verify(element);
        verifyOptionalParent(element, CLASS);
        verifyNoParameters(element);
      }
    },
    METHOD {
      @Override
      public void verify(_CElement<?, ?> element) {
        super.verify(element);
        verifyParent(element, CLASS);
//        element.parameters().forEach(p -> {
//          if (!p.parent().equals(element)) {
//            throw new InconsistentModelException("The parent of method parameters must be the method", p);
//          }
//        });
      }
    },
    PARAMETER {
      @Override
      public void verify(_CElement<?, ?> element) {
        super.verify(element);
        verifyNoParameters(element);
        if (element.parentOption().isDefined()) {
          throw new InconsistentModelException("Method parameters must not have a parent", element);
        }
      }
    };

    void verify(_CElement<?, ?> element) {
      element.narrow(this);
    }

    static void verifyNoParameters(_CElement<?, ?> element) {
      if (!element.parameters().isEmpty()) {
        throw new InconsistentModelException("Elements of kind " + element.kind() + " cannot have parameters", element);
      }
    }

    static void verifyOptionalParent(_CElement<?, ?> element, Kind kind) {
      element.parentOption().forEach(p -> {
        if (p.kind() != kind) {
          throw new InconsistentModelException("Parent of kind " + p.kind() + " must be a " + kind, element);
        }
      });
    }

    static void verifyParent(_CElement<?, ?> element, Kind kind) {
      if (element.parentOption().isEmpty()) {
        throw new InconsistentModelException("Elements of kind " + element.kind() + " must have a parent", element);
      }
      verifyOptionalParent(element, kind);
    }

  }

  static abstract class Builder<S, T> {
    public CElement.Builder<S, T> parent(CElement<S, T> parent) {
      return parentOption(parent);
    }

    public CElement.Builder<S, T> parent(Option<CElement<S, T>> parent) {
      return parentOption(parent);
    }

    public abstract CElement.Builder<S, T> parentOption(Option<CElement<S, T>> parent);
    public abstract CElement.Builder<S, T> parentOption(CElement<S, T> parent);
  }

  private static None<Void> voidNone() {
    return (None<Void>)Option.<Void>none();
  }

}
