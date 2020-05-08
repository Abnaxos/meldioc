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

package ch.raffael.meldioc.util.advice;

import ch.raffael.meldioc.util.Exceptions;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import java.util.function.Function;
import java.util.function.Supplier;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * TODO JavaDoc
 */
public class CompositeAroundAdvice extends AroundAdvice.Basic {

  private final Runnable before;
  private final Runnable after;

  public CompositeAroundAdvice(Runnable before, Runnable after) {
    this.before = before;
    this.after = after;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected void beforeImpl() {
    before.run();
  }

  @Override
  protected void afterImpl() {
    after.run();
  }

  public static abstract class AbstractConfiguration<SELF> {
    private static final Runnable BEFORE_TERMINATOR = () -> {};
    private static final Function<Option<AdviceException>, Option<AdviceException>> AFTER_TERMINATOR = Function.identity();

    private Seq<Supplier<? extends Runnable>> before = List.of();
    private Seq<Supplier<? extends Runnable>> after = List.of();

    public SELF before(Supplier<? extends Runnable> hook) {
      before = before.append(hook);
      return self();
    }

    public SELF after(Supplier<? extends Runnable> hook) {
      after = after.append(hook);
      return self();
    }

    public SELF removeBefore(Supplier<? extends Runnable> hook) {
      before = before.remove(hook);
      return self();
    }

    public SELF removeAfter(Supplier<? extends Runnable> hook) {
      after = after.remove(hook);
      return self();
    }

    @SuppressWarnings("unchecked")
    protected final SELF self() {
      return (SELF) this;
    }

    protected Seq<Supplier<? extends Runnable>> before() {
      return before;
    }

    protected Seq<Supplier<? extends Runnable>> after() {
      return after;
    }

    protected CompositeAroundAdvice advice() {
      return new CompositeAroundAdvice(beforeRunnable(), afterRunnable());
    }

    protected Runnable beforeRunnable() {
      return before.map(Supplier::get).foldLeft(BEFORE_TERMINATOR, (n, r) ->
          () -> {
            n.run();
            try {
              r.run();
            } catch (Throwable e) {
              throw AdviceException.onBefore(e);
            }
          });
    }

    protected Runnable afterRunnable() {
      var teardownChain = after.map(Supplier::get).foldRight(AFTER_TERMINATOR, (r, n) ->
          exc -> {
            exc = n.apply(exc);
            try {
              r.run();
              return exc;
            } catch (Throwable e) {
              Exceptions.rethrowIfFatal(e, exc.getOrNull());
              return exc.map(ae -> {
                ae.addSuppressed(e);
                return ae;
              }).orElse(() -> some(AdviceException.onAfter(e)));
            }
          });
      return () -> teardownChain.apply(none()).forEach(e -> {
        throw e;
      });
    }
  }

  public static class Builder extends AbstractConfiguration<Builder> {
    @Override
    public CompositeAroundAdvice advice() {
      return super.advice();
    }
  }
}
