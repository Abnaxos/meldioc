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

package ch.raffael.compose.tooling.model;

import ch.raffael.compose.Provision;
import ch.raffael.compose.util.immutables.Immutable;

import java.util.Optional;

/**
 * @since 2019-03-25
 */
@Immutable.Public
abstract class _ProvisionConfig<S> extends AbstractProvisionConfig<S> {

  private static final ModelAnnotationType TYPE = ModelAnnotationType.of(Provision.class);

  public static ProvisionConfig<Provision> of(Provision annotation) {
    return ProvisionConfig.<Provision>builder()
        .source(annotation)
        .shared(annotation.shared())
        .override(annotation.override())
        .build();
  }

  public abstract boolean shared();
  public abstract Optional<Boolean> override();

  public String provisionMethodName() {
    return shared() ? SHARED_METHOD_NAME : DIRECT_METHOD_NAME;
  }

  @Override
  public final ModelAnnotationType type() {
    return TYPE;
  }

}
