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

import io.vavr.collection.List;
import io.vavr.collection.Seq;

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
    // TODO (2020-04-11) WTF! It doesn't work with sets, there must be something wrong with the hash codes
    // With set on the duplicate message:
    //  - contains() returns false, add() true
    //  - but when checking with iterator, we find one where equals()==true
    // As a workaround, we can use lists, it doesn't really matter. But something's wrong somewhere.
    // I couldn't find the problem in my code -- maybe something in javax.lang.model?
    private Seq<Message<S, T>> seen = List.empty();
    private final Object lock = new Object();
    private final MessageSink<S, T> delegate;

    public UniqueWrapper(MessageSink<S, T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void message(Message<S, T> message) {
      boolean report;
      synchronized (lock) {
        report = !seen.contains(message);
        if (report) {
          seen = seen.append(message);
        }
      }
      if (report) {
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
