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

package ch.raffael.meldioc.tools.dslgen.tree;

import ch.raffael.meldioc.tools.dslgen.Scope;
import ch.raffael.meldioc.tools.dslgen.expr.Expressions;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.codehaus.groovy.runtime.NullObject;

import javax.annotation.Nonnull;
import java.util.Iterator;

import static io.vavr.control.Option.some;
import static java.util.Objects.requireNonNullElse;

/**
 * TODO JavaDoc
 */
public class BlockNode extends ListNode {

  private final static Object NO_EXPR = new Object();

  private final Loop loop;

  public BlockNode(Loop loop) {
    super(loop.toString());
    this.loop = loop;
  }

  @Override
  public Stream<String> lines(Scope scope) {
    class Err {
      Object err = "Unspecified error";
    }
    var object = requireNonNullElse(loop.expr.map(e -> {
          try {
            return Expressions.eval(scope.binding(), e);
          } catch (Exception ex) {
            var err = new Err();
            err.err = ex;
            return err;
          }
        }).getOrElse(NO_EXPR),
        NullObject.getNullObject());
    if (object instanceof Err) {
      return Stream.of(ErrorNode.errorMessage(((Err) object).err));
    }
    Iterable<?> iterable;
    if (object instanceof Iterator) {
      iterable = iterableOfIterator((Iterator<?>) object);
    } else if (object instanceof Iterable) {
      iterable = (Iterable<?>) object;
    } else {
      iterable = Expressions.isGroovyTrue(object) ? List.of(object) : List.empty();
    }
    return Stream.<Object>ofAll(iterable).zipWithIndex().flatMap(tpl -> {
      var v = tpl._1();
      var s = new Scope(some(scope));
      loop.ident.forEach(id -> s.binding().setVariable(id, v));
      var result = BlockNode.super.lines(s);
      return loop.separate && tpl._2() > 0 ? result.prepend("") : result;
    });
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  private Iterable<Object> iterableOfIterator(Iterator<?> object) {
    return () -> (Iterator<Object>) object;
  }

  public static final class Loop {
    final Option<String> ident;
    final Option<String> expr;
    final boolean separate;

    public Loop(Option<String> ident, Option<String> expr, boolean separate) {
      this.ident = ident;
      this.expr = expr;
      this.separate = separate;
    }

    @Override
    public String toString() {
      return "Loop[" + ident + ":" + expr + "]";
    }
  }
}
