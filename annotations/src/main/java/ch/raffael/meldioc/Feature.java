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

package ch.raffael.meldioc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * <p>The main building block of Meld, represents a functionality or group
 * thereof (module).</p>
 *
 * <p>Features consist of:
 * <ul>
 * <li> Provisions, the components that implement the functionality
 * <li> Configuration Parameters
 * <li> ExtensionPoints, that allow other features configure this feature further
 * <li> Setup methods (which often contribute to ExtensionPoints)
 * </ul>
 * </p>
 *
 * <p>Many features are split into a feature interface that only declares a set
 * of provisions and one or more implementation(s) of that feature
 * interface.</p>
 *
 * <p>Features are put together to a {@link Configuration}.</p>
 */
@Documented
@Target(TYPE)
@Retention(CLASS)
public @interface Feature {

  /**
   * Mount a feature into a configuration. Mount methods must be abstract.
   *
   * @see Configuration
   */
  @Documented
  @Target(METHOD)
  @Retention(CLASS)
  @interface Mount {
    /**
     * Instead of generating an implementation of the mount, add a method to
     * the builder to inject an implementation. Use with caution.
     */
    boolean injected() default false;
  }

  /**
   * <p>Mark an interface as dependency (as opposed to the feature implementing it).
   * This currently has no effect, it's just there for clarification:</p>
   *
   * <pre>
   *  {@literal @Feature}
   *  class MyFeature implements @DependsOn AnotherFeature {
   *    // ...
   *  }</pre>
   *
   * <p>&rarr; This feature does <em>not</em> implement {@code AnotherFeature}, but
   * depends on it.</p>
   */
  @Documented
  @Target(TYPE_USE)
  @Retention(CLASS)
  @interface DependsOn {
  }

  /**
   * Mark a superclass or implemented interface as imported into the feature.
   * Usually, extending or implementing non-feature types is an error. This
   * annotation allows to declare that you know what you're doing. Use with
   * caution.
   */
  @Documented
  @Target(TYPE_USE)
  @Retention(CLASS)
  @interface Import {
  }
}
