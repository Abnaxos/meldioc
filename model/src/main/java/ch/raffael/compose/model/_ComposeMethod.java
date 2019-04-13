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
import org.immutables.value.Value;

/**
 * TODO JavaDoc
 */
@Immutable.Local
abstract class _ComposeMethod<S, T> {

  @Value.Parameter
  abstract CElement<S, T> method();

  abstract Seq<ComposeMethod<S, T>> overrides();

  @Value.Auxiliary
  abstract Seq<Message<S, T>> messages();

  public abstract ComposeMethod<S, T> withMessages(Seq<Message<S, T>> value);

  ComposeMethod<S, T> addMessage(MessageSink<S, T> sink, Message<S, T> msg) {
    sink.message(msg);
    return this.withMessages(messages().append(msg));
  }

  ComposeMethod<S, T> addMessages(MessageSink<S, T> sink, Iterable<? extends Message<S, T>> msg) {
    msg.forEach(sink::message);
    return this.withMessages(messages().appendAll(msg));
  }

}
