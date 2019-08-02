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

package ch.raffael.compose.http.undertow.codec;

/**
 * Represents an empty body and provides codecs for it.
 */
public final class EmptyBody {
  @SuppressWarnings("InstantiationOfUtilityClass")
  public static final EmptyBody INSTANCE = new EmptyBody();

  private static Decoder<Object, EmptyBody> DECODER = (ex, __, c) -> c.accept(ex, instance());
  private static Encoder<Object, EmptyBody> ENCODER = (ex, __, ___) -> ex.endExchange();

  private EmptyBody() {
  }

  public static EmptyBody instance() {
    return INSTANCE;
  }

  public static EmptyBody empty() {
    return INSTANCE;
  }

  public static Decoder<Object, EmptyBody> decoder() {
    return DECODER;
  }

  public static Encoder<Object, EmptyBody> encoder() {
    return ENCODER;
  }
}
