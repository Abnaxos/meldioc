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
 * <p>Provisions are components that implement the functionality of the
 * features (Spring beans, Guice bindings, "services"). By default,
 * provisions are unscoped, i.e. a new instance will be created every time
 * the provision is requested. By declaring
 * {@code @Provision(singleton=true)}, a single, lazily initialized instance
 * will be returned every time the provision is requested. Provision methods
 * cannot be final, as they will be extended in Configurations. They may be
 * abstract, in which case they'll be forwarded to an implementation found
 * in the Configuration.</p>
 *
 * <p>According to the Java specification, annotations on methods are not
 * inherited. To comply to the constraints set by Java, the annotation
 * must be repeated on all overriding classes. The annotation processor
 * will report an error otherwise. Additionally, if a provision is
 * declared as singleton, this declaration must be repeated in overriding
 * methods, because otherwise, it would fall back to the default, i.e.
 * false. The annotation processor checks that a provision isn't
 * accidentally "downgraded" from singleton to unscoped, it's an error to
 * override a singleton provision with {@code @Provision(singleton=false)}
 * or just {@code @Provision}. It is possible to "downgrade" a singleton
 * provision to unscoped by additionally specifying the override attribute:
 * {@code @Provision(singleton=false, override=true)}.</p>
 */
@Documented
@Target(METHOD)
@Retention(CLASS)
public @interface Provision {
  boolean singleton() default false;
  boolean override() default false;
}
