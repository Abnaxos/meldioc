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

package ch.raffael.compose.modules.http.jetty;

import ch.raffael.compose.modules.http.FilterMapping;
import ch.raffael.compose.util.Exceptions;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class FilterWrapper<C> implements Filter {

  private final FilterMapping<? super C> mapping;

  FilterWrapper(FilterMapping<? super C> mapping) {
    this.mapping = mapping;
  }

  @Override
  public void init(@Nonnull FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(@Nonnull ServletRequest request, @Nonnull ServletResponse response, @Nonnull FilterChain chain) throws IOException, ServletException {
    try {
      mapping.target().apply().filter(DefaultJettyHttpModule.getRequestContext(request),
          (HttpServletRequest) request, (HttpServletResponse) response, chain::doFilter);
    } catch (Throwable e) {
      throw Exceptions.alwaysRethrow(e, ServletException.class, IOException.class,
          e2 -> new ServletException(e2.toString(), e2));
    }
  }

  @Override
  public void destroy() {
  }

  @Override
  public String toString() {
    return "HandlerFilter{" +
        "mapping=" + mapping +
        '}';
  }
}
