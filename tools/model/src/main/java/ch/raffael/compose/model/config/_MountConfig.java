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

package ch.raffael.compose.model.config;

import ch.raffael.compose.Module;
import ch.raffael.compose.util.immutables.Immutable;

@Immutable.Public
abstract class _MountConfig<S> extends ElementConfig<S> {

  public static final ModelAnnotationType TYPE = ModelAnnotationType.of(Module.Mount.class);
  public static final String INJECTED = "injected";

  public static MountConfig<Module.Mount> of(Module.Mount annotation) {
    return MountConfig.<Module.Mount>builder()
        .source(annotation)
        .injected(annotation.injected())
        .build();
  }

  @Override
  public final ModelAnnotationType type() {
    return TYPE;
  }

  public abstract boolean injected();

}
