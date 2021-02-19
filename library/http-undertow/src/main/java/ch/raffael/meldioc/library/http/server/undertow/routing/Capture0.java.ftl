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
[#import "/parameters.ftl" as p]
[#import "/codegen.ftl" as c]
[#import "actions.ftl" as a]

import io.undertow.server.HttpServerExchange;

import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;

[@a.import_actions/]

/**
 * TODO JavaDoc
 */
abstract class Capture0<T> {
  { assert this instanceof Capture; }

  abstract String name();
  abstract T get(HttpServerExchange exchange) throws HttpStatusException;

  [@a.actions predef=["T self"] void=false; variant]
    [#assign typevars = c.tvars(["R"]+variant.vararg_params?map(x -> x.type))]
    [#assign capture_types = variant.vararg_params?map(x -> x + {"type": "Capture<? extends ${x.type}>"})]
    [#assign common_args = capture_types?map(x -> "${x.type} ${x.name}") + ["${variant.arg_type} action"]]
    [@c.indent -2]
      public ${typevars} Capture<R> map(${common_args?join(", ")}) {
        return map("("+${(["name()+"]+variant.vararg_params?map(x -> "${x.name}.name()+"))?join("")}")'",
            ${(variant.vararg_params?map(x -> x.name) + ["action"])?join(", ")});
      }
      public ${typevars} Capture<R> map(${(["String name"]+common_args)?join(", ")}) {
        return new Capture.Mapped<>(name, x ->
            action.perform(${(["Capture0.this.get(x)"]+variant.vararg_params?map(x -> "${x.name}.get(x)"))?join(", ")}));
      }
    [/@c.indent]

  [/@a.actions]
}
