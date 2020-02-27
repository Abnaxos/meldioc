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

package ch.raffael.meldioc.library.http.server.undertow.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigUtil;
import org.slf4j.Logger;
import org.xnio.Option;
import org.xnio.OptionMap;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static ch.raffael.meldioc.logging.Logging.logger;

public class XnioOptions {

  private static final Logger LOG = logger();

  private final OptionMap.Builder builder;
  private final ClassLoader classLoader;
  private final List<Class<?>> classes;

  public XnioOptions(ClassLoader classLoader, Class<?>... classes) {
    this(OptionMap.builder(), classLoader, classes);
  }

  public XnioOptions(OptionMap.Builder builder, ClassLoader classLoader, Class<?>... classes) {
    this(builder, classLoader, Arrays.asList(classes));
  }

  public XnioOptions(ClassLoader classLoader, Collection<? extends Class<?>> classes) {
    this(OptionMap.builder(), classLoader, classes);
  }

  public XnioOptions(OptionMap.Builder builder, ClassLoader classLoader, Collection<? extends Class<?>> classes) {
    this.builder = builder;
    this.classLoader = classLoader;
    this.classes = List.copyOf(classes);
  }

  public XnioOptions load(Config config) {
    config.entrySet().stream()
        .map(e -> ConfigUtil.splitPath(e.getKey()))
        .filter(p -> p.size() == 1)
        .map(p -> p.get(0))
        .forEach(k -> setOption(k, config.getString(k)));
    return this;
  }

  private XnioOptions setOption(String key, String value) {
    var field = classes.stream()
        .flatMap(c -> {
          try {
            var f = c.getField(key);
            return Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                       && Option.class.isAssignableFrom(f.getType())
                   ? Stream.of(f)
                   : Stream.empty();
          } catch (NoSuchFieldException e) {
            return Stream.empty();
          }
        })
        .findFirst().orElse(null);
    if (field == null) {
      LOG.warn("Unknown option '{}' for classes {}", key, classes);
    } else {
      Option<?> option = null;
      try {
        option = (org.xnio.Option<?>) field.get(null);
      } catch (IllegalAccessException e) {
        LOG.error("Error retrieving option object for '{}' from {}", key, field, e);
      }
      if (option != null) {
        setString(option, value);
      }
    }
    return this;
  }

  public <T> XnioOptions set(org.xnio.Option<T> option, T value) {
    builder.set(option, value);
    return this;
  }

  public <T> XnioOptions setString(org.xnio.Option<T> option, String value) {
    builder.set(option, option.parseValue(value, classLoader));
    return this;
  }

  public OptionMap options() {
    return builder.getMap();
  }

  public void forEach(Consumer consumer) {
    var options = options();
    options.forEach(o -> forwardOption(consumer, options, o));
  }

  private <T> void forwardOption(Consumer consumer, OptionMap map, Option<T> option) {
    consumer.accept(option, map.get(option));
  }

  @FunctionalInterface
  public interface Consumer {
    <T> void accept(Option<T> option, T value);
  }

}
