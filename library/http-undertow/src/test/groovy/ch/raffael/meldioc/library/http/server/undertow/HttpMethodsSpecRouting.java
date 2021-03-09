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

package ch.raffael.meldioc.library.http.server.undertow;

import ch.raffael.meldioc.library.http.server.undertow.routing.RoutingDefinition;
import ch.raffael.meldioc.library.http.server.undertow.util.RequestContexts;
import io.vavr.control.Option;

public class HttpMethodsSpecRouting extends RoutingDefinition<RequestContexts.Empty> {

  public HttpMethodsSpecRouting() {
    path("echo").route(() -> {
      var head = query("head").asString();
      post("post").accept(codec().plainText())
          .map(head, this::echo)
          .respond(codec().plainText());
      put("put").accept(codec().plainText())
          .map(head, this::echo)
          .respond(codec().plainText());
      put("put-post").post().accept(codec().plainText())
          .map(head, this::echo)
          .respond(codec().plainText());
      get("get")
          .map(() -> "get")
          .map(head, this::echo)
          .respond(codec().plainText());
    });
  }

  private String echo(String text, Option<String> head) {
    return "ECHO: " + head.map(h -> h + "; ").getOrElse("") + text;
  }
}
