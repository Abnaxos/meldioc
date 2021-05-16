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

package c.parameters.good;

import c.parameters.MyEnum;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigMemorySize;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.List;

@Feature
public abstract class AllParamTypesFeature {

  @Parameter
  abstract int intParam();

  @Parameter
  abstract Integer boxedIntParam();

  @Parameter
  abstract long longParam();

  @Parameter
  abstract Long boxedLongParam();

  @Parameter
  abstract double doubleParam();

  @Parameter
  abstract Double boxedDoubleParam();

  @Parameter
  abstract boolean boolParam();

  @Parameter
  abstract Boolean boxedBoolParam();

  @Parameter
  abstract Number numberParam();

  @Parameter
  abstract String stringParam();

  @Parameter
  abstract Duration durationParam();

  @Parameter
  abstract Period periodParam();

  @Parameter
  abstract TemporalAmount temporalParam();

  @Parameter
  abstract Config configParam();

  @Parameter
  abstract ConfigMemorySize memSizeParam();

  @Parameter
  abstract MyEnum enumParam();

  @Parameter
  abstract List<Integer> intListParam();

  @Parameter
  abstract List<Long> longListParam();

  @Parameter
  abstract List<Double> doubleListParam();

  @Parameter
  abstract List<Boolean> boolListParam();

  @Parameter
  abstract List<Number> numberListParam();

  @Parameter
  abstract List<String> stringListParam();

  @Parameter
  abstract List<Duration> durationListParam();

  @Parameter
  abstract List<ConfigMemorySize> memSizeListParam();

  @Parameter
  abstract List<MyEnum> enumListParam();

  @Parameter
  abstract Iterable<Integer> intIterableParam();

  @Parameter
  abstract Iterable<Long> longIterableParam();

  @Parameter
  abstract Iterable<Double> doubleIterableParam();

  @Parameter
  abstract Iterable<Boolean> boolIterableParam();

  @Parameter
  abstract Iterable<Number> numberIterableParam();

  @Parameter
  abstract Iterable<String> stringIterableParam();

  @Parameter
  abstract Iterable<Duration> durationIterableParam();

  @Parameter
  abstract Iterable<ConfigMemorySize> memSizeIterableParam();

  @Parameter
  abstract Iterable<MyEnum> enumIterableParam();
}
