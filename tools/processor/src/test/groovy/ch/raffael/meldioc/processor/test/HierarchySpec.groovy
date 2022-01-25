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

package ch.raffael.meldioc.processor.test

import ch.raffael.meldioc.model.messages.Message
import spock.lang.Specification

import static ch.raffael.meldioc.processor.test.tools.ProcessorTestCase.compile

class HierarchySpec extends Specification {

  def "Non-feature super classes should be annotated with `@Feature.Import`"() {
    given:
    final importedClassName = 'c.hierarchy.imports.ImportedFeature'
    when:
    final c = compile('c/hierarchy/imports')

    then:
    with (c.findMessage {it.id == Message.Id.MissingFeatureImportAnnotation}) {
      it.pos == c.marker('missing-import')
      it.message.contains(importedClassName)
    }
    with (c.findMessage {it.id == Message.Id.MissingFeatureImportAnnotation}) {
      it.pos == c.marker('missing-import-iface')
      it.message.contains(importedClassName)
    }
    with (c.findMessage {it.id == Message.Id.MissingFeatureImportAnnotation}) {
      it.pos == c.marker('missing-import-with-hierarchy')
      it.message.contains(importedClassName)
    }
    c.findAllMessages {it.id == Message.Id.FeatureInterfacesShouldDeclareProvisionsOnly}.size() == 2
    c.allGood
  }
}
