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

package ch.raffael.meldioc.tools.dslgen;

import ch.raffael.meldioc.tools.dslgen.expr.CompositeBinding;
import ch.raffael.meldioc.tools.dslgen.expr.Expressions;
import groovy.lang.Binding;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class Substitution {

  Substitution() {
  }

  public Result apply(Scope scope, String input) {
    return apply(scope, new Result(false, input));
  }

  public abstract Result apply(Scope scope, Result input);

  public abstract boolean isEmpty();

  abstract String description();

  @Override
  public String toString() {
    return "Substitution." + getClass().getSimpleName() + "[" + description() + "]";
  }

  public static final class Result {
    private final boolean hit;
    private final String string;

    public Result(boolean hit, String string) {
      this.string = string;
      this.hit = hit;
    }

    public static Result initial(String string) {
      return new Result(false, string);
    }

    public String string() {
      return string;
    }

    public boolean isHit() {
      return hit;
    }

    public boolean isMiss() {
      return !hit;
    }

    public Result hit() {
      return hit(string);
    }

    public Result hit(String string) {
      return new Result(true, string);
    }

    public Result miss() {
      return miss(string);
    }

    public Result miss(String string) {
      return new Result(hit, string);
    }

    public Result forceMiss() {
      return new Result(false, string);
    }

    public Result forceMiss(String string) {
      return new Result(false, string);
    }

    public Result rejoin(Result original) {
      return new Result(original.hit || hit, string);
    }

    @Override
    public String toString() {
      return "Substitution.Result[" + (hit ? "hit" : "miss") + ":" + string + "]";
    }
  }

  public static class Groovy extends Substitution {

    private final MatchMode mode;
    private final String match;
    private final String replacement;

    public Groovy(MatchMode mode, String match, String replacement) {
      this.mode = mode;
      this.match = match;
      this.replacement = replacement;
    }

    @Override
    public Result apply(Scope scope, Result input) {
      StringBuilder result = null;
      var matcher = mode.toPattern(Expressions.stringOrEval(scope.binding(), match)).matcher(input.string());
      var start = 0;
      while (matcher.find()) {
        if (result == null) {
          result = new StringBuilder();
        }
        result.append(input.string(), start, matcher.start());
        var b = new CompositeBinding(
            new Binding(Stream.rangeClosed(0, matcher.groupCount())
                .toMap(g -> "_" + g, g -> Objects.requireNonNullElse(matcher.group(g), ""))
                .toJavaMap()),
            scope.binding());
        String r;
        try {
          r = Expressions.stringOrEval(b, replacement);
        } catch (Exception e) {
          return input.miss("[[ERROR]] " + replacement + ": " + e);
        }
        result.append(r);
        start = matcher.end();
      }
      if (result != null) {
        result.append(input.string(), start, input.string().length());
        return input.hit(result.toString());
      } else {
        return input.miss();
      }
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    String description() {
      return match;
    }
  }

  public static class First extends Substitution {

    private Seq<Substitution> substitutions;

    public First() {
      this(List.empty());
    }

    public First(Seq<Substitution> substitutions) {
      this.substitutions = substitutions;
    }

    public static First of(Substitution... substitutions) {
      return new First(List.of(substitutions));
    }

    public static First of(Iterable<? extends Substitution> substitutions) {
      return new First(List.ofAll(substitutions));
    }

    @Override
    public Result apply(Scope scope, Result input) {
      if (substitutions.isEmpty()) {
        return input.miss();
      }
      return substitutions.toStream()
          .foldLeft(input.forceMiss(), (p, s) -> p.isMiss() ? s.apply(scope, p) : p)
          .rejoin(input);
    }

    @Override
    public boolean isEmpty() {
      return substitutions.isEmpty();
    }

    public void append(Substitution... substitutions) {
      this.substitutions = this.substitutions.appendAll(List.of(substitutions));
    }

    public void append(Iterable<? extends Substitution> substitutions) {
      this.substitutions = this.substitutions.appendAll(substitutions);
    }

    @Override
    String description() {
      return substitutions.mkString(",");
    }
  }

  public static class All extends Substitution {

    private Seq<Substitution> substitutions;


    public All() {
      this(List.empty());
    }
    public All(Seq<Substitution> substitutions) {
      this.substitutions = substitutions;
    }

    public static All of(Substitution... substitutions) {
      return new All(List.of(substitutions));
    }

    public static All of(Iterable<? extends Substitution> substitutions) {
      return new All(List.ofAll(substitutions));
    }

    @Override
    public Result apply(Scope scope, Result input) {
      if (substitutions.isEmpty()) {
        return input;
      }
      var res = input;
      for (var s : substitutions) {
        res = s.apply(scope, res);
      }
      return res;
//      return substitutions.toStream()
//          .foldLeft(Option.none(), (i, s) -> s.apply(i.getOrElse(input)));
    }

    @Override
    public boolean isEmpty() {
      return substitutions.isEmpty();
    }

    public void append(Substitution... substitutions) {
      this.substitutions = this.substitutions.appendAll(List.of(substitutions));
    }

    public void append(Iterable<? extends Substitution> substitutions) {
      this.substitutions = this.substitutions.appendAll(substitutions);
    }

    @Override
    String description() {
      return substitutions.mkString(",");
    }
  }

  public enum MatchMode {

    PLAIN(s -> Pattern.compile(Pattern.quote(s))),
    REGEX(Pattern::compile);

    private final Function<? super String, ? extends Pattern> toPattern;

    MatchMode(Function<? super String, ? extends Pattern> toPattern) {
      this.toPattern = toPattern;
    }

    public Pattern toPattern(String pattern) {
      return toPattern.apply(pattern);
    }
  }
}
