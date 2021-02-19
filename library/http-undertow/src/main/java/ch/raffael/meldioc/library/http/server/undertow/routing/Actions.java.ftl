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

package ch.raffael.meldioc.library.http.server.undertow.routing;
[#compress]
  [#import "/parameters.ftl" as p]
  [#import "/codegen.ftl" as c]
  [#import "actions.ftl" as a]
[/#compress]

public final class Actions {

  private Actions() {
  }

  [#list ["of", "action"] as name]
    [@a.action_literals name 1 false /]
  [/#list]

  [@a.actions; v]
    [@c.indent -2]
      @FunctionalInterface
      public interface ${v.type} {
        [@c.verbose]
            // as argument: ${v.arg_type}
        [/@c.verbose]
        ${v.void?then("void", "R")} perform(${v.all_params?map(e -> e.full)?join(", ")}) throws Exception;
      }
    [/@c.indent]

  [/@a.actions]
}
