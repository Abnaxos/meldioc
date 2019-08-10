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

package ch.raffael.compose.processor.test.tools

import groovy.io.FileType

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TestEnvironment {

  private static Path BASEPATH
  private static Path WORK_BASEPATH
  private static Path SOURCE_BASEPATH
  private static String CLASSPATH
  private static String PROCESSOR_PATH

  private final static INIT_LOCK = new Object()
  private static volatile INITIALIZED = false

  static Path sourceOutputPath(String caseName) {
    init()
    prepareDirectory(WORK_BASEPATH.resolve(path(caseName)).resolve('src'))
  }

  static Path classOutputPath(String caseName) {
    init()
    prepareDirectory(WORK_BASEPATH.resolve(path(caseName)).resolve('classes'))
  }

  static String classpath(String caseName) {
    init()
    CLASSPATH
  }

  static String processorPath(String caseName) {
    init()
    PROCESSOR_PATH
  }

  static Path sourcePath(String caseName) {
    init()
    SOURCE_BASEPATH.resolve(path(caseName))
  }

  static List<File> sourceFiles(String caseName) {
    List<File> sources = []
    sourcePath(caseName).toFile().eachFileRecurse(FileType.FILES) {f ->
      if (f.name.endsWith('.java')) {
        sources.add(f)
      }
    }
    if (!sources) {
      throw new IllegalStateException("No source files for case $caseName")
    }
    return sources
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
          propertiesPath.withInputStream {properties.load(it)}
          CLASSPATH = properties.getProperty('classpath')
          PROCESSOR_PATH = properties.getProperty('processor-path')
          properties.get('classpath')
          if (!basePath) {
            throw new IllegalStateException('Cannot determine project base directory')
          }
          WORK_BASEPATH = basePath.resolve('target/processor-tests.work')
          SOURCE_BASEPATH = basePath.resolve(path('src/test/cases'))
          BASEPATH = basePath
          INITIALIZED = true
        }
      }
    }
  }

  private static path(String path) {
    return Paths.get(path.replace('/', File.separator))
  }

  private static Path prepareDirectory(Path path) {
    path.toFile().deleteDir()
    Files.createDirectories(path)
    path
  }
}
