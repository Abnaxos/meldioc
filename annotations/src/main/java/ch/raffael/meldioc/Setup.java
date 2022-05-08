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
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * <p>Setup methods are called during construction of the configuration to add
 * further initialisation logic. They shouldn't return a value (if they do,
 * the return value will be discarded). They may take extension points as
 * arguments, which will be resolved from all available extension points of
 * all features in the configuration.</p>
 *
 * <p>During the execution of setup methods, provisions are not available
 * yet, as the corresponding features may not have been initialized yet.
 * Configuration parameters, however, are available in setup methods.</p>
 *
 * <p>Setup methods are not suitable to perform any post-construction
 * actions like application startup. Use a separate {@code start()}
 * method or similar in the configuration instead. The base library
 * provides a small framework for startup/shutdown for more complex
 * cases.</p>
 *
 * <!-- text block: extension-points-setup -->
 * <p><strong>Important:</strong> Extension points must be used
 * <em>exclusively</em> in setup methods. Unfortunately, there's no way for
 * Meld to protect extension points from escaping the setup method (e.g. by
 * assigning them to a field), it's the programmers responsibility to not do
 * this.</p>
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
 *
 * @see ExtensionPoint
 * @see Configuration
 */
@Target(METHOD)
@Retention(CLASS)
@Documented
public @interface Setup {
}
