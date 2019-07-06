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

package ch.raffael.compose.processor;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Wraps a {@code TypeMirror} using {@code Types::sameType()} in {@code equals()}.
 */
public final class TypeRef {

  private final Types types;
  private final TypeMirror mirror;

  public TypeRef(Types types, TypeMirror mirror) {
    this.types = types;
    this.mirror = mirror;
  }

  public TypeMirror mirror() {
    return mirror;
  }

  @Override
  public String toString() {
    return "TypeRef[" + mirror.toString() + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    //noinspection ObjectEquality
    if (o == null || getClass() != o.getClass()) return false;
    TypeRef that = (TypeRef) o;
    return types.equals(that.types) && types.isSameType(mirror, that.mirror);
  }

  @Override
  public int hashCode() {
    // TODO (2019-07-06) find a better hash code -- this should work OK, though
    // note that not including `types` in the hash code is not an issue,
    // as we will never work with TypeRefs backed by different `Types`
    return mirror.toString().hashCode();
  }
}
