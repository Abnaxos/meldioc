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
import org.gradle.api.logging.Logger

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import java.util.stream.Stream

import static java.nio.charset.StandardCharsets.UTF_8

class Versioning {

  static final String EXT_NAME = 'versioning'
  static final String RELEASE_PROPERTY = 'RELEASE'

  final Path versionPropertiesPath
  final String baseVersion
  final boolean release
  final String proposedVersion

  Versioning(Project project, Path versionPropertiesPath, Properties props) {
    this.versionPropertiesPath = versionPropertiesPath.toAbsolutePath().normalize()
    baseVersion = props.getProperty('v')?.trim()
    release = Stream.iterate(project, {it != null}, {it.parent})
        .filter({it.projectDir.isDirectory()})
        .flatMap({Optional.ofNullable(it.properties).stream()})
        .flatMap({Optional.ofNullable(it.get(RELEASE_PROPERTY) as String).stream()})
        .findFirst()
        .map({
          if (it != baseVersion) {
            throw new GradleException("Property $RELEASE_PROPERTY mismatch: expected '$baseVersion', got '$it'")
          }
          true
        })
        .orElse(false)
    proposedVersion = release ? baseVersion : "$baseVersion-SNAPSHOT"
  }

  static Versioning get(Project project) {(Versioning)project.extensions.getByName(EXT_NAME)}
  static Versioning find(Project project) {(Versioning)project.extensions.findByName(EXT_NAME)}

  void updateVersionProperties(Logger logger, String newVersion) {
    logger.quiet "Updating $versionPropertiesPath to $newVersion"
    def pattern = Pattern.compile(/^(?<h>\s*v\s*[:=]\s*).*$/, Pattern.MULTILINE)
    Files.writeString(versionPropertiesPath,
        pattern.matcher(Files.readString(versionPropertiesPath, UTF_8)).replaceAll(
            '${h}' + newVersion.replace('$', '\\$')),
        UTF_8)
  }
}
