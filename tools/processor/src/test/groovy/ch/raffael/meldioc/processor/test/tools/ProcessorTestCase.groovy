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

package ch.raffael.meldioc.processor.test.tools

import ch.raffael.meldioc.processor.ComposeProcessor
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import javax.annotation.Nullable
import javax.tools.*
import java.nio.file.Files
import java.nio.file.Path

class ProcessorTestCase {

  final static GENERATE_ON_ERRORS = false

  final Path sourcePath
  final String caseName
  final List<Message> messages = []
  final Map<String, Message.SourcePosition> markers = [:]
  ClassLoader rtClassLoader = null
  Config shellConfig = ConfigFactory.empty()

  private ProcessorTestCase(String caseName) {
    this.caseName = caseName
    sourcePath = TestEnvironment.sourcePath(caseName)
  }

  static ProcessorTestCase processorTestCase(String caseName) {
    new ProcessorTestCase(caseName)
  }

  static ProcessorTestCase compile(String caseName) {
    return processorTestCase(caseName).compile()
  }

  ProcessorTestCase compile() {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler()
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)
    List<String> options = Arrays.asList(
        '-d', prepareOutputDirectory(TestEnvironment.classOutputPath(caseName)) as String,
        '-s', prepareOutputDirectory(TestEnvironment.sourceOutputPath(caseName)) as String,
        '-cp', TestEnvironment.classpath(caseName) as String,
        '-Xlint:unchecked', '-g',
        '-processor', [MarkerProcessor, ComposeProcessor].collect {it.name}.join(','),
        "-A$ComposeProcessor.OPT_INCLUDE_MSG_ID=true" as String,
        "-A$ComposeProcessor.OPT_GENERATE_ON_ERRORS=$GENERATE_ON_ERRORS" as String,
        '--processor-path', TestEnvironment.processorPath(caseName))
    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
        TestEnvironment.sourceFiles(caseName))
    println "Compiling: $compilationUnits"
    int markerCount = 0
    int diagCount = 0
    def diag = new DiagnosticListener<JavaFileObject>() {
      @Override
      void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (diagnostic.getMessage(Locale.US).startsWith('The following options were not recognized by any processor:')) {
          // ignore, see https://bugs.openjdk.java.net/browse/JDK-8162455
          return
        }
        def marker = MarkerProcessor.marker(diagnostic)
        if (marker != null) {
          markerCount++
          if (markers.containsKey(marker)) {
            System.err.println "Duplicate marker: $marker"
          }
          markers[marker] = new Message.SourcePosition(sourcePath, diagnostic)
        }
        else {
          diagCount++
          println diagnostic
          if (!(diagnostic.getMessage(Locale.US) =~ /^\[compose] (Generating|Not generating) .*/)) {
            messages.add(new Message(sourcePath, diagnostic))
          }
        }
      }
    }
    def result = compiler.getTask(null, fileManager, diag, options, null, compilationUnits).call()
    println "Compilation finished ${result?'successfully':'with errors'}: $diagCount messages, $markerCount markers"
    messages.sort({l, r -> l.pos.line<=>r.pos.line ?: l.pos.col<=> r.pos.col})
    this
  }

  Message.SourcePosition marker(String name) {
    markers[name]
  }

  boolean isAllGood() {
    return warningCount == 0 && errorCount == 0
  }

  int getWarningCount() {
    messages.findAll {m -> !m.consumed && m.warning}.size()
  }

  int getErrorCount() {
    messages.findAll {m -> !m.consumed && m.error}.size()
  }

  Message message() {
    def next = messages.find {!it.consumed}
    if (next) {
      next.consumed = true
    }
    next ?: new Message('NO MORE MESSAGES')
  }

  @Nullable
  Message findMessage(Closure filter) {
    def found = messages.findAll {!it.consumed}.find(filter)
    if (found) {
      found.consumed = true
    }
    found
  }

  ProcessorTestCase shellConfig(Config config) {
    shellConfig = config.resolve()
    return this
  }

  def context(String name = 'Context') {
    shellBuilder(name).build()
  }

  def shellBuilder(String name = 'Context') {
    loadClass("${caseName.replace('/', '.')}.${name}Shell").builder().config(shellConfig)
  }

  def loadClass(String name, boolean resolve = true) {
    if (!rtClassLoader) {
      rtClassLoader = new URLClassLoader([TestEnvironment.classOutputPath(caseName).toUri().toURL()] as URL[],
                                         getClass().classLoader)
    }
    if (name.startsWith('.')) {
      name = caseName.replace('/', '.') + name
    }
    return Class.forName(name, resolve, rtClassLoader)
  }

  private static Path prepareOutputDirectory(Path path) {
    path.toFile().deleteDir()
    Files.createDirectories(path)
    path
  }
}
