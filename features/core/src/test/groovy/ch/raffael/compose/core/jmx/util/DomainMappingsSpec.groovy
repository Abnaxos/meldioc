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

package ch.raffael.compose.core.jmx.util

import ch.raffael.compose.core.jmx.util.DomainMappings
import spock.lang.Specification


class DomainMappingsSpec extends Specification {

  def "DomainMapper returns the mapping for the longest matching domain or the default"() {
    given: "A domain mapping"
    def mapping = DomainMappings.of('default')
        .addMapping('a', 'a')
        .addMapping('a.b', 'b')
        .addMapping('a.c', 'c')
    when:
    def domain = mapping.domainFor(qualifiedName)

    then:
    domain == expectedDomain

    where:
    qualifiedName | expectedDomain
    'foo'         | 'default'
    'a'           | 'a'
    'a.b'         | 'b'
    'a.b.c'       | 'b'
  }

}
