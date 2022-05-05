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

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.util.immutables.PureImmutable;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;

@PureImmutable
@SuppressWarnings("varargs") // Bug in immutables or immutables-vavr: the builder methods are not annotated correctly
public abstract class ConfigurationConfig<S> extends ElementConfig<S> implements ConfigurationConfig_With<S> {

  public static final ModelAnnotationType TYPE = ModelAnnotationType.of(Configuration.class);
  public static final String MOUNT = "mount";
  public static final String SHELL_NAME = "shellName";
  public static final String PACKAGE_LOCAL = "packageLocal";

  public static ConfigurationConfig<Configuration> of(Configuration annotation) {
    return ConfigurationConfig_Immutable.<Configuration>builder()
        .source(annotation)
        .shellName(annotation.shellName())
        .packageLocal(annotation.packageLocal())
        .build();
  }

  public abstract Seq<ClassRef> mount();
  public abstract String shellName();
  public abstract boolean packageLocal();

  ConfigurationConfig() {
  }

  public static <S> Builder<S> builder() {
    return ConfigurationConfig_Immutable.builder();
  }

  public ClassRef shellClassRef(String packageName, String simpleName) {
    String targetName = shellName().replace("*", simpleName);
    int pos = simpleName.lastIndexOf('.');
    if (pos >= 0) {
          return ClassRef.of(targetName.substring(0, pos), targetName.substring(pos + 1));
    } else {
      return ClassRef.of(packageName, targetName);
    }
  }

  @Override
  public final ModelAnnotationType type() {
    return TYPE;
  }

  @Override
  public Map<String, Object> valueMap() {
    return HashMap.of(
        SHELL_NAME, shellName(),
        PACKAGE_LOCAL, packageLocal());
  }

  public static abstract class Builder<S> extends ElementConfig.Builder<S> {
    Builder() {}
    @Override
    public abstract Builder<S> from(ElementConfig<S> instance);
    public abstract Builder<S> from(ConfigurationConfig<S> instance);
    @Override
    public abstract Builder<S> source(S source);
    public abstract Builder<S> addMount(ClassRef element);
    public abstract Builder<S> addMount(ClassRef... elements);
    public abstract Builder<S> addAllMount(Iterable<ClassRef> element);
    public abstract Builder<S> mount(Seq<ClassRef> elements);
    public abstract Builder<S> setIterableMount(Iterable<ClassRef> elements);
    public abstract Builder<S> shellName(String shellName);
    public abstract Builder<S> packageLocal(boolean packageLocal);
    public abstract ConfigurationConfig<S> build();
  }
}
