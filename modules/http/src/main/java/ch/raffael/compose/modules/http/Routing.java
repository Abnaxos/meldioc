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

package ch.raffael.compose.modules.http;

import ch.raffael.compose.ExtensionPoint;
import io.vavr.CheckedFunction0;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;

import javax.servlet.DispatcherType;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

/**
 * @since 2019-03-23
 */
@ExtensionPoint
public interface Routing {

  HandlerMapper map(String pathSpec);

  FilterMapper filter(String pathSpec);

  interface HandlerMapper {
    HandlerMapper name(String name);
    void to(CheckedFunction0<? extends Handler> handler);
    default void toSupplier(Supplier<? extends Handler> handler) {
      to(handler::get);
    }
    default void to(Handler handler) {
      CheckedFunction0.constant(handler);
    }
  }

  interface FilterMapper {
    FilterMapper name(String name);
    FilterMapper dispatch(Iterable<DispatcherType> dispatch);
    default FilterMapper dispatch(DispatcherType... dispatch) {
      return dispatch(asList(dispatch));
    }
    void to(CheckedFunction0<? extends Filter> filter);
    default void throughSupplier(Supplier<? extends Filter> filter) {
      to(filter::get);
    }
    default void through(Handler filter) {
      CheckedFunction0.constant(filter);
    }
  }

  @ExtensionPoint
  class Default implements Routing {
    private LinkedHashSet<HandlerMapping> handlerMappings = LinkedHashSet.empty();
    private LinkedHashSet<FilterMapping> filterMappings = LinkedHashSet.empty();
    @Override
    public HandlerMapper map(String pathSpec) {
      return new HandlerMapper() {
        private final HandlerMapping.Builder mapping = HandlerMapping.builder().pathSpec(pathSpec);
        @Override
        public HandlerMapper name(String name) {
          mapping.name(name);
          return this;
        }
        @Override
        public void to(CheckedFunction0<? extends Handler> handler) {
          handlerMappings = handlerMappings.add(mapping.target(handler).build());
        }
      };
    }
    @Override
    public FilterMapper filter(String pathSpec) {
      return new FilterMapper() {
        private final FilterMapping.Builder mapping = FilterMapping.builder().pathSpec(pathSpec);
        @Override
        public FilterMapper name(String name) {
          mapping.name(name);
          return this;
        }
        @Override
        public FilterMapper dispatch(Iterable<DispatcherType> dispatch) {
          mapping.addAllDispatch(dispatch);
          return this;
        }
        @Override
        public void to(CheckedFunction0<? extends Filter> filter) {
          filterMappings = filterMappings.add(mapping.target(filter).build());
        }
      };
    }
    public Set<HandlerMapping> handlerMappings() {
      return handlerMappings;
    }
    public Set<FilterMapping> filterMappings() {
      return filterMappings;
    }
  }

}
