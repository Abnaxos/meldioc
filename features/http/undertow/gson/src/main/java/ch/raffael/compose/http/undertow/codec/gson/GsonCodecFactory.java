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

package ch.raffael.compose.http.undertow.codec.gson;

import ch.raffael.compose.http.undertow.HttpStatusException;
import ch.raffael.compose.http.undertow.codec.Decoder;
import ch.raffael.compose.http.undertow.codec.Encoder;
import ch.raffael.compose.http.undertow.codec.ObjectCodecFactory;
import ch.raffael.compose.http.undertow.codec.StringCodec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import io.vavr.control.Option;

import java.util.ServiceLoader;

import static io.vavr.API.*;

public class GsonCodecFactory implements ObjectCodecFactory<Object> {

  private final Gson gson;

  public GsonCodecFactory(Gson gson) {
    this.gson = gson;
  }

  public GsonCodecFactory() {
    this(standardGsonBuilder().create());
  }

  public static GsonBuilder standardGsonBuilder() {
    return loadServiceLoaderTypeAdapters(new GsonBuilder());
  }

  public static GsonBuilder loadServiceLoaderTypeAdapters(GsonBuilder builder) {
    return loadServiceLoaderTypeAdapters(GsonCodecFactory.class, builder);
  }

  public static GsonBuilder loadServiceLoaderTypeAdapters(Class<?> baseClass, GsonBuilder builder) {
    ServiceLoader.load(TypeAdapterFactory.class, baseClass.getClassLoader()).stream()
        .map(ServiceLoader.Provider::get)
        .forEach(builder::registerTypeAdapterFactory);
    return builder;
  }

  @Override
  public <T> Option<Encoder<Object, ? super T>> encoder(Class<T> type) {
    return Some((ex, ctx, obj) -> StringCodec.json().encode(ex, ctx, gson.toJson(obj)));
  }

  @Override
  public <T> Option<Decoder<Object, ? extends T>> decoder(Class<T> type) {
    return Some((ex, ctx, sink) -> StringCodec.json().decode(ex, ctx,
        (ex2, json) -> {
          var object = gson.fromJson(json, type);
          if (object == null) {
            HttpStatusException.endRequestWithBadRequest(ex2, "No content");
          } else {
            sink.accept(ex2, object);
          }
        }));
  }
}
