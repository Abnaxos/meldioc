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

package ch.raffael.compose.processor.test

import ch.raffael.compose.Parameter
import ch.raffael.compose.model.messages.Message
import ch.raffael.compose.processor.test.meta.Fine
import ch.raffael.compose.processor.test.tools.ProcessorTestCase
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import static ch.raffael.compose.processor.test.tools.ProcessorTestCase.compile

@Stepwise
class ParametersSpec extends Specification {

  @Shared
  ProcessorTestCase fine
  @Shared
  Config config = ConfigFactory.load('ch/raffael/compose/processor/test/all-parameters').resolve()
  @Shared
  def mappings = [
      intParam: ['int-param', {Config c, String p -> c.getInt(p)}],
      boxedIntParam: ['boxed-int-param', {Config c, String p -> c.getInt(p)}],
      longParam: ['long-param', {Config c, String p -> c.getLong(p)}],
      boxedLongParam: ['boxed-long-param', {Config c, String p -> c.getLong(p)}],
      doubleParam: ['double-param', {Config c, String p -> c.getDouble(p)}],
      boxedDoubleParam: ['boxed-double-param', {Config c, String p -> c.getDouble(p)}],
      boolParam: ['bool-param', {Config c, String p -> c.getBoolean(p)}],
      boxedBoolParam: ['boxed-bool-param', {Config c, String p -> c.getBoolean(p)}],
      numberParam: ['number-param', {Config c, String p -> c.getNumber(p)}],
      stringParam: ['string-param', {Config c, String p -> c.getString(p)}],
      durationParam: ['duration-param', {Config c, String p -> c.getDuration(p)}],
      periodParam: ['period-param', {Config c, String p -> c.getPeriod(p)}],
      temporalParam: ['temporal-param', {Config c, String p -> c.getTemporal(p)}],
      configParam: ['config-param', {Config c, String p -> c.getConfig(p)}],
      memSizeParam: ['mem-size-param', {Config c, String p -> c.getMemorySize(p)}],
      enumParam: ['enum-param', {Config c, String p -> c.getEnum(myEnumClass(), p)}],
      intListParam: ['int-list-param', {Config c, String p -> c.getIntList(p)}],
      longListParam: ['long-list-param', {Config c, String p -> c.getLongList(p)}],
      doubleListParam: ['double-list-param', {Config c, String p -> c.getDoubleList(p)}],
      boolListParam: ['bool-list-param', {Config c, String p -> c.getBooleanList(p)}],
      numberListParam: ['number-list-param', {Config c, String p -> c.getNumberList(p)}],
      stringListParam: ['string-list-param', {Config c, String p -> c.getStringList(p)}],
      durationListParam: ['duration-list-param', {Config c, String p -> c.getDurationList(p)}],
      memSizeListParam: ['mem-size-list-param', {Config c, String p -> c.getMemorySizeList(p)}],
      enumListParam: ['enum-list-param', {Config c, String p -> c.getEnumList(myEnumClass(), p)}],
      intIterableParam: ['int-iterable-param', {Config c, String p -> c.getIntList(p)}],
      longIterableParam: ['long-iterable-param', {Config c, String p -> c.getLongList(p)}],
      doubleIterableParam: ['double-iterable-param', {Config c, String p -> c.getDoubleList(p)}],
      boolIterableParam: ['bool-iterable-param', {Config c, String p -> c.getBooleanList(p)}],
      numberIterableParam: ['number-iterable-param', {Config c, String p -> c.getNumberList(p)}],
      stringIterableParam: ['string-iterable-param', {Config c, String p -> c.getStringList(p)}],
      durationIterableParam: ['duration-iterable-param', {Config c, String p -> c.getDurationList(p)}],
      memSizeIterableParam: ['mem-size-iterable-param', {Config c, String p -> c.getMemorySizeList(p)}],
      enumIterableParam: ['enum-iterable-param', {Config c, String p -> c.getEnumList(myEnumClass(), p)}],
  ]

  @SuppressWarnings("GroovyAssignabilityCheck")
  private Class<? extends Enum> myEnumClass() {
    fine.loadClass('c.parameters.MyEnum')
  }

  @Fine
  def "Compilation succeeds and test-internal meta data is fine"() {
    when:
    fine = compile("c/parameters/fine")

    then: "Compilation succeeds"
    fine.allFine
    and: "All parameter methods have an entry in MAPPINGS"
    def declared = fine.loadClass('c.parameters.fine.AllParamTypesFeature').
        declaredMethods.findAll {it.getAnnotation(Parameter.class)}.collect {it.name} as Set
    mappings.keySet().sort() == declared.sort()
    and: "All parameters are defined in the config file"
    mappings.values().findAll {config.hasPath(it[0] as String)}.sort() == mappings.values().sort()
  }

  @Fine
  @Unroll
  def "All parameters will be read from the configuration, both with and without defaults (#method)"() {
    when:
    def shell = fine.shellBuilder("NoDefaultsContext").config(config).build()
    def val = shell.mountAllParamTypes()."$method"()

    then:
    val == mapping[1].call(config, mapping[0])

    when:
    shell = fine.shellBuilder("WithDefaultsContext").config(config).build()
    val = shell.mountAllParamTypes()."$method"()

    then:
    val == mapping[1].call(config, mapping[0])

    where:
    method << mappings.entrySet()*.key
    mapping << mappings.entrySet()*.value
  }

  @Fine
  @Unroll
  def "Defaults will be used for configuration parameters if specified (#method)"() {
    given:
    def empty = Mock(Config) {
      isResolved() >> true
    }
    def shell = fine.shellBuilder("WithDefaultsContext").config(empty).build()

    when:
    shell.mountAllParamTypes()."$method"()

    then:
    1 * empty.hasPath(mapping[0]) >> false
    0 * empty._

    where:
    method << mappings.entrySet()*.key
    mapping << mappings.entrySet()*.value
  }

  @Fine
  def "Hardcoded (`Parameter.HARDCODE`) parameters will not refer to the configuration anymore"() {
    given:
    def config = Mock(Config)

    when:
    def v = fine.shellBuilder("HardcodeContext").config(config).build().stringParam()

    then:
    v == 'hardcoded'
    config.isResolved() >> true
    0 * config._
  }

  def "Various errors"() {
    when:
    def c = compile('c/parameters/flawed')

    then:
    c.findMessage {it.id == Message.Id.ConfigTypeNotSupported}.pos == c.marker('object-list')
    c.findMessage {it.id == Message.Id.ConfigTypeNotSupported}.pos == c.marker('object')
    with(c.findMessage {it.id == Message.Id.ConflictingOverride}) {
      def m = c.marker('override')
      pos == m
      c.findMessage {it.pos == m}.id == Message.Id.AbstractMethodWillNotBeImplemented
    }
    c.findMessage {it.id == Message.Id.AbstractMethodWillNotBeImplemented}.message.contains(' hardcode() ')
    c.allFine
  }
}
