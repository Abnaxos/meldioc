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

package c.parameters.good;

import c.parameters.MyEnum;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigMemorySize;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Feature
public class AllParamTypesWithDefaultsFeature extends AllParamTypesFeature {

  final Map<String, Integer> calls = new LinkedHashMap<>();

  @Parameter
  @Override
  int intParam() {
    registerCall("intParam");
    return 42;
  }

  @Parameter
  @Override
  Integer boxedIntParam() {
    registerCall("boxedIntParam");
    return 42;
  }

  @Parameter
  @Override
  long longParam() {
    registerCall("longParam");
    return 42L;
  }

  @Parameter
  @Override
  Long boxedLongParam() {
    registerCall("boxedLongParam");
    return 42L;
  }

  @Parameter
  @Override
  double doubleParam() {
    registerCall("doubleParam");
    return 42.0;
  }

  @Parameter
  @Override
  Double boxedDoubleParam() {
    registerCall("boxedDoubleParam");
    return 42.0;
  }

  @Parameter
  @Override
  boolean boolParam() {
    registerCall("boolParam");
    return false;
  }

  @Parameter
  @Override
  Boolean boxedBoolParam() {
    registerCall("boxedBoolParam");
    return false;
  }

  @Parameter
  @Override
  Number numberParam() {
    registerCall("numberParam");
    return 42;
  }

  @Parameter
  @Override
  String stringParam() {
    registerCall("stringParam");
    return "foo";
  }

  @Parameter
  @Override
  Duration durationParam() {
    registerCall("durationParam");
    return Duration.ofSeconds(42);
  }

  @Parameter
  @Override
  Period periodParam() {
    registerCall("periodParam");
    return Period.ofDays(42);
  }

  @Parameter
  @Override
  TemporalAmount temporalParam() {
    registerCall("temporalParam");
    return Duration.ofSeconds(42);
  }

  @Parameter
  @Override
  Config configParam() {
    registerCall("configParam");
    return ConfigFactory.parseMap(Map.of("sub", "foo"));
  }

  @Parameter
  @Override
  ConfigMemorySize memSizeParam() {
    registerCall("memSizeParam");
    return ConfigMemorySize.ofBytes(42);
  }

  @Parameter
  @Override
  MyEnum enumParam() {
    registerCall("enumParam");
    return MyEnum.FOO;
  }

  @Parameter
  @Override
  List<Integer> intListParam() {
    registerCall("intListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<Long> longListParam() {
    registerCall("longListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<Double> doubleListParam() {
    registerCall("doubleListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<Boolean> boolListParam() {
    registerCall("boolListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<Number> numberListParam() {
    registerCall("numberListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<String> stringListParam() {
    registerCall("stringListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<Duration> durationListParam() {
    registerCall("durationListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<ConfigMemorySize> memSizeListParam() {
    registerCall("memSizeListParam");
    return List.of();
  }

  @Parameter
  @Override
  List<MyEnum> enumListParam() {
    registerCall("enumListParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<Integer> intIterableParam() {
    registerCall("intIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<Long> longIterableParam() {
    registerCall("longIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<Double> doubleIterableParam() {
    registerCall("doubleIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<Boolean> boolIterableParam() {
    registerCall("boolIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<Number> numberIterableParam() {
    registerCall("numberIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<String> stringIterableParam() {
    registerCall("stringIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<Duration> durationIterableParam() {
    registerCall("durationIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<ConfigMemorySize> memSizeIterableParam() {
    registerCall("memSizeIterableParam");
    return List.of();
  }

  @Parameter
  @Override
  Iterable<MyEnum> enumIterableParam() {
    registerCall("enumIterableParam");
    return List.of();
  }

  private void registerCall(String call) {
    calls.compute(call, (k, v) -> v == null ? 1 : v + 1);
  }
}
