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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * <p>Extension points allow other features to further configure this feature
 * during {@link Setup setup} (contribute to the extension point). For example,
 * a {@code PaymentFeature} could provide an extension point {@code
 * PaymentMethods} that allows other features to contribute payment methods:</p>
 *
 * <pre>
 *   {@literal @Feature}
 *   class CreditCardPaymentFeature {
 *
 *     {@literal @Provision}
 *     public CreditCardPaymentMethod creditCardPaymentMethod() {
 *       return new CreditCardPaymentMethod();
 *     }
 *
 *     {@literal @Setup}
 *     protected void setupCreditCardPayment(PaymentMethods paymentMethods) {
 *       paymentMethods.add("Credit Card", this::creditCardPaymentMethod);
 *     }
 *   }</pre>
 *
 * <p>Preferably, extension point classes are designed for an expressive,
 * DSL-like syntax, e.g. using fluent builder APIs.</p>
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
 * @see Setup
 * @see Configuration
 */
@Target({METHOD, TYPE})
@Retention(CLASS)
public @interface ExtensionPoint {
}
