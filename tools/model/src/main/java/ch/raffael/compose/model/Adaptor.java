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

import io.vavr.collection.Seq;

/**
 * The adaptor to the underlying AST ({@code javax.lang.model}, PSI).
 */
public interface Adaptor<S, T> {

  /**
   * True, if left is a subtype of right.
   */
  boolean isSubtypeOf(T left, T right);

  /**
   * True, if the type represents a reference type.
   */
  boolean isReference(T type);

  /**
   * True, if the type represents a primitive type ({@code void} is
   * <strong>not</strong> treated as type).
   */
  boolean isPrimitive(T type);

  /**
   * True, if the type is a pseudo-type ({@code void}, error).
   */
  default boolean isNoType(T type) {
    return !isReference(type) && !isPrimitive(type);
  }

  /**
   * True, if the type is an enum type.
   */
  boolean isEnumType(T type);

  /**
   * Returns a type or a ({@link #isNoType(Object) NoType} if not found.
   */
  T typeOf(ClassRef ref);

  /**
   * Returns a Celement for the given type.
   */
  CElement<S, T> classElement(T type);

  /**
   * Returns the super types of the given types.
   */
  Seq<? extends T> superTypes(T type);

  /**
   * Returns all methods declared in the given type. This does not include
   * inherited methods, but it does include overridden methods.
   */
  Seq<CElement<S, T>> declaredMethods(T type);

  /**
   * Returns the package name of the given element.
   */
  String packageOf(CElement<S, T> element);

  /**
   * Returns the type of an {@code Iterable} with the given component type.
   */
  T iterableOf(T componentType);

  /**
   * Returns the type of a {@code Collection} with the given component type.
   */
  T collectionOf(T componentType);

  /**
   * Returns the type of a {@code List} with the given component type.
   */
  T listOf(T componentType);

  /**
   * Returns the component type of a given {@code Iterable} or some
   * <em>NoType</em>, if the given type is not an {@code Iterable}.
   */
  T componentTypeOfIterable(T iterableType);

}
