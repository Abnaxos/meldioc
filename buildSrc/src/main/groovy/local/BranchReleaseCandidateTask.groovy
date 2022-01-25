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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

class BranchReleaseCandidateTask extends DefaultTask {

  @Option(option = 'next', description = 'The next version to be set in the current branch')
  @Internal
  String nextVersion

  BranchReleaseCandidateTask() {
    outputs.upToDateWhen {false}
  }

  @TaskAction
  void branchReleaseCandidate() {
    if (!nextVersion) {
      throw new GradleException("Invalid next version: $nextVersion")
    }
    def v = Versioning.get(project)
    def git = new GitInvoker(project)
    def rcBranch = "release-candidate/$v.baseVersion" as String
    if (!git('branch', '-l', rcBranch).empty) {
      throw new GradleException("Branch $rcBranch exists")
    }
    v.updateVersionProperties(logger, nextVersion)
    if (git('diff', v.versionPropertiesPath).empty) {
      throw new GradleException("$v.versionPropertiesPath is unchanged")
    }
    logger.quiet "Create branch $rcBranch"
    git 'branch', rcBranch
    logger.quiet "Commit version.properties"
    git 'commit', '-m', "Set version to $nextVersion", v.versionPropertiesPath
    git 'checkout', rcBranch
  }
}
