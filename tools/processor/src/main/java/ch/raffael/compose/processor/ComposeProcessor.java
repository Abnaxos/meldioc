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

import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Feature;
import ch.raffael.compose.Parameter;
import ch.raffael.compose.Provision;
import ch.raffael.compose.Setup;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.processor.env.Environment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;

/**
 * Main processor class.
 */
@SuppressWarnings("unused")
public class ComposeProcessor extends AbstractProcessor {

  @Override
  public boolean process(@Nonnull Set<? extends TypeElement> annotations, @Nonnull RoundEnvironment roundEnv) {
    Environment env = new Environment(processingEnv);
    Optional<? extends TypeElement> configurationAnnotation = annotations.stream()
        .filter(e -> e.getQualifiedName().toString().equals(Configuration.class.getCanonicalName())).findAny();
    validateParticipants(annotations, roundEnv, env, configurationAnnotation);
    configurationAnnotation.ifPresent(
        a -> roundEnv.getElementsAnnotatedWith(a).forEach(elem -> generateConfigurationShell(env, elem)));
    return true;
  }

  private Optional<TypeElement> findTypeElement(@Nullable Element elem) {
    while (elem != null && !(elem instanceof TypeElement)) {
      elem = elem.getEnclosingElement();
    }
    return Optional.ofNullable((TypeElement) elem);
  }

  private void validateParticipants(@Nonnull Set<? extends TypeElement> annotations,
                                    @Nonnull RoundEnvironment roundEnv,
                                    Environment env,
                                    Optional<? extends TypeElement> configurationAnnotation) {
    annotations.stream()
        .filter(e -> configurationAnnotation.map(asm -> !asm.equals(e)).orElse(true))
        .flatMap(e -> roundEnv.getElementsAnnotatedWith(e).stream())
        .map(this::findTypeElement)
        .flatMap(Optional::stream)
        .filter(e -> configurationAnnotation.map(asm -> e.getAnnotationMirrors().stream()
            .noneMatch(a -> asm.equals(e))).orElse(true))
        .distinct()
        .forEach(e -> {
//          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Checking model", e);
          env.model().modelOf(env.typeRef(e.asType()));
        });
  }

  private void generateConfigurationShell(Environment env , Element element) {
    try {
      if (element instanceof TypeElement && element.getAnnotation(Generated.class) == null) {
        writeSourceFile(new Generator(ComposeProcessor.class, env, (TypeElement) element));
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
      var out = processingEnv.getFiler().createSourceFile(generator.targetClassName(), generator.sourceElement());
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
  }

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        Configuration.class.getCanonicalName(),
        Setup.class.getCanonicalName(),
        Parameter.class.getCanonicalName(), Parameter.Prefix.class.getCanonicalName(),
        ExtensionPoint.Api.class.getCanonicalName(), ExtensionPoint.Provision.class.getCanonicalName(),
        Feature.class.getCanonicalName(), Feature.Mount.class.getCanonicalName(), Feature.DependsOn.class.getCanonicalName(),
        Provision.class.getCanonicalName()
    );
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_11;
  }

}
