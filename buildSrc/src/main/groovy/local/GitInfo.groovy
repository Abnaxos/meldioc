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

package local

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.regex.Pattern

class GitInfo {

  static final P_EXECUTABLE = 'git.executable'
  static final String VERSION_RE = /(?<v>\d+(?:\.\d+)*)/
  static final NEWLINE_RE = ~/\r?\n|\r/

  final Project project
  final String gitExecutable

  Pattern releaseBranchRE = ~('(?:release-candidate|maintenance)/' + VERSION_RE + '')
  Pattern releaseTagRE = ~('release/' + VERSION_RE)
  Pattern snapshotBranchRE = ~('(?:snapshot)/' + VERSION_RE + '')

  String branch
  String hash
  String version
  boolean snapshot = true

  GitInfo(Project project) {
    this.project = project
    gitExecutable = project.properties[P_EXECUTABLE] ?: project.gradle.properties[P_EXECUTABLE] ?: 'git'
  }

  static load(Project project) {
    return new GitInfo(project).load()
  }

  def load() {
    branch = gitOneLine 'rev-parse', '--abbrev-ref', 'HEAD'
    hash = gitOneLine 'rev-parse', 'HEAD'
    def rcMatcher = releaseBranchRE.matcher(this.branch)
    if (rcMatcher.matches()) {
      def releases = git('tag', '-l', '--points-at', 'HEAD')
          .collect {releaseTagRE.matcher(it)}
          .findAll {it.matches()}
          .collect {it.group('v')}
      if (releases.size() == 0) {
        version = rcMatcher.group('v') + '-SNAPSHOT'
        snapshot = true
      } else if (releases.size() == 1) {
        version = releases[0]
        snapshot = false
      } else {
        throw new GradleException("No distinct version tag found: $releases")
      }
    } else {
      def snapshotMatcher = snapshotBranchRE.matcher(this.branch)
      if (snapshotMatcher.matches()) {
        version = snapshotMatcher.group('v') + '-SNAPSHOT'
        snapshot = true
      }
    }
    if (version == null) {
      version = project.properties['ch.raffael.meldioc.fallbackSnapshotVersion'] + '-SNAPSHOT'
    }
    project.logger.quiet "Detected version: $version (snapshot: $snapshot)"
    return this
  }

  String gitOneLine(Object... gitArgs) {
    def lines = git(gitArgs)
    if (lines.size() != 1) {
      throw new GradleException("Expected one line, got $lines")
    }
    return lines[0]
  }

  List<String> git(Object... gitArgs) {
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    project.exec {
      workingDir = project.rootDir
      standardOutput = out
      commandLine = [gitExecutable] + gitArgs.collect {it as String}
    }
    NEWLINE_RE.split(new String(out.toByteArray())).collect {it.trim()}.findAll {!it.isEmpty()} as List
  }
}
