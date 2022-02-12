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

package ch.raffael.meldioc.library.http.server.undertow

import ch.raffael.meldioc.library.http.server.undertow.testlib.UndertowSpecification
import ch.raffael.meldioc.library.http.server.undertow.util.HttpStatus

class HttpStatusSpec extends UndertowSpecification {

  def "Successful requests set their status codes"() {
    def res

    when: "Request a simple OK status"
    res = http {
      path '/s/ok'
      GET()
    }
    then: "The status code is OK"
    res.statusCode() == HttpStatus.OK.code()
    res.body() == 'ok,ok,ok'

    when: "Request an ACCEPTED status"
    res = http {
      path '/s/accepted'
      GET()
    }
    then: "The status code is ACCEPTED"
    res.statusCode() == HttpStatus.ACCEPTED.code()
    res.body() == 'ok,ok,ok'
  }

  def "Requests throwing non-recoverable exceptions set their status codes"() {
    def res

    when: "A request that throws a non-recoverable exception"
    res = http {
      path '/s/throwNonRecoverable'
    }
    then: "The status code is INTERNAL_SERVER_ERROR"
    res.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR.code()

    when: "A request that throws a non-recoverable exception that sets a custom status NOT_EXTENDED"
    res = http {
      path '/s/throwNonRecoverable,notExtended'
    }
    then: "The status code is NOT_EXTENDED"
    res.statusCode() == HttpStatus.NOT_EXTENDED.code()
  }

  def "Requests throwing recoverable exception set their status codes"() {
    def res

    when: "A request that throws a recoverable exception"
    res = http {
      path '/s/throwRecoverable'
    }
    then: "The status code is OK, the body is the result of the recovery"
    res.statusCode() == HttpStatus.OK.code()
    res.body() == 'recovered'

    when: "A request that that throws a recoverable exception and would normally return ACCEPTED"
    res = http {
      path '/s/throwRecoverable,accepted'
    }
    then: "The status code is ACCEPTED, the body is the result of the recovery"
    res.statusCode() == HttpStatus.ACCEPTED.code()
    res.body() == 'recovered'

    when: "A request that throws a recoverable exception with a custom recovery status code MULTI_STATUS"
    res = http {
      path '/s/throwRecoverable,recoverStatus'
    }
    then: "The status code is MULTI_STATUS, the body is the result of the recovery"
    res.statusCode() == HttpStatus.MULTI_STATUS.code()
    res.body() == 'recovered'

    when: "A request that would normally return ACCEPTED, throws a recoverable exception with a custom recovery status code MULTI_STATUS"
    res = http {
      path '/s/accepted,throwRecoverable,recoverStatus'
    }
    then: "The status code is MULTI_STATUS, the body is the result of the recovery"
    res.statusCode() == HttpStatus.MULTI_STATUS.code()
    res.body() == 'recovered'

    when: "A request that throws a recoverable exception, but recovery fails"
    res = http {
      path '/s/throwRecoverable,failRecovery'
    }
    then: "The status code is INTERNAL_SERVER_ERROR"
    res.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR.code()
  }
}
