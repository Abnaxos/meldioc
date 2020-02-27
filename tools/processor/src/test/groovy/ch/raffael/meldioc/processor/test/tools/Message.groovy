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

import ch.raffael.meldioc.processor.Diagnostics
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import java.nio.file.Path

class Message {

  private static Set<Diagnostic.Kind> WARNING_KINDS = Set.of(
      Diagnostic.Kind.WARNING, Diagnostic.Kind.MANDATORY_WARNING)
  private static Set<Diagnostic.Kind> ERROR_KINDS = Set.of(Diagnostic.Kind.ERROR)

  final Diagnostic<? extends JavaFileObject> diagnostic
  final SourcePosition pos
  final ch.raffael.meldioc.model.messages.Message.Id id
  final String message
  boolean consumed

  Message(Path sourcePath, Diagnostic<? extends JavaFileObject> diagnostic) {
    this.diagnostic = diagnostic
    this.pos = new SourcePosition(sourcePath, diagnostic)
    def t = Diagnostics.extractMessageId(diagnostic.getMessage(Locale.US))
    this.id = t._1.orNull
    this.message = t._2
  }

  Message(String message) {
    this.diagnostic = null
    this.pos = new SourcePosition(null, -1, -1)
    this.id = null
    this.message = message
  }

  boolean isWarning() {
    WARNING_KINDS.contains(diagnostic.kind)
  }

  boolean isError() {
    ERROR_KINDS.contains(diagnostic.kind)
  }

  boolean isInfo() {
    !warning && !error
  }

  @Override
  String toString() {
    return pos + ": " + message
  }

  @EqualsAndHashCode
  @ToString
  static class SourcePosition {

    final String file
    final long line
    final long col

    SourcePosition(Path sourceFile, long line, long col) {
      this.file = sourceFile.toString().replace(File.separator, '/')
      this.line = line
      this.col = col
    }

    SourcePosition(Path sourcePath, Diagnostic<? extends JavaFileObject> diagnostic) {
      this(relativeSourcePath(sourcePath, diagnostic), diagnostic.lineNumber, diagnostic.columnNumber)
    }

    private static Path relativeSourcePath(Path sourcePath, Diagnostic<? extends JavaFileObject> diagnostic) {
      def p = extractPath(diagnostic)
      p ? sourcePath.relativize(p) : null
    }

    private static Path extractPath(Diagnostic<? extends JavaFileObject> diagnostic) {
      // it's a com.sun.tools.javac.file.PathFileObject
      diagnostic.source?.path
    }
  }
}
