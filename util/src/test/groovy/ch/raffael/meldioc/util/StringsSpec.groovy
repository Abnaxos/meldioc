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

package ch.raffael.meldioc.util

import spock.lang.Specification
import spock.lang.Unroll

class StringsSpec extends Specification {

  @Unroll
  def "CamelCase is split correctly, handling special characters as new word separator ('#camelCase')"() {
    when:
    def words = Strings.camelCaseWords(camelCase)

    then:
    words.toJavaList() == expectedWords

    where:
    camelCase         | expectedWords
    'fooBar'          | ['foo', 'Bar']
    'FooBar'          | ['Foo', 'Bar']
    'FOOBar'          | ['FOO', 'Bar']
    'fooBARFoo'       | ['foo', 'BAR', 'Foo']
    'foo_bar_$foo'    | ['foo', 'bar', 'foo']
    'foo_bar_$FOOBar' | ['foo', 'bar', 'FOO', 'Bar']
    'foo42Bar'        | ['foo42', 'Bar']
    '_fooBar'         | ['foo', 'Bar']
    '55foo'           | ['55', 'foo']
    '_55foo'          | ['55', 'foo']
    '_55$foo'         | ['55', 'foo']
    'foo55'           | ['foo55']
    'foo__'           | ['foo']
    '42'              | ['42']
    ''                | []
    '_$_'             | []
  }


}
