/*
 *  Copyright (c) 2020 Raffael Herzog
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

package ch.raffael.meldioc.library.codec;

import io.vavr.control.Option;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

public interface ObjectCodecFactory {

  default <T> Option<ObjectEncoder<T>> encoder(Class<T> type) {
    return encoder(type, none());
  }

  default <T> Option<ObjectEncoder<T>> encoder(Class<T> type, ContentType contentType) {
    return encoder(type, some(contentType));
  }

  <T> Option<ObjectEncoder<T>> encoder(Class<T> type, Option<ContentType> contentType);

  default <T> Option<ObjectDecoder<T>> decoder(Class<T> type) {
    return decoder(none(), type);
  }

  default <T> Option<ObjectDecoder<T>> decoder(ContentType contentType, Class<T> type) {
    return decoder(some(contentType), type);
  }

  <T> Option<ObjectDecoder<T>> decoder(Option<ContentType> contentType, Class<T> type);

  boolean canEncode(Class<?> type);
  boolean canEncodeAs(ContentType contentType);
  boolean canDecode(ContentType contentType);
  boolean canDecodeAs(Class<?> type);

  boolean isInvalidInput(Throwable exception);
}
