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

package ch.raffael.compose.processor.model;

import ch.raffael.compose.Configuration;
import ch.raffael.compose.processor.util.Elements;
import ch.raffael.compose.tooling.model.ConfigurationConfig;
import ch.raffael.compose.util.immutables.Immutable;
import org.immutables.value.Value;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;

/**
 * TODO javadoc
 */
@Immutable.Public
abstract class _ConfigurationMethod extends ModelElement.OfExecutable<ConfigurationConfig<Configuration>> {

  @Override
  @Value.Parameter
  public abstract CompositionTypeModel enclosing();

  @Override
  @Value.Parameter
  public abstract ExecutableElement element();

  @Override
  @Value.Parameter
  public abstract ConfigurationConfig<Configuration> config();

  public String fullPath() {
    // TODO (2019-03-31) WTF? why is this unchecked?
    var path = config().path().orElseGet(() -> element().getSimpleName().toString());
    return config().absolute() ? path :
        enclosing().pool().modelOf((DeclaredType) element().getEnclosingElement().asType())
            .configurationPrefix().map(p -> p + "." + path).orElse(path);
  }

  public boolean hasDefault() {
    return !Elements.isAbstract(element());
  }

}
