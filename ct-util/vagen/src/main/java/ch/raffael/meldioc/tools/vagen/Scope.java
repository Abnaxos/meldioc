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

package ch.raffael.meldioc.tools.vagen;

import ch.raffael.meldioc.tools.vagen.expr.CompositeBinding;
import ch.raffael.meldioc.tools.vagen.tree.ErrorNode;
import groovy.lang.Binding;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

import java.util.Objects;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public final class Scope {

  private final Option<? extends Scope> parent;
  private final Binding binding;

  private final Substitution.All allSubstitutions;
  private Substitution.First currentGroup;

  private Seq<String> errors = List.empty();

  public Scope() {
    this(none());
  }

  @SuppressWarnings("CopyConstructorMissesField")
  public Scope(Scope parent) {
    this(some(Objects.requireNonNull(parent, "parent")));
  }

  public Scope(Option<? extends Scope> parent) {
    this.parent = parent;
    this.binding = new CompositeBinding(
        parent.map(Scope::binding).map(List::of).getOrElse(List.empty())
            .prepend(new Binding()));
    currentGroup = new Substitution.First();
    allSubstitutions = new Substitution.All(List.of(currentGroup));
  }

  public Substitution.Result applySubstitutions(String line) {
    return applySubstitutions(Substitution.Result.initial(line));
  }

  public Substitution.Result applySubstitutions(Substitution.Result line) {
    try {
      var res = allSubstitutions.apply(this, line);
      return parent.map(p -> p.applySubstitutions(res)).getOrElse(res);
    } catch (Exception e) {
      error(e.toString());
      return new Substitution.Result(false, ErrorNode.errorMessage(e));
    }
  }

  public void addSubstitution(Substitution.MatchMode mode, String match, String replacement) {
    currentGroup.append(new Substitution.Groovy(mode, match, replacement));
  }

  public void newSubstitutionGroup() {
    if (!currentGroup.isEmpty()) {
      currentGroup = new Substitution.First();
      allSubstitutions.append(currentGroup);
    }
  }

  public void error(String message) {
    errors = errors.append(message);
    parent.forEach(p -> p.error(message));
  }

  public Seq<String> errors() {
    return errors;
  }

  public Option<? extends Scope> parent() {
    return parent;
  }

  public Scope root() {
    return Stream.iterate(some(this), s -> s.flatMap(Scope::parent)).takeWhile(Option::isDefined).last().get();
  }

  public Binding binding() {
    return binding;
  }
}
