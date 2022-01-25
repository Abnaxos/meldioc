/*
 *  Copyright (c) 2021 Raffael Herzog
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

package c.extendability;

import c.extendability.subpackage.SubpackagePublic;
import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.Feature.Mount;
import ch.raffael.meldioc.processor.test.tools.Marker;

@Configuration
public abstract class ErrContext {

  @Mount
  abstract NestedMount.PublicConstructor nestedPublicConstructor();

  @Mount
  abstract NestedMount.LocalConstructor nestedLocalConstructor();

  @Marker("mount-nested-constructor-not-accessible")
  @Mount
  abstract NestedMount.ConstructorNotAccessible nestedConstructorNotAccessible();

  @Marker("mount-inner")
  @Mount
  abstract InnerMount.ErrInner inner();

  @Mount
  abstract SubpackagePublic.PublicConstructor subpackagePublicConstructor();

  @Marker("mount-subpackage-constructor-not-accessible")
  @Mount
  abstract SubpackagePublic.ErrLocalConstructor subpackageLocalConstructor();

  @Marker("no-default-constructor-feature")
  @Mount
  abstract NoDefaultConstructorFeature noDefaultConstructorFeature();

  @Marker("mount-final-feature")
  @Mount
  abstract FinalFeature finalFeature();

  @Marker("mount-sealed-feature")
  @Mount
  abstract SealedFeature sealedFeature();

  @Mount
  abstract SealedFeature.NonSealed nonSealedFeature();
}
