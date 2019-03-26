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

import ch.raffael.compose.Context;
import ch.raffael.compose.Mount;
import ch.raffael.compose.Provision;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.mirrors.MirroredAnnotation;
import ch.raffael.compose.processor.mirrors.MirroredAssembly;
import ch.raffael.compose.processor.model.CompositionInfo;
import ch.raffael.compose.processor.model.ModelElement;
import ch.raffael.compose.processor.model.MountMethod;
import ch.raffael.compose.processor.model.ProvisionMethod;
import ch.raffael.compose.processor.util.ElementPredicates;
import ch.raffael.compose.tooling.model.ClassRef;
import ch.raffael.compose.tooling.model.ModelElementConfig;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.time.Instant;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static ch.raffael.compose.processor.Debug.ON_DEVEL_MODE;

/**
 * @since 2019-03-23
 */
public class Generator {

  private final Instant timestamp = Instant.now();
  private final Environment env;
  private final TypeElement sourceType;
  private final CompositionInfo sourceInfo;

  private final MirroredAssembly mirroredAssembly;
  private final ClassName targetClassName;
  private final TypeSpec.Builder target;

  Generator(Environment env, TypeElement sourceType) {
    this.env = env;
    this.sourceType = sourceType;
    mirroredAssembly = MirroredAnnotation.find(env, sourceType, MirroredAnnotation::assembly)
        .orElseThrow(() -> new InternalErrorException(sourceType + " not annotated with " + Context.class.getSimpleName()));
    ClassRef targetRef = mirroredAssembly.config().assemblyClassRef(
        env.elements().getPackageOf(sourceType).getQualifiedName().toString(), sourceType.getSimpleName().toString());
    var validator = env.problems().validator(sourceType, mirroredAssembly.mirror());
    targetClassName = ClassName.get(
        validator.validJavaPackageName(targetRef.packageName())
            .substituteOnError(targetRef.packageName(), "$invalid$"),
        validator.validIdentifier(targetRef.className(), "class name")
            .substituteOnError(targetRef.className(), "$Invalid$"));
    target = TypeSpec.classBuilder(targetClassName);
    sourceInfo = env.compositionInfoFor(sourceType);
  }

  TypeElement sourceType() {
    return sourceType;
  }

  String targetClassName() {
    return targetClassName.toString();
  }

  String generate() {
    target.addAnnotation(AnnotationSpec.builder(Generated.class)
        .addMember("timestamp", "$S", timestamp.toString())
        .addMember("version", "$S", "TODO")
        .build());
    if (!Debug.FAILSAFE_GEN) {
      target.superclass(TypeName.get(sourceType.asType()));
      new AssemblyClassValidator(env).validateAll(sourceType, targetClassName.packageName());
      if (!mirroredAssembly.config().packageLocal()) {
        target.addModifiers(Modifier.PUBLIC);
      }
      target.addModifiers(Modifier.FINAL);
      generateMembers();
    }
    return JavaFile.builder(targetClassName.packageName(), target.build())
        .addFileComment("Generated by ch.raffael.compose, " + new Date(timestamp.toEpochMilli()))
        .build().toString();
  }

  private void generateMembers() {
    var mounts = generateMountClasses();
    generateProvisionMethods();
    generateMountFields();
    generateMountMethods();
    mounts.map(TypeSpec.Builder::build).forEach(target::addType);
  }

  private void generateProvisionMethods() {
    BiConsumer<ProvisionMethod, MethodSpec.Builder> annotator = (c, m) -> m.addAnnotation(AnnotationSpec.builder(Provision.class)
        .addMember("shared", "$L", String.valueOf(c.config().shared()))
        .build());
    sourceInfo.provisionMethods()
        .map(ProvisionMethod::element)
        .filter(ElementPredicates::isAbstract)
        .forEach(m -> forwardToMounts(m,
            CompositionInfo::provisionMethods,
            annotator,
            sourceInfo.mountMethods()));
    sourceInfo.provisionMethods()
        .reject(m -> ElementPredicates.isAbstract(m.element()))
        .forEach(m -> {
          MethodSpec.Builder overriding = MethodSpec.overriding(m.element());
          annotator.accept(m, overriding);
          target.addMethod(overriding
              .addCode("return super.$L();\n", m.element().getSimpleName())
              .build());
        });
  }

  private void generateMountFields() {
    sourceInfo.mountMethods().forEach(t ->
        target.addField(FieldSpec.builder(TypeName.get(t.element().getReturnType()), t.memberName())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $L.$L()", targetClassName.simpleName(), t.className())
            .build()));
  }

  private void generateMountMethods() {
    sourceInfo.mountMethods().forEach(t ->
        target.addMethod(MethodSpec.overriding(t.element())
            .addAnnotation(Mount.class)
            .addCode("return $L;\n", t.memberName())
            .build()));
  }

  private Seq<TypeSpec.Builder> generateMountClasses() {
    return sourceInfo.mountMethods()
        .map(t -> {
          TypeSpec.Builder builder = TypeSpec.classBuilder(t.className())
              .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
              .superclass(TypeName.get(t.element().getReturnType()))
              .addModifiers();
          CompositionInfo info = env.compositionInfoFor(t.typeElement());
          forwardToAssembly(builder, info.provisionMethods());
          forwardToAssembly(builder, info.mountMethods());
          return builder;
        });
  }

  private <T extends ModelElement.OfExecutable> void forwardToAssembly(TypeSpec.Builder builder, Traversable<? extends T> methods) {
    methods.map(ModelElement.OfExecutable::element)
        .filter(ElementPredicates::isAbstract)
        .forEach(m ->
            builder.addMethod(MethodSpec.overriding((ExecutableElement) m)
                .addCode("return $L.this.$L();\n", targetClassName.simpleName(), m.getSimpleName())
                .build()));
  }



  private <C extends ModelElementConfig, E extends ModelElement.OfExecutable<C>> void forwardToMounts(
      ExecutableElement method,
      Function<? super CompositionInfo, ? extends Traversable<? extends E>> type,
      BiConsumer<E, ? super MethodSpec.Builder> methodCustomiser,
      Traversable<? extends MountMethod> mountMethods) {
    var candidates = mountMethods
        .flatMap(tm -> type.apply(env.compositionInfoFor(tm.typeElement()))
                .reject(m -> ElementPredicates.isAbstract(m.element()))
                .filter(m -> m.element().getSimpleName().equals(method.getSimpleName()))
                .map(m -> Tuple.of(tm, m)));
    if (candidates.size() == 0) {
      env.problems().error(sourceType, "No suitable implementation found for " + method);
      ON_DEVEL_MODE.accept(() ->
          generateStubForward(target, method, "No suitable implementation"));
    } else if (candidates.size() > 1) {
      env.problems().error(sourceType, "Multiple suitable implementations found for " + method + ": "
          + candidates.map(Tuple2::_2).mkString(", "));
      ON_DEVEL_MODE.accept(() ->
          generateStubForward(target, method, "Multiple suitable implementations: " + candidates.mkString(", ")));
    } else {
      var methodBuilder = MethodSpec.overriding(method);
      methodCustomiser.accept(candidates.head()._2, methodBuilder);
      methodBuilder.addCode("return $L.$L();\n",
          candidates.head()._1.memberName(), candidates.head()._2.element().getSimpleName());
      target.addMethod(methodBuilder.build());
    }
  }

  private void generateStubForward(TypeSpec.Builder builder, ExecutableElement method, String reason) {
    builder.addMethod(MethodSpec.overriding(method)
        .addAnnotation(Provision.class)
        .addCode("// " + reason + "\nreturn null;\n")
        .build());
  }
}
