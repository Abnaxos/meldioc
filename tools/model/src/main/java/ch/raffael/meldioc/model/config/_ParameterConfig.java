/*
 *  Copyright (c) 2021 Raffael Herzog
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

import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.model.SrcElement;
import ch.raffael.meldioc.util.Strings;
import ch.raffael.meldioc.util.immutables.Immutable;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import static io.vavr.control.Option.some;
import static java.util.function.Function.identity;

@Immutable.Public
abstract class _ParameterConfig<S> extends ElementConfig<S> {

  public static final ModelAnnotationType TYPE = ModelAnnotationType.of(Parameter.class);
  public static final String VALUE = "value";
  public static final String ABSOLUTE = "absolute";

  public static ParameterConfig<Parameter> of(Parameter annotation) {
    return ParameterConfig.<Parameter>builder()
        .source(annotation)
        .value(annotation.value())
        .absolute(annotation.absolute())
        .build();
  }
  public abstract String value();

  public abstract boolean absolute();

  @Override
  public final ModelAnnotationType type() {
    return TYPE;
  }

  @Override
  public Map<String, Object> valueMap() {
    return HashMap.of(VALUE, value(), ABSOLUTE, absolute());
  }

  @Override
  public String displayName() {
    return type().displayName() + (value().isEmpty() ? "" : "(" + VALUE + "=\"" + value() + "\")");
  }

  public Option<String> path() {
    return Option.when(!value().isEmpty(), this::value);
  }

  public String fullPath(SrcElement<?, ?> element) {
    if (value().equals(Parameter.ALL)) {
      return Parameter.ALL;
    }
    String name = path().getOrElse(
        () -> Strings.camelCaseWords(element.name()).map(String::toLowerCase).mkString("-"));
    if (absolute()) {
      return name;
    }
    Option<SrcElement<?, ?>> enclosing = some(element);
    while (enclosing.isDefined()) {
      if (enclosing.get().kind() == SrcElement.Kind.CLASS) {
        break;
      }
      enclosing = enclosing.flatMap(SrcElement::parentOption);
    }
    return enclosing.map(e -> e.parameterPrefixConfigOption().map(p -> p.value() + "." + name))
        .flatMap(identity()).getOrElse(name);
  }
}
