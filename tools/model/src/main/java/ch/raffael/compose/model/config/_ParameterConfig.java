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

import ch.raffael.compose.Parameter;
import ch.raffael.compose.model.CElement;
import ch.raffael.compose.util.Strings;
import ch.raffael.compose.util.immutables.Immutable;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import static io.vavr.API.*;
import static java.util.function.Function.identity;

@Immutable.Public
abstract class _ParameterConfig<S> extends ElementConfig<S> {

  private static final Option<String> ALL = Some(Parameter.ALL);

  public static final ModelAnnotationType TYPE = ModelAnnotationType.of(Parameter.class);
  public static final String VALUE = "value";
  public static final String ABSOLUTE = "absolute";

  public static ParameterConfig<Parameter> of(Parameter annotation) {
    return ParameterConfig.<Parameter>builder()
        .source(annotation)
        .value(Option.when(!annotation.value().isEmpty(), annotation.value()))
        .absolute(annotation.absolute())
        .build();
  }
  public abstract Option<String> value();

  public abstract boolean absolute();

  @Override
  public final ModelAnnotationType type() {
    return TYPE;
  }

  @Override
  public Map<String, Object> valueMap() {
    return Map(
        VALUE, value(),
        ABSOLUTE, absolute());
  }

  @Override
  public String displayName() {
    return type().displayName() + value().map(p -> "(" + VALUE + "=\"" + p + "\")").getOrElse("");
  }

  public String fullPath(CElement<?, ?> element) {
    if (value().equals(ALL)) {
      return Parameter.ALL;
    }
    String name = value().getOrElse(
        () -> Strings.camelCaseWords(element.name()).map(String::toLowerCase).mkString("-"));
    if (absolute()) {
      return name;
    }
    Option<CElement<?, ?>> enclosing = Some(element);
    while (enclosing.isDefined()) {
      if (enclosing.get().kind() == CElement.Kind.CLASS) {
        break;
      }
      enclosing = enclosing.flatMap(CElement::parentOption);
    }
    return enclosing.map(e -> e.parameterPrefixConfigOption().map(p -> p.value() + "." + name))
        .flatMap(identity()).getOrElse(name);
  }
}
