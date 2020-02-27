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

package ch.raffael.meldioc.library.http.server.undertow.codec;

import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

/**
 * Write a Java object as HTTP response body.
 */
public interface HttpEncoder<C, R> {

  void encode(HttpServerExchange exchange, C ctx, R value);

  static String receiveFullString(HttpServerExchange exchange, Charset charset) throws HttpStatusException {
    return receive((f, err) -> exchange.getRequestReceiver().receiveFullString(
        (ex, data) -> f.complete(data), err, charset));
  }

  static byte[] receiveFullBytes(HttpServerExchange exchange, Charset charset) throws HttpStatusException {
    return receive((f, err) -> exchange.getRequestReceiver().receiveFullBytes(
        (ex, data) -> f.complete(data), err));
  }

  private static <T> T receive(BiConsumer<CompletableFuture<T>, Receiver.ErrorCallback> completer)
      throws HttpStatusException {
    var future = new CompletableFuture<T>();
    completer.accept(future, (ex, err) -> future.completeExceptionally(HttpStatusException.serverError(err)));
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw HttpStatusException.serverError(e);
    } catch (ExecutionException e) {
      throw e.getCause() == null ? HttpStatusException.serverError(e) : HttpStatusException.serverError(e.getCause());
    }
  }
}
