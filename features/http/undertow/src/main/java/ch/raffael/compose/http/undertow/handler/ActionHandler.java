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

package ch.raffael.compose.http.undertow.handler;

import ch.raffael.compose.http.undertow.codec.Decoder;
import ch.raffael.compose.http.undertow.codec.Encoder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.function.Function;

/**
 * Handler for actions written in the routing DSL.
 */
public class ActionHandler<C, B, R> implements HttpHandler {

  private final Decoder<? super C, ? extends B> decoder;
  private final Encoder<? super C, ? super R> encoder;
  private final Function<? super HttpServerExchange, ? extends C> context;
  private final Invoker<? super C, ? super B, ? extends R> invoker;

  public ActionHandler(Decoder<? super C, ? extends B> decoder, Encoder<? super C, ? super R> encoder,
                       Function<? super HttpServerExchange, ? extends C> context,
                       Invoker<? super C, ? super B, ? extends R> invoker) {
    this.decoder = decoder;
    this.encoder = encoder;
    this.context = context;
    this.invoker = invoker;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    C ctx = context.apply(exchange);
    decoder.decode(exchange, ctx, (ex, body) -> {
        R response = invoker.invoke(ex, ctx, body);
        encoder.encode(ex, ctx, response);
    });
  }

  public interface Invoker<C, B, R> {
    R invoke(HttpServerExchange exchange, C context, B body) throws Exception;
  }

}
