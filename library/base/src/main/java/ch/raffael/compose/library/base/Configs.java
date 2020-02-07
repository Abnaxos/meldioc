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

package ch.raffael.compose.library.base;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import io.vavr.Tuple2;
import io.vavr.control.Option;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.vavr.API.*;

/**
 * Some utilities for dealing with Typesafe Config.
 */
public class Configs {

  private Configs() {
  }

  public static Properties toProperties(Config config) {
    return toProperties(config, new Properties());
  }

  public static Properties toProperties(Config config, Properties target) {
    config.entrySet().stream()
        .filter(e -> e.getValue().valueType() != ConfigValueType.NULL)
        .forEach(e -> target.setProperty(e.getKey(), e.getValue().unwrapped().toString()));
    return target;
  }

  public static <M extends Map<? super String, Object>> M toMap(Config config, M target) {
    config.entrySet().stream()
        .filter(e -> e.getValue().valueType() != ConfigValueType.NULL)
        .forEach(e -> target.put(e.getKey(), e.getValue().unwrapped()));
    return target;
  }

  public static Map<String, Object> toMap(Config config) {
    return toMap(config, new LinkedHashMap<>());
  }

  public static io.vavr.collection.Map<String, Object> toVavrMap(Config config) {
    return config.entrySet().stream()
        .filter(e -> e.getValue().valueType() != ConfigValueType.NULL)
        .map(e -> Tuple(e.getKey(), e.getValue().unwrapped()))
        .collect(io.vavr.collection.LinkedHashMap.collector(Tuple2::_1, Tuple2::_2));
  }

  public static <T> void configure(Config config,
                                   BiFunction<? super Config, ? super String, ? extends T> getter,
                                   String path,
                                   Consumer<? super T> configurer) {
    if (config.hasPath(path)) {
      configurer.accept(getter.apply(config, path));
    }
  }

  public static <T> Option<T> option(Config config,
                                     BiFunction<? super Config, ? super String, ? extends T> getter,
                                     String path) {
    return config.hasPath(path) ? Some(getter.apply(config, path)) : None();
  }

}
