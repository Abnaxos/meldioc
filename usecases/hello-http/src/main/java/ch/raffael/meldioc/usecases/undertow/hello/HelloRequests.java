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

package ch.raffael.meldioc.usecases.undertow.hello;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;

import java.time.Instant;

import static ch.raffael.meldioc.logging.Logging.logger;

class HelloRequests {

  private static final Logger LOG = logger();

  private final String greeting;

  HelloRequests(String greeting) {
    this.greeting = greeting;
  }

  String text(Option<String> name) {
    return text(name.getOrElse("whoever you are"));
  }

  String text(String name) {
    return sayHello(greeting, name);
  }

  RestHelloResponse json(RestHelloRequest request) {
    return RestHelloResponse.builder()
        .message(sayHello(request.greeting().orElse(greeting), request.name()))
        .timestamp(Instant.now())
        .build();
  }

  String longText() {
    return "A long text:\n" + Stream.range(0, 1000).map(n -> "#" + n + "\n").mkString();
  }

  private String sayHello(String greeting, String name) {
    String hello = greeting + " " + name + "!";
    LOG.info("Saying '{}'", hello);
    return hello;
  }
}
