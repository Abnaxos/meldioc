/*
 *  Copyright (c) 2022 Raffael Herzog
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
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatus;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public final class HttpStatusSpecRouting extends RoutingDefinition {

  private static final boolean VERBOSE_THROW = false;

  public HttpStatusSpecRouting() {
    path("s").captureString("mode").route(modeString -> {
      var mode = modeString.map(s -> HashSet.of(s.split(",")));
      //noinspection Convert2MethodRef
      get()
          .map(() -> List.empty())
          .map(b -> b.append("ok"))
          .status(mode, m -> m.contains("accepted") ? some(HttpStatus.ACCEPTED) : none())
          .map(mode, (b, m) -> {
            if (m.contains("throwNonRecoverable")) {
              throw throwing(new NonRecoverable());
            }
            return b.append("ok");
          })
          .exceptionStatus(NonRecoverable.class, mode,
              m -> m.contains("notExtended") ? some(HttpStatus.NOT_EXTENDED) : none())
          .map(mode, (b, m) -> {
            if (m.contains("throwRecoverable")) {
              throw throwing(new Recoverable());
            }
            return b.append("ok");
          })
          .exceptionStatus(Recoverable.class, mode,
              m -> m.contains("recoverStatus") ? some(HttpStatus.MULTI_STATUS) : none())
          .recover(Recoverable.class, mode, m -> {
            if (m.contains("failRecovery")) {
              throw throwing(new NonRecoverable());
            } else {
              return List.of("recovered");
            }
          })
          .map(b -> b.mkString(","))
          .respond(codec().plainText());
    });
  }

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private <T extends Throwable> T throwing(T throwing) {
    if (VERBOSE_THROW) {
      System.out.print("Throwing: ");
      throwing.printStackTrace(System.out);
    }
    return throwing;
  }

  static class NonRecoverable extends Exception {
  }
  static class Recoverable extends Exception {
  }
}
