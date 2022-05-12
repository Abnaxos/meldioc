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

package ch.raffael.meldioc.processor.test

import ch.raffael.meldioc.Parameter
import ch.raffael.meldioc.model.messages.Message
import ch.raffael.meldioc.processor.test.tools.ProcessorTestCase
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.objectweb.asm.*
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile
import static ch.raffael.meldioc.processor.test.tools.TestCaseOptions.options

@Stepwise
class ParametersSpec extends Specification {

  @Shared
  ProcessorTestCase good
  @Shared
  Config config = ConfigFactory.load('ch/raffael/meldioc/processor/test/all-parameters').resolve()
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
    good.loadClass('c.parameters.MyEnum')
  }

  def "Compilation succeeds and test-internal meta data is fine"() {
    when:
    good = compile("c/parameters/good")

    then: "Compilation succeeds"
    good.allGood
    and: "All parameter methods have an entry in MAPPINGS"
    def paramMethods = readParameterMethods(good.loadClass('c.parameters.good.AllParamTypesFeature'))
    mappings.keySet().sort() == paramMethods.sort()
    and: "All parameters are defined in the config file"
    mappings.values().findAll {config.hasPath(it[0] as String)}.sort() == mappings.values().sort()
  }

  @Unroll
  def "All parameters will be read from the configuration, both with and without defaults (#method)"() {
    when:
    def shell = good.shellBuilder("NoDefaultsContext").config(config).build()
    def val = shell.mountAllParamTypes()."$method"()

    then:
    val == mapping[1].call(config, mapping[0])

    when:
    shell = good.shellBuilder("WithDefaultsContext").config(config).build()
    val = shell.mountAllParamTypes()."$method"()

    then:
    val == mapping[1].call(config, mapping[0])

    where:
    method << mappings.entrySet()*.key
    mapping << mappings.entrySet()*.value
  }

  @Unroll
  def "Defaults will be used for configuration parameters if specified (#method)"() {
    given:
    def empty = Mock(Config) {
      isResolved() >> true
    }
    empty.resolve() >> empty
    def shell = good.shellBuilder("WithDefaultsContext").config(empty).build()

    when:
    shell.mountAllParamTypes()."$method"()

    then:
    1 * empty.hasPath(mapping[0]) >> false
    0 * empty._

    where:
    method << mappings.entrySet()*.key
    mapping << mappings.entrySet()*.value
  }

  def "@Parameter(absolute = true) resolves parameters without the prefix"() {
    given:
    def ctx = good.shellBuilder('AbsoluteAndRelativeContext').config(config).build()

    expect:
    ctx.myParam() == 'relative-param'
    and:
    ctx.absolute() == 'absolute-param'
    and:
    ctx.absoluteImplicitName() == 'absolute-implicit-name-param'
  }

  def "Various errors"() {
    when:
    def c = compile('c/parameters/bad')

    then:
    c.findMessage {it.id == Message.Id.ConfigTypeNotSupported}.pos == c.marker('object-list')
    c.findMessage {it.id == Message.Id.ConfigTypeNotSupported}.pos == c.marker('object')
    with(c.findMessage {it.id == Message.Id.ConflictingOverride}) {
      def m = c.marker('override')
      pos == m
      c.findMessage {it.pos == m}.id == Message.Id.AbstractMethodWillNotBeImplemented
    }
    c.allGood
  }

  def "When Typesafe Config isn't in the classpath, it's an error to use @Parameter"() {
    when:
    def c = compile('c/parameters/noConfig/bad', options().noTypesafeConfig())

    then:
    with(c.message()) {
      id == Message.Id.TypesafeConfigNotOnClasspath
      pos == c.marker('no-config-mount')
    }
    with(c.message()) {
      id == Message.Id.TypesafeConfigNotOnClasspath
      pos == c.marker('no-config-provision')
    }
    and:
    c.allGood
  }

  def "When Typesafe Config isn't in the classpath, configurations that don't use any parameters are fine"() {
    when:
    def c = compile('c/parameters/noConfig/good', options().noTypesafeConfig())

    then:
    c.allGood
  }

  private List<String> readParameterMethods(Class<?> cls) {
    def bytecode = cls.getClassLoader().getResource(cls.name.replace('.', '/') + '.class').getBytes()
    List<String> methodNames = []
    new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM8) {
      @Override
      MethodVisitor visitMethod(int access, String methodName,
                                String descriptor, String signature, String[] exceptions) {
        //noinspection GroovyUnusedAssignment
        def mv = super.visitMethod(access, methodName, descriptor, signature, exceptions)
        return new MethodVisitor(Opcodes.ASM8, mv) {
          @Override
          AnnotationVisitor visitAnnotation(String d, boolean visible) {
            if (Type.getType(d).className == Parameter.class.getName()) {
              methodNames.add methodName
            }
            return super.visitAnnotation(descriptor, visible)
          }
        }
      }
    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
    return methodNames
  }
}
