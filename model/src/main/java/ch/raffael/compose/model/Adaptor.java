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

import ch.raffael.compose.model.messages.MessageSink;
import io.vavr.collection.Seq;

/**
 * TODO JavaDoc
 */
public interface Adaptor<S, T> {

  boolean isSubtypeOf(T left, T right);

  default boolean isAssignableTo(T left, T right) {
    return isSubtypeOf(left, right);
  }

  boolean isReference(T type);

  boolean isPrimitive(T type);

  default boolean isNoType(T type) {
    return !isReference(type) && !isPrimitive(type);
  }

  boolean isEnumType(T type);

  // https://docs.oracle.com/javase/specs/jls/se12/html/jls-8.html#jls-8.4.2
  boolean canOverride(CElement<S, T> left, CElement<S, T> right);

  /**
   * @return A type or a ({@link #isNoType(Object) NoType} if not found.
   */
  T typeOf(ClassRef ref);

  CElement<S, T> classElement(T type);

  Seq<? extends T> superTypes(T type, MessageSink messages);

  Seq<CElement<S, T>> declaredMethods(T type, MessageSink messages);

  String packageOf(CElement<S, T> element);

  T iterableOf(T componentType);

  T collectionOf(T componentType);

  T listOf(T componentType);

  T componentTypeOfIterable(T iterableType);

}
