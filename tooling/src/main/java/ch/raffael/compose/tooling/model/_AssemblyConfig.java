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

import ch.raffael.compose.Assembly;
import ch.raffael.compose.util.immutables.Immutable;

import java.util.Optional;

/**
 * @since 2019-03-25
 */
@Immutable.Public
abstract class _AssemblyConfig<S> extends ModelElementConfig<S> {

  private static final ModelAnnotationType TYPE = ModelAnnotationType.of(Assembly.class);

  public static AssemblyConfig<Assembly> of(Assembly annotation) {
    return AssemblyConfig.<Assembly>builder()
        .source(annotation)
        .shellName(annotation.shellName())
        .packageLocal(annotation.packageLocal())
        .build();
  }

  public abstract String shellName();
  public abstract boolean packageLocal();
  public abstract Optional<String> parent();

  public ClassRef shellClassRef(String packageName, String simpleName) {
    var targetName = shellName().replace("*", simpleName);
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
}
