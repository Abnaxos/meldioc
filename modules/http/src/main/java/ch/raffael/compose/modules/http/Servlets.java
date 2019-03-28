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
import io.vavr.API;
import io.vavr.CheckedFunction0;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;

import javax.servlet.DispatcherType;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

/**
 * @since 2019-03-23
 */
@ExtensionPoint
public interface Servlets {

  HandlerMapper handle(String pathSpec);

  FilterMapper filter(String pathSpec);

  interface HandlerMapper {
    HandlerMapper name(String name);
    void with(CheckedFunction0<? extends Handler> handlerFun);
    default void with(Handler handler) {
      with(CheckedFunction0.constant(handler));
    }
    default void withSupplier(Supplier<? extends Handler> handlerFun) {
      with(handlerFun::get);
    }
  }

  interface FilterMapper {
    FilterMapper name(String name);
    FilterMapper dispatch(Iterable<DispatcherType> dispatch);
    default FilterMapper dispatch(DispatcherType... dispatch) {
      return dispatch(asList(dispatch));
    }
    void through(CheckedFunction0<? extends Filter> filterFun);
    default void through(Filter filter) {
      through(CheckedFunction0.constant(filter));
    }
    default void throughSupplier(Supplier<? extends Filter> filterFun) {
      through(filterFun::get);
    }
  }

  @ExtensionPoint
  class Default implements Servlets {
    public static final Set<DispatcherType> DEFAULT_DISPATCH = API.Set(DispatcherType.REQUEST);

    private AtomicReference<LinkedHashSet<HandlerMapping>> handlerMappings =
        new AtomicReference<>(LinkedHashSet.empty());
    private AtomicReference<LinkedHashSet<FilterMapping>> filterMappings =
        new AtomicReference<>(LinkedHashSet.empty());
    @Override
    public HandlerMapper handle(String pathSpec) {
      return new HandlerMapper() {
        private final HandlerMapping.Builder mapping = HandlerMapping.builder().pathSpec(pathSpec);
        @Override
        public HandlerMapper name(String name) {
          mapping.name(name);
          return this;
        }
        @Override
        public void with(CheckedFunction0<? extends Handler> handlerFun) {
          handlerMappings.updateAndGet(m -> m.add(mapping.target(handlerFun).build()));
        }
      };
    }
    @Override
    public FilterMapper filter(String pathSpec) {
      return new FilterMapper() {
        private final FilterMapping.Builder mapping = FilterMapping.builder()
            .pathSpec(pathSpec)
            .dispatch(DEFAULT_DISPATCH);
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
        public void through(CheckedFunction0<? extends Filter> filterFun) {
          filterMappings.updateAndGet(m -> m.add(mapping.target(filterFun).build()));
        }
      };
    }
    public Set<HandlerMapping> handlerMappings() {
      return handlerMappings.get();
    }
    public Set<FilterMapping> filterMappings() {
      return filterMappings.get();
    }
  }

}
