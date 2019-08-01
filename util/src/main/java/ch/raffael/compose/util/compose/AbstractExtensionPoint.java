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

package ch.raffael.compose.util.compose;

import ch.raffael.compose.util.concurrent.SafePublisher;

/**
 * Base class for extension points. It's not very useful, but does establish
 * some common pattern. It also wraps the acceptor class with a {@link
 * SafePublisher}.
 *
 * <p>The general pattern is as follows:
 *
 * <pre>
 * &#64;ExtensionPoint
 * public class MyAcceptor {
 *
 *   public static class EP extends AbstractExtensionPoint&lt;MyAcceptor, Result&gt; {
 *
 *     &#64;Override
 *     public void accept() {
 *       return accept(publishedAcceptor());
 *     }
 *     &#64;Override
 *     protected void accept(MyAcceptor acceptor) {
 *       return new Result(acceptor);
 *     }
 *   }
 * }
 * </pre>
 */
public abstract class AbstractExtensionPoint<T, R> {

  private final SafePublisher<T> acceptor;

  public AbstractExtensionPoint(T acceptor) {
    this.acceptor = SafePublisher.of(acceptor);
  }

  public T acceptor() {
    return acceptor.unsafe();
  }

  public T publishedAcceptor() {
    return acceptor.published();
  }

  public R apply() throws Exception {
    return apply(publishedAcceptor());
  }

  protected abstract R apply(T acceptor) throws Exception;

}
