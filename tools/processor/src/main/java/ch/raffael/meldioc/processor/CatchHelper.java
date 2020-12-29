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

package ch.raffael.meldioc.processor;

import ch.raffael.meldioc.processor.env.Environment;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static ch.raffael.meldioc.processor.util.Elements.asDeclaredType;

final class CatchHelper extends Environment.WithEnv {

  private Set<DeclaredType> declared;
  private boolean catchAll = false;

  CatchHelper(Environment env) {
    super(env);
    declared = LinkedHashSet.empty();
  }

  CatchHelper add(Stream<? extends TypeMirror> add) {
    add.forEach(this::add);
    return this;
  }

  CatchHelper add(TypeMirror add) {
    if (!(add instanceof DeclaredType)) {
      throw new IllegalArgumentException("Executable type expected: add=" + add);
    }
    if (catchAll) {
      return this;
    }
    if (env.types().isSameType(add, env.known().throwable())) {
      catchAll = true;
      declared = LinkedHashSet.of(env.known().throwable());
      return this;
    }
    if (isUnchecked(asDeclaredType(add))) {
      return this;
    }
    declared = declared.reject(t -> env.types().isSubtype(t, add));
    if (!declared.exists(t -> env.types().isSubtype(add, t))) {
      declared = declared.add(asDeclaredType(add));
    }
    return this;
  }

  boolean catchAll() {
    return catchAll;
  }

  CatchHelper withChecked(Consumer<? super Traversable<? super DeclaredType>> consumer) {
    if (declared.nonEmpty()) {
      consumer.accept(declared);
    }
    return this;
  }

  CatchHelper forEachChecked(Consumer<? super DeclaredType> consumer) {
    declared.forEach(consumer);
    return this;
  }

  CatchHelper withUnchecked(Consumer<? super Traversable<? super DeclaredType>> consumer) {
    Option.of(unchecked())
        .filter(Traversable::nonEmpty)
        .forEach(consumer);
    return this;
  }

  CatchHelper forEachUnchecked(Consumer<? super DeclaredType> consumer) {
    unchecked().forEach(consumer);
    return this;
  }

  CatchHelper withAll(Consumer<? super Traversable<? super DeclaredType>> consumer) {
    Option.of(all())
        .filter(Traversable::nonEmpty)
        .forEach(consumer);
    return this;
  }

  CatchHelper forAll(Consumer<? super DeclaredType> consumer) {
    all().forEach(consumer);
    return this;
  }

  CatchHelper ifUnhandled(DeclaredType t, Runnable then) {
    if (!isUnchecked(t) || declared.exists(h -> env.types().isSubtype(t, h))) {
      then.run();
    }
    return this;
  }

  CatchHelper catchAll(Runnable then) {
    ifUnhandled(env.known().throwable(), then);
    return this;
  }

  Traversable<DeclaredType> checked() {
    return declared;
  }

  Traversable<DeclaredType> unchecked() {
    return List.of(env.known().runtimeException(), env.known().error())
        .filter(t -> !declared.exists(h -> env.types().isSubtype(t, h)));
  }

  Traversable<DeclaredType> all() {
    return declared.addAll(unchecked());
  }

  private boolean isUnchecked(DeclaredType t) {
    return env.types().isSubtype(t, env.known().runtimeException())
        || env.types().isSubtype(t, env.known().error());
  }
}
