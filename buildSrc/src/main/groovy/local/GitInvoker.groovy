/*
 *  Copyright (c) 2021 Raffael Herzog
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

class GitInvoker {

  final Project project
  final String gitExecutable

  GitInvoker(Project project, gitExecutable = 'git') {
    this.project = project
    this.gitExecutable = gitExecutable ?: project.properties['get.executable'] ?: 'git'
  }

  String commitHash() {
    gitOneLine 'rev-parse', 'HEAD'
  }

  boolean clean() {
    def changes = git('status', '--porcelain')
    changes.each {
      project.logger.info "(git) Changed: $it"
    }
    return changes.empty
  }

  void branchReleaseCandidate(String rc, String nextVersion) {
    def rcBranch = "release-candidate/$rc" as String
    git 'branch', rcBranch
    // update version.properties
    git 'commit', '-m', "Set version to $nextVersion", project.file('version.properties')
    git 'checkout', rcBranch
  }

  String branchName() {
    def branchName = gitOneLine 'rev-parse', '--abbrev-ref', 'HEAD'
    project.logger.info "(git) Detected branch: $branchName"
    return branchName
  }

  List<String> tags() {
    git('tag', '-l', '--points-at', 'HEAD')
  }

  String gitOneLine(Object... args) {
    gitOneLine(args as List)
  }

  String gitOneLine(List args) {
    def lines = git(args)
    if (lines.size() != 1) {
      throw new GradleException("git $args: expected one line, got ${lines.size()}")
    }
    return lines[0]
  }

  List<String> git(Object... args) {
    git(args as List)
  }

  List<String> git(List gitArgs) {
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    def cmdLine = [gitExecutable] + gitArgs.collect {it as String}
    def result = project.exec {
      workingDir = project.rootDir
      standardOutput = out
      commandLine = cmdLine
      ignoreExitValue = true
    }
    def lines = out.toString().readLines().collect {it.trim()}.findAll {!it.isEmpty()}
    if (result.exitValue != 0) {
      throw new GradleException("Git call $cmdLine failed:\n${lines.join('\n')}")
    }
    return lines
  }

  List<String> call(Object... args) {git(args)}
  List<String> call(List args) {git(args)}
}
