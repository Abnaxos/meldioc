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

package ch.raffael.compose.http.undertow.routing;

import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import java.util.function.Supplier;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public abstract class RoutingDefinitionInfrastructure {

  Tier currentTier = new Tier(None());

  RoutingDefinitionInfrastructure() {
  }

  void runBlock(Block._0 block) {
    currentTier = new Tier(Some(currentTier));
    try {
      block.run();
    } finally {
      currentTier = currentTier.parent.get();
    }
  }

  <V1> void runBlock(Block._1<? super V1> block, Value<? extends V1> val1) {
    currentTier = new Tier(Some(currentTier));
    try {
      block.run(val1);
    } finally {
      currentTier = currentTier.parent.get();
    }
  }

  <V1, V2> void runBlock(Block._2<? super V1, ? super V2> block, Value<? extends V1> val1, Value<? extends V2> val2) {
    currentTier = new Tier(Some(currentTier));
    try {
      block.run(val1, val2);
    } finally {
      currentTier = currentTier.parent.get();
    }
  }

  <V1, V2, V3> void runBlock(Block._3<? super V1, ? super V2, ? super V3> block, Value<? extends V1> val1, Value<? extends V2> val2, Value<? extends V3> val3) {
    currentTier = new Tier(Some(currentTier));
    try {
      block.run(val1, val2, val3);
    } finally {
      currentTier = currentTier.parent.get();
    }
  }

  public final static class Block {

    private Block() {
    }

    @FunctionalInterface
    public interface _0 {
      void run();
    }

    @FunctionalInterface
    public interface _1<V1> {
      void run(Value<? extends V1> val);
    }

    @FunctionalInterface
    public interface _2<V1, V2> {
      void run(Value<? extends V1> val1, Value<? extends V2> val2);
    }

    @FunctionalInterface
    public interface _3<V1, V2, V3> {
      void run(Value<? extends V1> val1, Value<? extends V2> val2, Value<? extends V3> val3);
    }

  }

  public static abstract class Value<T> {
  }

  public static abstract class Template {

    private Template() {
    }

    public static final class _0 extends Template {
      private _0() {
      }

      public _1<Integer> asInt() {
        // TODO FIXME (2019-06-29) implement
        return null;
      }
      public _1<String> asString() {
        // TODO FIXME (2019-06-29) implement
        return null;
      }
    }

    public static final class _1<T1> extends Template {
      private _1() {
      }

      public _2<T1, Integer> andInt() {
        // TODO FIXME (2019-06-29) implement
        return null;
      }
      public _2<T1, String> andString() {
        // TODO FIXME (2019-06-29) implement
        return null;
      }
    }

    public static final class _2<T1, T2> extends Template {
      private _2() {
      }
    }

  }

  public static abstract class Query {

    public abstract OptionalValue<Integer> asInt();

    public abstract OptionalValue<String> asString();

    public static abstract class OptionalValue<T> extends Value<Option<T>> {
      public abstract Value<T> required();
      public abstract Value<T> orElse(T orElse);
      public abstract Value<T> orElse(Supplier<T> orElse);

      public abstract OptionalCollectionValue<T, Seq<T>> list();
      public abstract OptionalCollectionValue<T, Set<T>> set();

      @SafeVarargs
      public final Value<Seq<T>> list(T... orElse) {
        return list(Array.of(orElse));
      }
      @SafeVarargs
      public final Value<Set<T>> set(T... orElse) {
        return set(HashSet.of(orElse));
      }
      public Value<Seq<T>> list(Seq<? extends T> orElse) {
        return list(() -> orElse);
      }
      public Value<Set<T>> set(Set<? extends T> orElse) {
        return set(() -> orElse);
      }
      public abstract Value<Seq<T>> list(Supplier<? extends Seq<? extends T>> orElse);

      public abstract Value<Set<T>> set(Supplier<? extends Set<? extends T>> orElse);
    }

    public static abstract class OptionalCollectionValue<T, C extends Traversable<T>> extends Value<C> {
      abstract Value<C> nonEmpty();
    }

  }

  class Tier {
    private final Option<Tier> parent;

    Tier(Option<Tier> parent) {
      this.parent = parent;
    }
  }


}
