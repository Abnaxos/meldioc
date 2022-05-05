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

package ch.raffael.meldioc.model.config;

import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.util.immutables.PureImmutable;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

@PureImmutable
public abstract class MountConfig<S> extends ElementConfig<S> implements MountConfig_With<S> {

  public static final ModelAnnotationType TYPE = ModelAnnotationType.of(Feature.Mount.class);
  public static final String INJECTED = "injected";

  MountConfig() {
  }

  public static <S> Builder<S> builder() {
    return MountConfig_Immutable.builder();
  }

  public static MountConfig<Feature.Mount> of(Feature.Mount annotation) {
    return MountConfig.<Feature.Mount>builder()
        .source(annotation)
        .injected(annotation.injected())
        .build();
  }

  public abstract boolean injected();

  @Override
  public final ModelAnnotationType type() {
    return TYPE;
  }

  @Override
  public Map<String, Object> valueMap() {
    return HashMap.of(INJECTED, injected());
  }

  public static abstract class Builder<S> extends ElementConfig.Builder<S> {
    Builder() {}
    public abstract Builder<S> from(ElementConfig<S> instance);
    public abstract Builder<S> from(MountConfig<S> instance);
    public abstract Builder<S> source(S source);
    public abstract Builder<S> injected(boolean injected);
    public abstract MountConfig<S> build();
  }
}
