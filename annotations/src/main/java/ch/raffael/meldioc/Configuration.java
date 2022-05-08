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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * <p>A configuration represents a set of feature implementation that together
 * form a whole (application, EJB, whatever). This is mostly done by
 * mounting ({@link Feature.Mount @Mount}) multiple features.</p>
 *
 * <p>The annotation processor will generate a class MyConfigurationShell that
 * acts as entry point with a nested class Builder. Example code to
 * construct an instance of a configuration:</p>
 *
 * <pre>
 *  {@literal @Configuration}
 *  abstract class MyConfiguration {
 *    // (mounts, provisions, setup methods etc)
 *
 *    static MyConfiguration create(Config cfg) {
 *      return MyConfigurationShell.builder().config(cfg).build();
 *    }
 *  }</pre>
 *
 * <p>A configuration can itself do everything that a feature can do, i.e.
 * declare provisions, configuration parameters, extension points and setup
 * methods.</p>
 *
 * <p><strong>Provisions:</strong> All provisions that are mounted from
 * features are added to the configuration. There can be only one
 * implementation of a provision, otherwise, it's considered a conflict
 * and therefore an error. If no implementation can be found for a
 * declared or mounted feature, it's also an error. The actual type of the
 * provision will be the type declared by the implementation. Abstract
 * provisions with a contravariant type (a super type of the actual type)
 * are fine, covariants (or invariants, obviously) are not.</p>
 *
 * <p><strong>Construction:</strong> During construction, all setup methods
 * of the configuration and all mounted features are called. Arguments of
 * setup methods will be resolved from all declared or mounted extension
 * points. Exactly one extension point must match the declared type. If
 * multiple extension points match, it's considered a conflict and
 * therefore an error, if none match, it's considered unresolved and also
 * an error.</p>
 *
 * <!-- text block: thread-safety -->
 * <p><strong>Thread-safety:</strong> All mounted features and the
 * configuration implementation itself will be assigned to a final field and
 * all setup methods will be called from the constructor. Therefore, all
 * actions in constructors and setup methods <em>happen-before</em> the
 * {@code build()} method exits, as long as no objects escape the object
 * tree under construction
 * (<a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-17.html#jls-17.5">JLS
 * 17.5</a>).</p>
 */
@Documented
@Target(TYPE)
@Retention(CLASS)
@SuppressWarnings("NullabilityAnnotations")
public @interface Configuration {

  /**
   * Additional features to mount.
   */
  Class<?>[] mount() default {};

  /**
   * The name of the shell class.
   */
  String shellName() default "*Shell";

  /**
   * Make the shell package local or public.
   */
  boolean packageLocal() default true;
}
