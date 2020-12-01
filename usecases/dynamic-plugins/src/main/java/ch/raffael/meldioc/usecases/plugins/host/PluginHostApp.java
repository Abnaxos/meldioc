/*
 *  Copyright (c) 2020 Raffael Herzog
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

package ch.raffael.meldioc.usecases.plugins.host;

import ch.raffael.meldioc.library.base.lifecycle.Lifecycle;
import ch.raffael.meldioc.logging.Logging;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;

import static ch.raffael.meldioc.logging.Logging.logger;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class PluginHostApp {

  private static final Logger LOG = logger();

  public static void main(String[] args) throws Exception {
    Logging.init();
    var ctx = DefaultHostContextShell.builder().config(ConfigFactory.empty()).build();
    Lifecycle.of(ctx.lifecycleFeature())
        .asApplication(LOG)
        .start();
    ctx.messages().all().forEach(m -> System.out.println("MSG: " + m));
    //Thread.sleep(1000);
    ctx.lifecycleFeature().shutdownActuator().performShutdown();
  }
}
