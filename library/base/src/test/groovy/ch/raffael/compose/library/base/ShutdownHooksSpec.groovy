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

package ch.raffael.compose.library.base

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ShutdownHooksSpec extends Specification {

  def "Shutdown hooks are called in the order they were added"() {
    given: "A temporary directory where the shutdown hook will create some files"
    Path path = Files.createTempDirectory(ShutdownHooksSpec.class.simpleName)
    def hook1 = path.resolve(MyShutdownRunnable.HOOK1)
    def hook2 = path.resolve(MyShutdownRunnable.HOOK2)

    when: "Run a JVM that adds shutdown hooks that create some files to prove that the hooks have been run"
    def proc = Runtime.getRuntime().exec(
        [Paths.get(System.getProperty('java.home')).resolve('bin').resolve('java').toString(),
         '-cp', System.getProperty('java.class.path'),
         MyShutdownRunnable.class.name, path.toString()] as String[])
    proc.waitFor()

    then: "The shutdown hook files exist"
    Files.isRegularFile(hook1)
    Files.isRegularFile(hook2)
    and: "The second file is newer than the first one"
    Files.getLastModifiedTime(hook2).toMillis() > Files.getLastModifiedTime(hook1).toMillis()

    cleanup:
    Files.deleteIfExists(hook1)
    Files.deleteIfExists(hook2)
    Files.deleteIfExists(path)
  }
}
