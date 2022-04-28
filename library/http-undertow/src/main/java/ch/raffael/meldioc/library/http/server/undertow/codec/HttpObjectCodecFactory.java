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

package ch.raffael.meldioc.library.http.server.undertow.codec;

import ch.raffael.meldioc.library.codec.ContentType;
import ch.raffael.meldioc.library.codec.ContentTypes;
import ch.raffael.meldioc.library.codec.ObjectCodecFactory;
import ch.raffael.meldioc.library.codec.ObjectEncoder;
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatusException;
import io.undertow.util.Headers;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import java.nio.ByteBuffer;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

/**
 * Factory for encoders and decoders that marshal/unmarshal Java objects
 * (like JSON or XML, i.e. mostly for REST).
 */
public interface HttpObjectCodecFactory {

  <T> Option<HttpDecoder<? extends T>> decoder(Class<T> type);

  <T> Option<HttpEncoder<? super T>> encoder(Class<T> type);

  static HttpObjectCodecFactory wrapBuffered(ObjectCodecFactory factory) {
    return new Adapter(factory);
  }

  class Adapter implements HttpObjectCodecFactory {
    private final ObjectCodecFactory factory;

    public Adapter(ObjectCodecFactory factory) {
      this.factory = factory;
    }

    @Override
    public <T> Option<HttpEncoder<? super T>> encoder(Class<T> type) {
      if (factory.canEncode(type)) {
        return some((exchange, value) -> {
          var encoder = Option.of(exchange.getRequestHeaders().getFirst(Headers.ACCEPT))
              .filter(s -> !s.isBlank())
              .map(ContentTypes::parseContentTypeListQ)
              .<ObjectEncoder<T>>flatMap(ctl -> ctl.foldLeft(none(),
                  (cur, ct) -> cur.orElse(() -> factory.encoder(type, ct))))
              .orElse(() -> factory.encoder(type))
              .getOrElseThrow(() -> new IllegalStateException("No encoder returned"));
          Tuple2<byte[], ContentType> encoded;
          try {
            encoded = encoder.encode(value);
          } catch (Exception e) {
            HttpStatusException.serverError("Error writing response: " + e, e).endRequest(exchange);
            return;
          }
          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, encoded._2.render());
          exchange.getResponseSender().send(ByteBuffer.wrap(encoded._1));
        });
      } else {
        return none();
      }
    }

    @Override
    public <T> Option<HttpDecoder<? extends T>> decoder(Class<T> type) {
      if (factory.canDecodeAs(type)) {
        return some((exchange, consumer) -> exchange.getRequestReceiver().receiveFullBytes((ex, bytes) -> {
          var decoder = factory.decoder(HttpContentTypes.contentType(exchange), type);
          if (decoder.isDefined()) {
            try {
              consumer.accept(exchange, decoder.get().decode(bytes));
            } catch (Exception e) {
              if (decoder.get().isInvalidInput(e)) {
                HttpStatusException.badRequest(e.toString(), e).endRequest(exchange);
              } else {
                HttpStatusException.serverError(e.toString(), e).endRequest(exchange);
              }
            }
          } else {
            HttpStatusException.serverError("No decoder found").endRequest(exchange);
          }
        }, HttpStatusException::endRequestWithServerError));
      } else {
        return none();
      }
    }
  }
}
