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

package ch.raffael.compose.usecases.http.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 2019-03-23
 */
public class HelloMain {

  private static final Logger LOG = LoggerFactory.getLogger(HelloMain.class);

  public static void main(String[] args) throws Exception {
    var ctx = new AppContextAssemblyManual();
    Runtime.getRuntime().addShutdownHook(new Thread(ctx::shutdown, "Shutdown"));
    ctx.shutdownCoordinator().onFinalize(() -> LOG.info("This is my shutdown hook"));
    ctx.start();
    ctx.httpModule().jettyServer().await();
    LOG.info("Hello application ready");
  }

}
