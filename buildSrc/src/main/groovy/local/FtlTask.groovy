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


import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

import java.lang.reflect.UndeclaredThrowableException

class FtlTask extends DefaultTask {

  public String sourceSet = 'main'
  public String language = 'java'
  public String languageExt = null

  @Input
  public Map<String, Object> model = [:]
  @Input
  public Map<String, String> autoImports = [:]
  @Input
  public List<String> autoIncludes = []

  @Input
  public List<String> includes = []
  @Input
  public List<String> excludes = []
  @Input
  public String encoding = 'UTF-8'
  @Input
  public String lineSeparator = System.lineSeparator()
  @Input
  public CleanupFunction cleanup = CleanupFunction.JAVA

  @Input
  public final Map<String, Object> attributes = new LinkedHashMap<>()

  private File templateDirectory = null
  private File outputDirectory = null

  File getTemplateDirectory() {
    templateDirectory ?: project.file("src/$sourceSet/$language")
  }

  @InputFiles
  FileCollection getAllTemplateFiles() {
    project.fileTree(getTemplateDirectory())
  }

  void setTemplateDirectory(File templateDirectory) {
    this.templateDirectory = templateDirectory
  }

  File getOutputDirectory() {
    outputDirectory ?: project.file("$project.buildDir/generated/sources/ftl/$language/$sourceSet")
  }

  @OutputFiles
  FileCollection getOutputFiles() {
    return project.files(project.files(processFiles({_, __, outputFile -> outputFile})))
  }

  void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory
  }

  void autoImport(Map<String, Object> autoImport) {
    autoImport.each { ns, tpl -> autoImports.put(ns, tpl as String)}
  }

  void autoInclude(autoInclude) {
    autoIncludes.add(autoInclude as String)
  }

  @TaskAction
  void generateAll() throws GradleException {
    try {
      def ftl = new Configuration(Configuration.VERSION_2_3_30)
      ftl.directoryForTemplateLoading = getTemplateDirectory()
      ftl.defaultEncoding = encoding
      autoImports.each {ns, tpl -> ftl.addAutoImport(ns, tpl)}
      autoIncludes.each {ftl.addAutoInclude(it)}
      ftl.tagSyntax = Configuration.SQUARE_BRACKET_TAG_SYNTAX
      ftl.logTemplateExceptions = false
      ftl.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
      ftl.wrapUncheckedExceptions = true
      processFiles {_, template, target ->
        logger.quiet "Generating: $template"
        def tpl = ftl.getTemplate(template)
        project.mkdir(target.parent)
        def m = new LinkedHashMap()
        m.put('templatePath', template)
        m.putAll(model)
        def rawResult = new StringWriter()
        tpl.process(m, rawResult)
        def resultLines = rawResult.toString().readLines()
        resultLines = (cleanup ?: cleanup.AS_IS).cleanup(resultLines)
        target.withWriter(encoding) {out ->
          resultLines.each {
            out.write(it)
            out.write(System.lineSeparator())
          }
        }
      }
    } catch (Exception e ) {
      if (e instanceof UndeclaredThrowableException) {
        if (e.cause instanceof Exception) {
          e = (Exception)e.cause
        }
      }
      if (e instanceof RuntimeException) {
        throw e
      }
      if (e.class.name.startsWith('freemarker.')) {
        e.message.readLines().each {logger.error it}
        throw new GradleException("Error processing templates", e)
      }
      throw new GradleException(e as String, e)
    }
  }

  def <T> List<T> processFiles(Visitor<T> visitor) throws Exception {
    def templateDir = getTemplateDirectory()
    def outputDir = getOutputDirectory()
    def result = new ArrayList<T>()
    project.fileTree(dir: templateDir, includes: includes ?: ["**/*.${languageExt ?: language}.ftl"], excludes: excludes)
        .visit {FileVisitDetails f ->
          if (!f.directory) {
            def out = project.file("$outputDir/$f.path" as String)
            if (out.name.endsWith('.ftl')) {
              out = new File(out.parentFile, out.name.substring(0, out.name.length() - 4))
            }
            result.add(visitor.visit(templateDir, f.relativePath.pathString, out))
          }
        }
    return result
  }

  @FunctionalInterface
  interface CleanupFunction extends Serializable {
    CleanupFunction AS_IS = {List<String> source -> source}
    CleanupFunction JAVA = {List<String> source ->
      List<String> result = []
      def emptyCount = 0
      for (line in source) {
        line = line.stripTrailing()
        if (!line) {
          emptyCount++
        } else {
          if (emptyCount) {
            if (!(line ==~ /\s*}/)) {
              result.add('')
            }
          }
          emptyCount = 0
          line = line.replaceAll(/(\S)\s{2,}/, '$1 ')
          line = line.replaceAll(/([(\[<])\s+/, '$1')
          line = line.replaceAll(/(\S)\s+([)\]>])/, '$1$2')
          line = line.replaceAll(/,(\S)/, ', $1')
          result.add(line)
        }
      }
      return result
    }

    List<String> cleanup(List<String> cleanup)
  }

  @FunctionalInterface
  interface Visitor<T> {
    T visit(File templateDir, String templateName, File outputPath) throws Exception
  }
}
