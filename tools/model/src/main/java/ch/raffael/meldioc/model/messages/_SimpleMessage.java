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

package ch.raffael.meldioc.model.messages;

import ch.raffael.meldioc.model.CElement;
import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.immutables.value.Value;

import static io.vavr.control.Option.some;

/**
 * A standard implementation of {@link Message}.
 */
@Immutable.Public
@SuppressWarnings("varargs") // Bug in immutables or immutables-vavr: the builder methods are not annotated correctly
abstract class _SimpleMessage<S, T> implements Message<S, T> {

  @Override
  @Value.Parameter
  public abstract Option<Id> id();

  @Override
  @Value.Parameter
  public abstract CElement<S, T> element();

  @Override
  @Value.Parameter
  public abstract String message();

  @Override
  @Value.Parameter
  public abstract Seq<CElement<S, T>> conflicts();

  @Override
  @Value.Default
  public boolean languageError() {
    return false;
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <S, T> SimpleMessage<S, T> of(Message.Id id, CElement<S, T> element, String message, CElement<S, T>... conflicts) {
    return SimpleMessage.of(some(id), element, message, List.of(conflicts));
  }

  public static <S, T> SimpleMessage<S, T> of(Message.Id id, CElement<S, T> element, String message, Seq<CElement<S, T>> conflicts) {
    return SimpleMessage.of(some(id), element, message, conflicts);
  }

  public static <S, T> SimpleMessage<S, T> of(Message.Id id, CElement<S, T> element, String message) {
    return SimpleMessage.of(some(id), element, message, List.empty());
  }

  public abstract SimpleMessage<S, T> withLanguageError(boolean value);

}
