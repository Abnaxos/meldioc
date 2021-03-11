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

package ch.raffael.meldioc.library.http.server.undertow

import ch.raffael.meldioc.library.http.server.undertow.testlib.UndertowSpecification
import groovyx.net.http.HttpResponseException

import java.nio.charset.StandardCharsets

class HttpMethodsSpec extends UndertowSpecification {

  def "The endpoints support the respective methods and echo our request"() {
    def res

    when:
    res = get(path: '/echo/get', query: [head: 'foo'])
    then:
    res.status == 200
    res.data.text == 'ECHO: foo; get'

    when:
    res = post(path: '/echo/post', contentType: 'text/plain', body: 'my post')
    then:
    res.status == 200
    res.data.text == 'ECHO: my post'

    when:
    res = put(path: '/echo/put', contentType: 'text/plain', body: 'my put')
    then:
    res.status == 200
    res.data.text == 'ECHO: my put'

    when:
    res = post(path: '/echo/put-post', contentType: 'text/plain', body: 'my post')
    then:
    res.status == 200
    res.data.text == 'ECHO: my post'

    when:
    res = put(path: '/echo/put-post', contentType: 'text/plain', body: 'my put')
    then:
    res.status == 200
    res.data.text == 'ECHO: my put'
  }

  def "A 405 with correct Allow header is returned on wrong HTTP method"() {
    def res

    when:
    try {
      res = post(path: '/echo/get', contentType: 'text/plain', body: 'my post')
    } catch (HttpResponseException e) {
      res = e.response
    }

    then:
    res.status == 405
    res.getFirstHeader('Allow')?.value?.split(',')?.collect {it.strip()} as Set ==
        ['GET', 'HEAD', 'OPTIONS'] as Set

    when:
    try {
      res = get(path: '/echo/put-post')
    } catch (HttpResponseException e) {
      res = e.response
    }

    then:
    res.status == 405
    res.getFirstHeader('Allow')?.value?.split(',')?.collect {it.strip()} as Set ==
        ['PUT', 'POST', 'OPTIONS'] as Set
  }

  def "OPTIONS requests return 204 with the Allow header set to the allowed methods"() {
    def res

    when:
    res = options(path: '/echo/get')

    then:
    res.status == 204
    res.getFirstHeader('Allow')?.value?.split(',')?.collect {it.strip()} as Set ==
        ['GET', 'HEAD', 'OPTIONS'] as Set

    when:
    res = options(path: '/echo/put-post')

    then:
    res.status == 204
    res.getFirstHeader('Allow')?.value?.split(',')?.collect {it.strip()} as Set ==
        ['PUT', 'POST', 'OPTIONS'] as Set
  }

  def "HEAD request does the same as GET, but without body"() {
    // we're doing telnet-style HTTP here to be able to inspect the response closely
    given:
    def socket = new Socket('localhost', undertow.port)
    def out = new BufferedWriter(new OutputStreamWriter(socket.outputStream, StandardCharsets.ISO_8859_1))
    def res = new BufferedReader(new InputStreamReader(socket.inputStream, StandardCharsets.ISO_8859_1))

    when:
    out.write('HEAD /echo/get HTTP/1.1\r\n')
    out.flush()
    out.write('Host: localhost\r\nConnection: close\r\n\r\n')
    out.flush()
    and: "read response"
    println 'Waiting for response ...'
    def lines = res.readLines()
    println 'Response:'
    lines.each {println "> $it"}

    then: "We get an OK response"
    lines[0].startsWith('HTTP/1.1 200 ')
    and: "The headers are set correctly"
    lines.find {it == 'Content-Type: text/plain; charset=UTF-8'} != null
    lines.collect{it =~ /Content-Length: (\d+)/}.find {it.matches()}?.group(1) as int ==
        'ECHO: get'.length()
    and: "The first empty line is also the last line of the request (i.e. no response body)"
    lines.findIndexOf {it.isEmpty()} == lines.size() - 1

    cleanup:
    res?.close()
    out?.close()
    socket?.close()
  }
}
