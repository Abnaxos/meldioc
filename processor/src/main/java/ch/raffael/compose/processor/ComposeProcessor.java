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

package ch.raffael.compose.processor;

import ch.raffael.compose.Assembly;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.processor.env.AptProblemReporter;
import ch.raffael.compose.processor.env.Environment;

import javax.annotation.Nonnull;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

/**
 * @since 2019-03-23
 */
@SuppressWarnings("unused")
public class ComposeProcessor extends AbstractProcessor {

  @Override
  public boolean process(@Nonnull Set<? extends TypeElement> annotations, @Nonnull RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(Assembly.class).forEach(this::processElement);
    return true;
  }

  private void processElement(Element element) {
    try {
      Environment env = new Environment(processingEnv, new AptProblemReporter(processingEnv.getMessager()));
      if (element instanceof TypeElement && element.getAnnotation(Generated.class) == null) {
        writeSourceFile(new Generator(env, (TypeElement) element));
      } else {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected a class", element);
      }
    } catch (Exception | AssertionError e) {
      reportFatal(element, e);
    }
  }

  private void writeSourceFile(Generator generator) {
    try {
      var source = generator.generate();
      var out = processingEnv.getFiler().createSourceFile(generator.targetClassName(), generator.sourceType());
      try (var writer = out.openWriter()) {
        writer.write(source);
      }
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          "Cannot write source file for " + generator.targetClassName() + ": " + e.getLocalizedMessage());
    }
  }

  private void reportFatal(Element element, Throwable e) {
    StringWriter out = new StringWriter();
    PrintWriter print = new PrintWriter(out);
    e.printStackTrace(print);
    print.close();
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal: " + out.toString(), element);
//    Stream.of(out.toString().split("\\r?\\n")).forEach(l ->
//        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal: " + l, element));
  }

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(Assembly.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_11;
  }

}
