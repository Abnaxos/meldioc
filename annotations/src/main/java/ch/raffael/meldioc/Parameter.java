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
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * <p>Used to retrieve a configuration parameter. Meld uses <a
 * href="https://github.com/lightbend/config">Typesafe Config</a> for all
 * configuration. All types supported by Typesafe Config may be used as
 * return value, including {@code Config} to return a sub-configuration.</p>
 *
 * <p>If the method is abstract, the parameter is required. If it has an
 * implementation, it will be used to retrieve a default value.</p>
 *
 * <p>If no parameter name is specified, the method name converted to kebab
 * case will be used, e.g.:</p>
 *
 * <pre>
 *  {@literal @Parameter}
 *  abstract int myParameter();</pre>
 *
 * <p>is equivalent to</p>
 *
 * <pre>
 *  {@literal @Parameter("my-parameter")}
 *  abstract int myParameter();</pre>
 *
 * <p>The special parameter name '*' refers to the whole configuration
 * tree.</p>
 *
 * <p>If a {@link Parameter.Prefix prefix} is specified on the feature
 * class, the parameter name will be relative to that prefix, unless
 * {@code absolute=true} is specified.</p>
 */
@Target(METHOD)
@Retention(CLASS)
@Documented
@SuppressWarnings("NullabilityAnnotations")
public @interface Parameter {

  String ALL = "*";

  /**
   * The key in the configuration to use, defaults to the method name
   * converted to kebab case.
   */
  String value() default "";

  /**
   * Ignore the specified {@link Prefix}
   */
  boolean absolute() default false;

  /**
   * Specify a prefix for all parameters in this class.
   */
  @Target(TYPE)
  @Retention(CLASS)
  @Documented
  @interface Prefix {
    String value();
  }
}
