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

package ch.raffael.meldioc.model.messages;

import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.util.immutables.PureImmutable;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.immutables.value.Value;

import static io.vavr.control.Option.some;

/**
 * A standard implementation of {@link Message}.
 */
@PureImmutable
@SuppressWarnings("varargs") // Bug in immutables or immutables-vavr: the builder methods are not annotated correctly
public abstract class SimpleMessage<S, T> implements Message<S, T>, SimpleMessage_With<S, T> {

  @Override
  @Value.Parameter
  public abstract Option<Id> id();

  @Override
  @Value.Parameter
  public abstract SrcElement<S, T> element();

  @Override
  @Value.Parameter
  public abstract String message();

  @Override
  @Value.Parameter
  public abstract Seq<SrcElement<S, T>> conflicts();

  @Override
  @Value.Default
  public boolean languageError() {
    return false;
  }

  public static <S, T> Builder<S, T> builder() {
    return SimpleMessage_Immutable.builder();
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <S, T> SimpleMessage<S, T> of(Message.Id id, SrcElement<S, T> element, String message, SrcElement<S, T>... conflicts) {
    return SimpleMessage_Immutable.of(some(id), element, message, List.of(conflicts));
  }

  public static <S, T> SimpleMessage<S, T> of(Message.Id id, SrcElement<S, T> element, String message, Seq<SrcElement<S, T>> conflicts) {
    return SimpleMessage_Immutable.of(some(id), element, message, conflicts);
  }

  public static <S, T> SimpleMessage<S, T> of(Message.Id id, SrcElement<S, T> element, String message) {
    return SimpleMessage_Immutable.of(some(id), element, message, List.empty());
  }

  public abstract SimpleMessage<S, T> withLanguageError(boolean value);

  public static abstract class Builder<S, T> {
    Builder() {}
    public abstract SimpleMessage_Immutable.Builder<S, T> from(SimpleMessage<S, T> instance);
    public abstract SimpleMessage_Immutable.Builder<S, T> addOrigins(Message<S, T> element);
    public abstract SimpleMessage_Immutable.Builder<S, T> addAllOrigins(Iterable<Message<S, T>> element);
    public abstract SimpleMessage_Immutable.Builder<S, T> origins(Seq<Message<S, T>> elements);
    public abstract SimpleMessage_Immutable.Builder<S, T> setIterableOrigins(Iterable<Message<S, T>> elements);
    public abstract SimpleMessage_Immutable.Builder<S, T> id(Option<Id> opt);
    public abstract SimpleMessage_Immutable.Builder<S, T> id(Id x);
    public abstract SimpleMessage_Immutable.Builder<S, T> element(SrcElement<S, T> element);
    public abstract SimpleMessage_Immutable.Builder<S, T> message(String message);
    public abstract SimpleMessage_Immutable.Builder<S, T> addConflicts(SrcElement<S, T> element);
    public abstract SimpleMessage_Immutable.Builder<S, T> addAllConflicts(Iterable<SrcElement<S, T>> element);
    public abstract SimpleMessage_Immutable.Builder<S, T> conflicts(Seq<SrcElement<S, T>> elements);
    public abstract SimpleMessage_Immutable.Builder<S, T> setIterableConflicts(Iterable<SrcElement<S, T>> elements);
    public abstract SimpleMessage_Immutable.Builder<S, T> languageError(boolean languageError);
    public abstract SimpleMessage_Immutable<S, T> build();
  }
}
