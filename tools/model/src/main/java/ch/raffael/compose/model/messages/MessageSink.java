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

package ch.raffael.compose.model.messages;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Provided by the actual tool as a target for all error and warning
 * messages. The processor will use these message to generate compiler
 * errors, an IDEA plugin will use them to show inspections.
 */
public interface MessageSink<S, T> {

  void message(Message<S, T> message);

  default Tracker<S, T> tracker() {
    return new Tracker<>(this);
  }

  static <S, T> UniqueWrapper<S, T> uniqueWrapper(MessageSink<S, T> delegate) {
    return new UniqueWrapper<>(delegate);
  }

  final class UniqueWrapper<S, T> implements MessageSink<S, T> {
    private final java.util.Set<Message<S, T>> seen =
        new ConcurrentHashMap<Message<S, T>, Boolean>(4, .7f, 1).keySet(true);
    private final MessageSink<S, T> delegate;
    public UniqueWrapper(MessageSink<S, T> delegate) {
      this.delegate = delegate;
    }
    @Override
    public void message(Message<S, T> message) {
      if (seen.add(message)) {
        delegate.message(message);
      }
    }
  }

  final class Tracker<S, T> implements MessageSink<S, T> {
    private final MessageSink<S, T> delegate;
    private boolean hasErrors = false;
    public Tracker(MessageSink<S, T> delegate) {
      this.delegate = delegate;
    }
    @Override
    public void message(Message<S, T> message) {
      hasErrors = true;
      delegate.message(message);
    }
    public boolean hasErrors() {
      return hasErrors;
    }
    public boolean ok() {
      return !hasErrors;
    }
  }


}
