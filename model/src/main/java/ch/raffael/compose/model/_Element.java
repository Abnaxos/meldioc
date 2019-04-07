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

import ch.raffael.compose.model.config.ElementConfig;
import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * TODO JavaDoc
 */
@Immutable.Public
public abstract class _Element<S, T> {

  _Element() {
  }

  public abstract Kind kind();

  public abstract S source();

  public Element<S, T> parent() {
    return parentOption().getOrElseThrow(() -> new InconsistentModelException("Element " + this + " has no parent", this));
  }

  public abstract Option<Element<S, T>> parentOption();

  public abstract String name();

  public abstract T type();

  public abstract AccessPolicy accessPolicy();

  public abstract boolean isStatic();

  public abstract boolean isFinal();

  public abstract boolean isAbstract();

  public boolean isOverridable() {
    return !isFinal() && !isStatic() && accessPolicy() != AccessPolicy.PRIVATE;
  }

  public abstract Seq<Element<S, T>> parameters();

  public abstract Set<ElementConfig> configs();

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

  public Element<S, T> narrow(Kind kind) {
    if (kind() != kind) {
      throw new InconsistentModelException("Expected kind " + kind + ", actual kind is " + kind(), this);
    }
    return (Element<S, T>)this;
  }

  public <ES extends S, ET extends T> Element<ES, ET> narrow(@Nullable Class<ES> sourceType, @Nullable Class<ET> typeType) {
    return narrow(null, sourceType, typeType);
  }

  @SuppressWarnings({"unchecked", "RedundantSuppression"})
  public <ES extends S, ET extends T> Element<ES, ET> narrow(@Nullable Kind kind, @Nullable Class<ES> sourceType, @Nullable Class<ET> typeType) {
    if (kind != null && kind() != kind) {
      throw new InconsistentModelException("Expected kind " + kind + ", actual kind is " + kind(), this);
    }
    if (sourceType != null) {
      source(sourceType);
    }
    if (typeType != null) {
      type(typeType);
    }
    return (Element<ES, ET>) this;
  }

  @Value.Check
  void verify() {
    kind().verify(this);
  }

  public enum Kind {
    CLASS {
      @Override
      public void verify(_Element<?, ?> element) {
        super.verify(element);
        verifyOptionalParent(element, CLASS);
        verifyNoParameters(element);
      }
    },
    METHOD {
      @Override
      public void verify(_Element<?, ?> element) {
        super.verify(element);
        verifyParent(element, CLASS);
        element.parameters().forEach(p -> {
          if (!p.parent().equals(element)) {
            throw new InconsistentModelException("The parent of method parameters must be the method", p);
          }
        });
      }
    },
    PARAMETER {
      @Override
      public void verify(_Element<?, ?> element) {
        super.verify(element);
        verifyNoParameters(element);
        Kind.verifyParent(element, METHOD);
      }
    };

    void verify(_Element<?, ?> element) {
      element.narrow(this);
    }

    static void verifyNoParameters(_Element<?, ?> element) {
      if (!element.parameters().isEmpty()) {
        throw new InconsistentModelException("Elements of kind " + element.kind() + " cannot have parameters", element);
      }
    }

    static void verifyOptionalParent(_Element<?, ?> element, Kind kind) {
      element.parentOption().forEach(p -> {
        if (p.kind() != kind) {
          throw new InconsistentModelException("Parent of kind " + p.kind() + " must be a " + kind, element);
        }
      });
    }

    static void verifyParent(_Element<?, ?> element, Kind kind) {
      if (element.parentOption().isEmpty()) {
        throw new InconsistentModelException("Elements of kind " + element.kind() + " must have a parent", element);
      }
      verifyOptionalParent(element, kind);
    }

  }

  static abstract class Builder<S, T> {
    public Element.Builder parent(Element<S, T> parent) {
      return parentOption(parent);
    }

    public Element.Builder parent(Option<Element<S, T>> parent) {
      return parentOption(parent);
    }

    public abstract Element.Builder parentOption(Option<Element<S, T>> parent);
    public abstract Element.Builder parentOption(Element<S, T> parent);
  }

}
