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

import ch.raffael.compose.http.undertow.codec.Decoder;
import ch.raffael.compose.http.undertow.codec.Encoder;
import ch.raffael.compose.util.VavrX;
import io.vavr.collection.Array;
import io.vavr.collection.Traversable;


/**
 * TODO JavaDoc
 */
public abstract class RoutingDefinition<C> extends RoutingDefinitionInfrastructure {


  public RoutingDefinition() {
  }

  public void path(String path, Block._0 block) {

  }

  public <V1> void path(Template._1<V1> template, Block._1<? super V1> block) {

  }

  public Query query(String name) {
    // TODO FIXME (2019-06-30) implement
    return null;
  }

  public <R> void get(Action._0<? super C, ? extends R> action) {

  }

  public <R, V1> void get(Value<? extends V1> val1, Action._1<? super C, ? super V1, ? extends R> action) {

  }

  public Template._0 template(String template) {
    // TODO FIXME (2019-06-29) implement
    return null;
  }

  public void restrict(Traversable<? extends Role> roles) {
    // TODO FIXME (2019-06-29) implement
    roles.distinct().map(Role::name);
  }

  public void restrict(Iterable<? extends Role> roles) {
    restrict(VavrX.traversableOf(roles));
  }

  public void restrict(Role... roles) {
    restrict(Array.of(roles));
  }

  public void mount(RoutingDefinition<? super C> routingDefinition) {

  }

  public void mount(String path, RoutingDefinition<? super C> routingDefinition) {

  }

  public void codec(Encoder encoder, Decoder<?> decoder) {

  }

  public void codec(Decoder<?> decoder) {

  }

  public void codec(Encoder encoder) {

  }

  public <T> Value<T> body(Class<T> body) {
    // TODO FIXME (2019-06-29) implement
    return null;
  }

  public Value<String> remainingPath() {
    // TODO FIXME (2019-06-29) implement
    return null;
  }

}
