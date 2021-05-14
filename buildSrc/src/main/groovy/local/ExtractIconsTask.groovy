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

import groovy.namespace.QName
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction


/**
 * TODO JavaDoc
 */
class ExtractIconsTask extends DefaultTask {

  static final String NS_SVG = 'http://www.w3.org/2000/svg'
  static final String NS_INKSCAPE = 'http://www.inkscape.org/namespaces/inkscape'

  static final QName E_GROUP = new QName(NS_SVG, 'g')
  static final QName A_GROUPMODE = new QName(NS_INKSCAPE, 'groupmode')
  static final QName A_LABEL = new QName(NS_INKSCAPE, 'label')
  static final QName A_STYLE = new QName(NS_SVG, 'style')

  @InputFile
  File sourceFile
  @OutputDirectory
  File outputDir

  @Input
  Map<String, Iterable<String>> icons = [:]

  @TaskAction
  void extractIcons() {
    def sourceXml = new XmlParser().parse(sourceFile)
    project.mkdir outputDir
    for (gen in icons.entrySet()) {
      logger.quiet "Extracting icon: $gen.key: $gen.value"
      def filtered = this.filterLayers(sourceXml, gen.value.toSet())
      new File(outputDir, gen.key+'.svg').withPrintWriter('UTF-8') {out ->
        out.println '<?xml version="1.0" encoding="UTF-8"?>'
        new XmlNodePrinter(out).print(filtered)
      }
    }
  }

  private Node filterLayers(Node sourceXml, Set<String> keep) {
    sourceXml = (Node)sourceXml.clone()
    def iter = sourceXml.children().iterator()
    while (iter.hasNext()) {
      def c = iter.next()
      if (!(c instanceof Node)) {
        continue
      }
      def n = (Node)c
      if (n.name() != E_GROUP) {
        continue
      }
      if (n.attribute(A_GROUPMODE) != 'layer') {
        continue
      }
      def label = n.attribute(A_LABEL)
      if (label != null && !keep.contains(label)) {
        iter.remove()
      }
      n.attributes().remove(A_STYLE)
      n.attributes().remove(A_STYLE.localPart)
      n.attributes().put(A_STYLE, 'display:inline')
    }
    return sourceXml
  }
}
