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

package ch.raffael.compose.http.undertow;

import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.http.undertow.routing.RoutingDefinition;
import io.vavr.collection.Seq;

import java.util.concurrent.atomic.AtomicReference;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
@ExtensionPoint.Acceptor
public interface HttpRouter<C> {

  HttpRouter route(RoutingDefinition<? super C> routingDef);

  @ExtensionPoint.Acceptor
  class Default<C> {
    private final AtomicReference<Seq<RoutingDefinition<? super C>>> definitions = new AtomicReference<>(Seq());
    private final HttpRouter<C> api = new HttpRouter<>() {
      @Override
      public HttpRouter route(RoutingDefinition<? super C> routingDef) {
        definitions.updateAndGet(d -> d.append(routingDef));
        return this;
      }
    };

    public HttpRouter<C> api() {
      return api;
    }

    public Seq<RoutingDefinition<? super C>> definitions() {
      return definitions.get();
    }
  }

}
