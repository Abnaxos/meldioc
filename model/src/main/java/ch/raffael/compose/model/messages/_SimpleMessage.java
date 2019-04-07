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

import ch.raffael.compose.model.Element;
import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Seq;
import org.immutables.value.Value;

/**
 * TODO JavaDoc
 */
@Immutable.Public
abstract class _SimpleMessage<S, T> implements Message<S, T> {

  @Override
  @Value.Parameter
  public abstract Element<S, T> element();

  @Override
  @Value.Parameter
  public abstract String message();

  @Override
  public abstract Seq<Element<S, T>> messageArgs();

  @Override
  @Value.Default
  public boolean languageError() {
    return false;
  }

  public abstract SimpleMessage<S, T> withLanguageError(boolean value);

  public SimpleMessage<S, T> asLanguageError() {
    return withLanguageError(true);
  }
}
