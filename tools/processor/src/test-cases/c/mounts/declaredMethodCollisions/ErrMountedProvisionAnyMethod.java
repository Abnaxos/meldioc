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

package c.mounts.declaredMethodCollisions;

import c.FeatureA;
import c.FeatureB;
import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.Feature.Import;
import ch.raffael.meldioc.Feature.Mount;
import ch.raffael.meldioc.processor.test.tools.Marker;

// this is how I found the issue
@Configuration
@Marker("override-b-inherited")
public abstract class ErrMountedProvisionAnyMethod extends @Import SuperWithBMethod {

  @Mount
  abstract FeatureA.Singleton featureA();
  @Mount
  abstract FeatureB.NonSingleton featureB();

  @Marker("override-a")
  private void a() {}
}
