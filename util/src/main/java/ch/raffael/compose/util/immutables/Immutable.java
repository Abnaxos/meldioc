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

package ch.raffael.compose.util.immutables;

import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO javadoc
 */
@Target({})
@SuppressWarnings("unused")
public @interface Immutable {

  String TYPE_IMMUTABLE = "*";
  String TYPE_IMMUTABLE_ENCLOSING = "*";
  String TYPE_ABSTRACT = "_*";

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeAbstract = TYPE_ABSTRACT,
      typeImmutable = TYPE_IMMUTABLE,
      typeImmutableEnclosing = TYPE_IMMUTABLE_ENCLOSING,
      visibility = Value.Style.ImplementationVisibility.PUBLIC,
      builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
      throwForInvalidImmutableState = IllegalBuilderStateException.class)
  @VavrEncodingEnabled
  @interface Public {
  }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeAbstract = TYPE_ABSTRACT,
      typeImmutable = TYPE_IMMUTABLE,
      typeImmutableEnclosing = TYPE_IMMUTABLE_ENCLOSING,
      visibility = Value.Style.ImplementationVisibility.PUBLIC,
      builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
      throwForInvalidImmutableState = IllegalBuilderStateException.class)
  @VavrEncodingEnabled
  @interface PublicStyle {
  }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeAbstract = TYPE_ABSTRACT,
      typeImmutable = TYPE_IMMUTABLE,
      typeImmutableEnclosing = TYPE_IMMUTABLE_ENCLOSING,
      visibility = Value.Style.ImplementationVisibility.PACKAGE,
      builderVisibility = Value.Style.BuilderVisibility.PACKAGE,
      throwForInvalidImmutableState = IllegalBuilderStateException.class)
  @VavrEncodingEnabled
  @interface Local {
  }

  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeAbstract = TYPE_ABSTRACT,
      typeImmutable = TYPE_IMMUTABLE,
      typeImmutableEnclosing = TYPE_IMMUTABLE_ENCLOSING,
      visibility = Value.Style.ImplementationVisibility.PACKAGE,
      builderVisibility = Value.Style.BuilderVisibility.PACKAGE,
      throwForInvalidImmutableState = IllegalBuilderStateException.class)
  @VavrEncodingEnabled
  @interface LocalStyle {
  }

}
