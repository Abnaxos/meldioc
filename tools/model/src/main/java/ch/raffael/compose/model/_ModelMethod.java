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
import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.immutables.value.Value;

/**
 * Rich representation of a method participating in the compose model.
 */
@Immutable.Public
abstract class _ModelMethod<S, T> {

  @Value.Parameter
  public abstract CElement<S, T> element();

  @Value.Parameter
  public abstract ModelType<S, T> modelType();

  public abstract Seq<ModelMethod<S, T>> overrides();

  public abstract Option<ModelMethod<S, T>> via();

  @Value.Default
  public boolean implied() {
    return false;
  }

  @Value.Auxiliary
  public abstract Seq<Either<ModelMethod<S, T>, BuiltinArgument>> arguments();

  @Value.Auxiliary
  public abstract Seq<Message<S, T>> messages();

  public abstract ModelMethod<S, T> withMessages(Seq<Message<S, T>> value);

  ModelMethod<S, T> addMessage(MessageSink<S, T> sink, Message<S, T> msg) {
    sink.message(msg);
    return this.withMessages(messages().append(msg));
  }

  ModelMethod<S, T> addMessages(MessageSink<S, T> sink, Iterable<? extends Message<S, T>> msg) {
    msg.forEach(sink::message);
    return this.withMessages(messages().appendAll(msg));
  }

}
