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

package ch.raffael.meldioc.processor.test.tools


import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TestEnvironment {

  private static Path BASEPATH
  private static Path WORK_BASEPATH
  private static Path SOURCE_BASEPATH
  private static String CLASSPATH
  private static String CLASSPATH_NO_TS_CONFIG
  private static String PROCESSOR_PATH

  private final static INIT_LOCK = new Object()
  private static volatile INITIALIZED = false

  static Path sourceOutputPath(String caseName) {
    init()
    WORK_BASEPATH.resolve(path(caseName)).resolve('src')
  }

  static Path classOutputPath(String caseName) {
    init()
    WORK_BASEPATH.resolve(path(caseName)).resolve('classes')
  }

  static String classpath(String caseName) {
    init()
    CLASSPATH
  }

  static String classpathNoConfig(String caseName) {
    init()
    CLASSPATH_NO_TS_CONFIG
  }

  static String processorPath(String caseName) {
    init()
    PROCESSOR_PATH
  }

  static Path sourcePath(String caseName) {
    init()
    SOURCE_BASEPATH.resolve(path(caseName)).normalize()
  }

  static List<File> sourceFiles(String caseName) {
    Set<File> sources = []
    def addSources = {File f ->
      if (f.isFile() && f.name.endsWith('.java')) {
        sources.add(f)
      }
    }
    def path = sourcePath(caseName)
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("No such test case: $path")
    }
    path.toFile().eachFileRecurse(addSources)
    while (true) {
      path = path.parent
      path.toFile().eachFile(addSources)
      if (path == SOURCE_BASEPATH) {
        break
      }
    }
    if (!sources) {
      throw new IllegalStateException("No source files for case $caseName")
    }
    return sources as List
  }

  private static synchronized init() {
    if (!INITIALIZED) {
      synchronized (INIT_LOCK) {
        if (!INITIALIZED) {
          def propertiesPath = path('target/processor-tests.properties')
          def basePath = ['.', 'tools/processor'].
              collect {Paths.get(it.replace('/', File.separator))}.
              find {p ->
                Files.isRegularFile(p.resolve(propertiesPath))
              }
          if (!basePath) {
            throw new IllegalStateException('Cannot determine base path')
          }
          basePath = basePath.toAbsolutePath().normalize()
          println "Using base path: $basePath"
          def properties = new Properties()
          try (def s = Files.newInputStream(propertiesPath)) {
            properties.load(s)
          }
          CLASSPATH = properties.getProperty('classpath')
          CLASSPATH_NO_TS_CONFIG = properties.getProperty('classpath.no-ts-config')
          PROCESSOR_PATH = properties.getProperty('processor-path')
          properties.get('classpath')
          if (!basePath) {
            throw new IllegalStateException('Cannot determine project base directory')
          }
          WORK_BASEPATH = basePath.resolve('target/processor-tests.work')
          SOURCE_BASEPATH = basePath.resolve(path('src/test-cases'))
          BASEPATH = basePath
          INITIALIZED = true
        }
      }
    }
  }

  private static path(String path) {
    return Paths.get(path.replace('/', File.separator))
  }
}
