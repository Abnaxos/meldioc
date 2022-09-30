/*
 *  Copyright (c) 2022 Raffael Herzog
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

package ch.raffael.meldioc.util.immutables;

import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Common styles for immutables.
 */
@Target({})
@SuppressWarnings("unused")
public @interface Immutable {

  @Deprecated(forRemoval = true)
  String TYPE_IMMUTABLE = "*";
  @Deprecated(forRemoval = true)
  String TYPE_IMMUTABLE_ENCLOSING = "*";
  @Deprecated(forRemoval = true)
  String TYPE_ABSTRACT = "_*";

  /**
   * Style for a "pure immutable" using sandwich pattern. Usage:
   *
   * <pre>{@literal @Immutable.Pure}
   * public abstract class MyRecord implements MyRecord_With {
   *   MyRecord() {}
   *   public static Builder builder() {return new Builder();}
   *
   *   // declare your fields here
   *
   *   public static final class Builder extends MyRecord_Immutable {
   *     Builder() {}
   *   }
   * }</pre>
   */
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeImmutable = "*_Immutable",
      typeWith = "*_With",
      typeImmutableEnclosing = "*_Immutables",
      visibility = Value.Style.ImplementationVisibility.PACKAGE,
      builderVisibility = Value.Style.BuilderVisibility.PACKAGE,
      overshadowImplementation = true,
      throwForInvalidImmutableState = IllegalBuilderStateException.class,
      passAnnotations = SuppressWarnings.class)
  @VavrEncodingEnabled
  @interface Pure {
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @Value.Style(
        typeImmutable = "*_Immutable",
        typeWith = "*_With",
        typeImmutableEnclosing = "*_Immutables",
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        builderVisibility = Value.Style.BuilderVisibility.PACKAGE,
        overshadowImplementation = true,
        throwForInvalidImmutableState = IllegalBuilderStateException.class,
        passAnnotations = SuppressWarnings.class)
    @VavrEncodingEnabled
    @interface Style {}
  }


  /**
   * @deprecated Use {@link Pure} and the sandwich pattern.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeAbstract = TYPE_ABSTRACT,
      typeImmutable = TYPE_IMMUTABLE,
      typeImmutableEnclosing = TYPE_IMMUTABLE_ENCLOSING,
      visibility = Value.Style.ImplementationVisibility.PUBLIC,
      builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
      throwForInvalidImmutableState = IllegalBuilderStateException.class,
      passAnnotations = SuppressWarnings.class)
  @VavrEncodingEnabled
  @Deprecated(forRemoval = true)
  @interface Public {}

  /**
   * @deprecated Use {@link Pure} and the sandwich pattern.
   */
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
  @Deprecated(forRemoval = true)
  @interface PublicStyle {}

  /**
   * @deprecated Use {@link Pure} and the sandwich pattern.
   */
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
  @Deprecated(forRemoval = true)
  @interface Local {}

  /**
   * @deprecated Use {@link Pure} and the sandwich pattern.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.TYPE)
  @Value.Style(
      typeImmutable = TYPE_IMMUTABLE,
      typeImmutableEnclosing = TYPE_IMMUTABLE_ENCLOSING,
      visibility = Value.Style.ImplementationVisibility.PACKAGE,
      builderVisibility = Value.Style.BuilderVisibility.PACKAGE,
      throwForInvalidImmutableState = IllegalBuilderStateException.class)
  @VavrEncodingEnabled
  @Deprecated(forRemoval = true)
  @interface LocalStyle {}
}
