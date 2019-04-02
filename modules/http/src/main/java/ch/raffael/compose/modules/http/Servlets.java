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
import ch.raffael.compose.util.Exceptions;
import io.vavr.API;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Set;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

/**
 * TODO javadoc
 */
@ExtensionPoint.Api
public interface Servlets<C> {

  HandlerMapper<C> handle(String pathSpec);

  FilterMapper<C> filter(String pathSpec);

  interface HandlerMapper<C> {
    HandlerMapper name(String name);
    void with(CheckedFunction0<? extends Handler<? super C>> handlerFun);
    default void with(Handler<? super C> handler) {
      with(CheckedFunction0.constant(handler));
    }
    default void with(Handler.IgnoringCtx handler) {
      with(CheckedFunction0.constant(handler));
    }
    default void with(CheckedFunction1<? super C, ? extends Handler<? super C>> handlerFun) {
      with((context, request, response) -> {
        try {
          handlerFun.apply(context).handle(context, request, response);
        } catch (Throwable throwable) {
          throw Exceptions.alwaysRethrow(throwable, ServletException.class, e -> new ServletException(e.toString(), e));
        }
      });
    }
  }

  interface FilterMapper<C> {
    FilterMapper name(String name);
    FilterMapper dispatch(Iterable<DispatcherType> dispatch);
    default FilterMapper dispatch(DispatcherType... dispatch) {
      return dispatch(asList(dispatch));
    }
    void through(CheckedFunction0<? extends Filter<? super C>> filterFun);
    default void through(Filter<? super C> filter) {
      through(CheckedFunction0.constant(filter));
    }
    default void through(Filter.IgnoringCtx filter) {
      through(CheckedFunction0.constant(filter));
    }
    default void trough(CheckedFunction1<? super C, ? extends Filter<? super C>> filterFun) {
      through((context, request, response, next) -> {
        try {
          filterFun.apply(context).filter(context, request, response, next);
        } catch (Throwable throwable) {
          throw Exceptions.alwaysRethrow(throwable, ServletException.class, e -> new ServletException(e.toString(), e));
        }
      });
    }
  }

  @ExtensionPoint.Api
  class Default<C> implements Servlets<C> {
    public static final Set<DispatcherType> DEFAULT_DISPATCH = API.Set(DispatcherType.REQUEST);

    private AtomicReference<LinkedHashSet<HandlerMapping<C>>> handlerMappings =
        new AtomicReference<>(LinkedHashSet.empty());
    private AtomicReference<LinkedHashSet<FilterMapping<C>>> filterMappings =
        new AtomicReference<>(LinkedHashSet.empty());
    @Override
    public HandlerMapper<C> handle(String pathSpec) {
      return new HandlerMapper<>() {
        private final HandlerMapping.Builder<C> mapping = HandlerMapping.<C>builder()
            .pathSpec(pathSpec);
        @Override
        public HandlerMapper name(String name) {
          mapping.name(name);
          return this;
        }
        @Override
        public void with(CheckedFunction0<? extends Handler<? super C>> handlerFun) {
          handlerMappings.updateAndGet(m -> m.add(mapping.target(handlerFun).build()));
        }
      };
    }
    @Override
    public FilterMapper<C> filter(String pathSpec) {
      return new FilterMapper<>() {
        private final FilterMapping.Builder<C> mapping = FilterMapping.<C>builder()
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
        public void through(CheckedFunction0<? extends Filter<? super C>> filterFun) {
          filterMappings.updateAndGet(m -> m.add(mapping.target(filterFun).build()));
        }
      };
    }
    public Set<HandlerMapping<C>> handlerMappings() {
      return handlerMappings.get();
    }
    public Set<FilterMapping<C>> filterMappings() {
      return filterMappings.get();
    }
  }

}
