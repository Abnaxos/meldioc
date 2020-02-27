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

package ch.raffael.meldioc.library.http.server.undertow.routing;

import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.util.Objects;

/**
 * A capture of some value from the request (path segment, query parameter).
 *
 * <p>The name if usually irrelevant and it's no problem to reuse the same
 * name multiple times. It's just to be able to insert something meaningful
 * in error messages.
 */
public abstract class Capture<T> {

  private final String name;

  Capture(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  abstract T get(HttpServerExchange exchange) throws HttpStatusException;

  public static final class Attachment<T> extends Capture<T> {
    private final AttachmentKey<String> key = AttachmentKey.create(String.class);
    private final Converter<? extends T> converter;

    Attachment(String name, Converter<? extends T> converter) {
      super(name);
      this.converter = converter;
    }

    T get(HttpServerExchange exchange) throws HttpStatusException {
      return Objects.requireNonNullElseGet(converter.convert(name(), exchange.getAttachment(key)),
          () -> {
            throw new IllegalStateException("No captured value for '" + name() + "'");
          });
    }

    void capture(HttpServerExchange exchange, String value) {
      exchange.putAttachment(key, value);
    }
  }

}
