/*
 *  Copyright (c) 2022 Raffael Herzog
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

package ch.raffael.meldioc.processor;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.ExtensionPoint;
import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.Setup;
import ch.raffael.meldioc.meta.Generated;
import ch.raffael.meldioc.processor.env.Environment;
import io.vavr.collection.List;
import io.vavr.control.Option;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
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
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes({
    "ch.raffael.meldioc.Configuration",
    "ch.raffael.meldioc.Feature",
    "ch.raffael.meldioc.Feature.Mount",
    "ch.raffael.meldioc.Feature.DependsOn",
    "ch.raffael.meldioc.Feature.Import",
    "ch.raffael.meldioc.Provision",
    "ch.raffael.meldioc.ExtensionPoint",
    "ch.raffael.meldioc.ExtensionPoint.Acceptor",
    "ch.raffael.meldioc.Setup",
    "ch.raffael.meldioc.Parameter",
    "ch.raffael.meldioc.Parameter.Prefix"})
@SupportedOptions(MeldProcessor.OPT_INCLUDE_MSG_ID)
public class MeldProcessor extends AbstractProcessor {

  public static final String OPT_INCLUDE_MSG_ID = "ch.raffael.meldioc.includeMessageId";
  public static final String OPT_GENERATE_ON_ERRORS = "ch.raffael.meldioc.generateOnErrors";
  public static final String OPT_VERBOSE = "ch.raffael.meldioc.verbose";

  private static final String LANG_VERSION_PREFIX = "RELEASE_";
  private static final io.vavr.collection.Set<String> OLD_LANG_VERSIONS = List.rangeClosed(0, 10)
      .map(v -> LANG_VERSION_PREFIX + v).toSet();
  private static final io.vavr.collection.Set<String> KNOWN_LANG_VERSIONS = List.rangeClosed(11, 19)
      .map(v -> LANG_VERSION_PREFIX + v).toSet();

  @Override
  public boolean process(@Nonnull Set<? extends TypeElement> annotations, @Nonnull RoundEnvironment roundEnv) {
    if (OLD_LANG_VERSIONS.contains(processingEnv.getSourceVersion().name())) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Language version too old: " + OLD_LANG_VERSIONS);
      return true;
    }
    if (!KNOWN_LANG_VERSIONS.contains(processingEnv.getSourceVersion().name())) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
          "Unknown language version " + processingEnv.getSourceVersion().name()
              + "; some constructs may cause unpredictable behaviour");
    }
    Environment env = new Environment(processingEnv,
        Option.of(processingEnv.getOptions().get(OPT_INCLUDE_MSG_ID))
            .map(v -> v.equals(String.valueOf(true)))
            .getOrElse(false));
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
        writeSourceFile(new Generator(MeldProcessor.class, env, (TypeElement) element));
      } else {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected a class", element);
      }
    } catch (Generator.Abort e) {
      if ("true".equals(processingEnv.getOptions().get(OPT_VERBOSE))) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "[meld] Code generation for " + element + " aborted: " + stackTrace(e));
      }
    } catch (Exception | AssertionError e) {
      reportFatal(element, e);
    }
  }

  private void writeSourceFile(Generator generator) {
    if (generator.errorCount() > 0 ) {
      if ("true".equals(processingEnv.getOptions().get(OPT_GENERATE_ON_ERRORS))) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "[meld] Generating " + generator.targetClassName() + " in spite of"
                + " " + generator.errorCount() + " errors (and " + generator.warningCount() + " warnings):"
                + " " + OPT_GENERATE_ON_ERRORS + " is set to true");
      } else if ("true".equals(processingEnv.getOptions().get(OPT_VERBOSE))) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "[meld] Generating " + generator.targetClassName());
      } else {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "[meld] Not generating " + generator.targetClassName() + " because"
                + " there were " + generator.errorCount() + " errors (and " + generator.warningCount() + " warnings)");
        return;
      }
    }
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
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal: " + stackTrace(e), element);
  }

  private String stackTrace(Throwable e) {
    StringWriter out = new StringWriter();
    PrintWriter print = new PrintWriter(out);
    e.printStackTrace(print);
    print.close();
    return out.toString();
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
        ExtensionPoint.class.getCanonicalName(), ExtensionPoint.class.getCanonicalName()+".Acceptor",
        Feature.class.getCanonicalName(), Feature.Mount.class.getCanonicalName(), Feature.DependsOn.class.getCanonicalName(),
        Provision.class.getCanonicalName()
    );
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
