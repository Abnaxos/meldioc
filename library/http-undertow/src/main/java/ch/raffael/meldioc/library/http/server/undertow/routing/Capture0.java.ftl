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

  [#list 0..*(p.pcount) as i]
  public ${c.tvars(["R"]+(1..*i), "P#")} Capture<R> map([@c.squash]
    ${c.joinf(1..*i, "Capture<? extends P#> p#", "#, ")}
    Action${i+1}R${c.tvars(["<:T"]+(1..*i)+[">:R"], "<:P#")} action
  [/@c.squash]) {
    [#if i==0]
    return map(name() + "'", ${c.joinf(1..*i, "p#", "#, ")} action);
    [#else]
    return map("(" + String.join("+", name()${c.joinf(1..*i, "p#.name()", ", #")}) + ")'",
      ${c.joinf(1..*i, "p#", "#, ")} action);
    [/#if]
  }

  public ${c.tvars(["R"]+(1..*i), "P#")} Capture<R> map([@c.squash]
    String name,
    ${c.joinf(1..*i, "Capture<? extends P#> p#", "#, ")}
    Action${i+1}R${c.tvars(["<:T"]+(1..*i)+[">:R"], "<:P#")} action
  [/@c.squash]) {
    return new Capture.Mapped<>(name, x ->
        action.perform(Capture0.this.get(x)${c.joinf(1..*i, "p#.get(x)", ", #")}));
  }

  [/#list]
}
