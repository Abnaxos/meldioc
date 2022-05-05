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

package ch.raffael.meldioc.usecases.undertow.hello;

import ch.raffael.meldioc.util.immutables.PureImmutable;
import org.immutables.gson.Gson;

import java.time.Instant;

@PureImmutable
@Gson.TypeAdapters
public abstract class RestHelloResponse implements RestHelloResponse_With {

  RestHelloResponse() {}

  public static Builder builder() {
    return RestHelloResponse_Immutable.builder();
  }

  public abstract String message();

  public abstract Instant timestamp();

  public static abstract class Builder {
    Builder() {}
    public abstract Builder from(RestHelloResponse instance);
    public abstract Builder message(String message);
    public abstract Builder timestamp(Instant timestamp);
    public abstract RestHelloResponse build();
  }
}
