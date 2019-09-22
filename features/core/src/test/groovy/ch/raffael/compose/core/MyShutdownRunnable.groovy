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

package ch.raffael.compose.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

import static ch.raffael.compose.core.ShutdownHooks.shutdownHooks

class MyShutdownRunnable implements Runnable {

  static final String HOOK1 = 'hook1'
  static final String HOOK2 = 'hook2'

  private final Path shutdownPath
  private final Duration delay

  MyShutdownRunnable(Path shutdownPath, Duration delay) {
    this.delay = delay
    this.shutdownPath = shutdownPath
  }

  @Override
  void run() {
    try {
      Files.newOutputStream(shutdownPath).close()
      sleep(delay.toMillis())
    }
    catch (Exception e) {
      e.printStackTrace()
    }
  }

  static void main(String[] args) throws Exception {
    def p = Paths.get(args[0])
    shutdownHooks()
        .add(new MyShutdownRunnable(p.resolve(HOOK1), Duration.ofMillis(500)))
        .add(new MyShutdownRunnable(p.resolve(HOOK2), Duration.ofMillis(0)))
    System.exit(0)
  }

}
