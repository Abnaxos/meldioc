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

package ch.raffael.compose.model;

import ch.raffael.compose.model.messages.Message;
import ch.raffael.compose.model.messages.MessageSink;
import io.vavr.API;
import io.vavr.Lazy;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;
import io.vavr.control.Option;

import javax.annotation.Nullable;

import static io.vavr.API.*;

/**
 * TODO JavaDoc
 */
public final class Model<S, T> implements MessageSink<S, T> {

  private static final Seq<ConfigRef<ClassRef>> STANDARD_CONFIG_REFS = Seq(
      ConfigRef.of(ClassRef.Primitives.INT, "getInt"),
      ConfigRef.of(ClassRef.Primitives.LONG, "getLong"),
      ConfigRef.of(ClassRef.Primitives.DOUBLE, "getDouble"),
      ConfigRef.of(ClassRef.Primitives.BOOLEAN, "getBoolean"),
      ConfigRef.of(ClassRef.Lang.INTEGER, "getInt"),
      ConfigRef.of(ClassRef.Lang.LONG, "getLong"),
      ConfigRef.of(ClassRef.Lang.DOUBLE, "getDouble"),
      ConfigRef.of(ClassRef.Lang.BOOLEAN, "getBoolean"),
      ConfigRef.of(ClassRef.Lang.NUMBER, "getNumber"),
      ConfigRef.of(ClassRef.Lang.STRING, "getString"),
      ConfigRef.of(ClassRef.Lang.CHAR_SEQUENCE, "getString"),
      ConfigRef.of(ClassRef.of("java.time", "Duration"), "getDuration"),
      ConfigRef.of(ClassRef.of("java.time", "Period"), "getPeriod"),
      ConfigRef.of(ClassRef.of("java.time", "TemporalAmount"), "getTemporal"),
      ConfigRef.of(ClassRef.of("com.typesafe.config", "Config"), "getConfig"),
      ConfigRef.of(ClassRef.of("com.typesafe.config", "ConfigMemorySize"), "getMemorySize"),
      ConfigRef.of(ClassRef.Lang.OBJECT, "getAnyRef"));

  private final Adaptor<S, T> adaptor;
  private final MessageSink<S, T> messages;
  private HashMap<T, Entry<S, T>> pool = HashMap.empty();

  private final T objectType;
  private final T enumType;
  private final Lazy<Seq<CElement<S, T>>> objectMethods;
  private final Seq<ConfigRef<T>> configSupportedTypes;

  private Model(Adaptor<S, T> adaptor, MessageSink<S, T> messages) {
    this.adaptor = adaptor;
    this.messages = MessageSink.uniqueWrapper(messages);
    this.objectType = adaptor.typeOf(ClassRef.Lang.OBJECT);
    objectMethods = API.Lazy(() -> this.adaptor.declaredMethods(this.objectType, this.messages)
        .map(m -> m.narrow(_CElement.Kind.METHOD))
        .map(m -> m.withConfigs(API.Set())));
    this.enumType = adaptor.typeOf(ClassRef.Lang.ENUM);
    configSupportedTypes = STANDARD_CONFIG_REFS
        .map(cr -> ConfigRef.of(adaptor.typeOf(cr.type()), cr.configMethodName()))
        .filter(cr -> !adaptor.isNoType(cr.type()))
        .flatMap(cr -> Seq(
            cr.withType(adaptor.listOf(cr.type())), cr.withType(adaptor.collectionOf(cr.type())),
            cr.withType(adaptor.iterableOf(cr.type())), cr))
        .filter(cr -> !adaptor.isNoType(cr.type()));
  }

  public static <S, T> Model create(Adaptor<S, T> adaptor, MessageSink<S, T> messages) {
    return new Model<>(adaptor, messages);
  }

  public static <S, T, A extends Adaptor<S, T> & MessageSink<S, T>> Model create(A adaptor) {
    return create(adaptor, adaptor);
  }

  public ComposeType<S, T> modelOf(T type) {
    return pool.computeIfAbsent(type, e -> new Entry<>())
        .apply((e, m) -> {
          //noinspection SynchronizationOnLocalVariableOrMethodParameter
          synchronized (e) {
            pool = m;
            if (e.model == null) {
              if (e.initializing) {
                throw new IllegalStateException("Model initialization recursion detected on type " + type);
              }
              e.initializing = true;
              try {
                e.model = new ComposeType<>(this, type);
              } finally {
                e.initializing = false;
              }
            }
            return e.model;
          }
        });
  }

  public Adaptor<S, T> adaptor() {
    return adaptor;
  }

  public T objectType() {
    return objectType;
  }

  public T enumType() {
    return enumType;
  }

  public Seq<CElement<S, T>> objectMethods() {
    return objectMethods.get();
  }

  public Option<ConfigRef<T>> configSupportedType(T type) {
    if (adaptor.isEnumType(type)) {
      return Some(ConfigRef.of(type, "getEnum").withAddTargetTypeArgument(true));
    }
    var componentType = adaptor.componentTypeOfIterable(type);
    if (adaptor.isEnumType(componentType)) {
      if (adaptor.isSubtypeOf(type, adaptor.listOf(componentType))
          || adaptor.isSubtypeOf(type, adaptor.collectionOf(componentType))
          || adaptor.isSubtypeOf(type, adaptor.iterableOf(componentType))) {
        return Some(ConfigRef.of(componentType, "getEnumList"));
      }
    }
    return configSupportedTypes
        .filter(t -> adaptor.isSubtypeOf(type, t.type()))
        .headOption();
  }

  @Override
  public void message(Message<S, T> message) {
    messages.message(message);
  }

  private final static class Entry<S, T> {
    private boolean initializing = false;
    @Nullable
    private ComposeType<S, T> model;
  }

}
