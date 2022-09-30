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

package ch.raffael.meldioc.model;

import ch.raffael.meldioc.util.immutables.Immutable;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * A reference to a configuration parameter.
 *
 * <p>TODO (2019-04-19) rename to ConfigParamRef
 */
@Immutable.Pure
public abstract class ConfigRef<T> implements ConfigRef_With<T> {
  ConfigRef() {}
  public static <T> Builder<T> builder() {return new Builder<>();}
  public static <T> ConfigRef<T> of(T type, String configMethodName) {
    return ConfigRef_Immutable.of(type, configMethodName);
  }

  @Value.Parameter
  public abstract T type();

  @Value.Parameter
  public abstract String configMethodName();

  @Value.Default
  @Nullable // immutables runs into an NPE if using `Option<T>`
  public T targetTypeArgument() {
    return null;
  }

  public static final class Builder<T> extends ConfigRef_Immutable.Builder<T> {
    Builder() {}
  }
}
