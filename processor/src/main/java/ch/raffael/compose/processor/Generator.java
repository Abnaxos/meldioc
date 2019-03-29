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
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.model.CompositionTypeModel;
import ch.raffael.compose.processor.model.ModelElement;
import ch.raffael.compose.processor.model.MountMethod;
import ch.raffael.compose.processor.model.ProvisionMethod;
import ch.raffael.compose.processor.util.Elements;
import ch.raffael.compose.runtime.CompositionException;
import ch.raffael.compose.tooling.model.AbstractProvisionConfig;
import ch.raffael.compose.tooling.model.AssemblyConfig;
import ch.raffael.compose.tooling.model.ClassRef;
import ch.raffael.compose.tooling.model.ModelElementConfig;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static ch.raffael.compose.processor.Debug.DEVEL_MODE;
import static ch.raffael.compose.processor.Debug.ON_DEVEL_MODE;
import static ch.raffael.compose.processor.util.Elements.toDeclaredType;

/**
 * @since 2019-03-23
 */
public class Generator {

  public static final String CONFIG_FIELD_NAME = "$config";
  public static final ClassName CONFIG_CLASS_NAME = ClassName.get("com.typesafe.config", "Config");

  private final Instant timestamp = Instant.now();
  private final Environment env;
  private final TypeElement sourceElement;
  private final DeclaredType sourceType;
  private final CompositionTypeModel sourceModel;

  private final AssemblyConfig<AnnotationMirror> assemblyConfig;
  private final ClassName targetClassName;
  private final TypeSpec.Builder targetBuilder;

  Generator(Environment env, TypeElement sourceElement) {
    this.env = env;
    this.sourceElement = sourceElement;
    this.sourceType = (DeclaredType) sourceElement.asType();
    assemblyConfig = env.adaptors().findConfig(sourceElement, env.adaptors()::assemblyConfigOf)
        .orElseThrow(() -> new InternalErrorException(sourceElement + " not annotated with " + Assembly.class.getSimpleName()));
    ClassRef targetRef = assemblyConfig.assemblyClassRef(
        env.elements().getPackageOf(sourceElement).getQualifiedName().toString(), sourceElement.getSimpleName().toString());
    var validator = env.problems().validator(sourceElement, assemblyConfig.source());
    targetClassName = ClassName.get(
        validator.validJavaPackageName(targetRef.packageName())
            .substituteOnError(targetRef.packageName(), "$invalid$"),
        validator.validIdentifier(targetRef.className(), "class name")
            .substituteOnError(targetRef.className(), "$Invalid$"));
    targetBuilder = TypeSpec.classBuilder(targetClassName);
    sourceModel = env.compositionTypeModelPool().modelOf(toDeclaredType(sourceElement.asType()));
  }

  TypeElement sourceElement() {
    return sourceElement;
  }

  public DeclaredType sourceType() {
    return sourceType;
  }

  String targetClassName() {
    return targetClassName.toString();
  }

  String generate() {
    targetBuilder.addAnnotation(AnnotationSpec.builder(Generated.class)
        .addMember("timestamp", "$S", timestamp.atZone(ZoneId.systemDefault().normalized()).toString())
        .addMember("version", "$S", "TODO")
        .build());
    targetBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "all")
        .build());
    if (!Debug.FAILSAFE_GEN) {
      targetBuilder.superclass(TypeName.get(sourceElement.asType()));
      new AssemblyClassValidator(env).validateAll(sourceElement, targetClassName.packageName());
      if (!assemblyConfig.packageLocal()) {
        targetBuilder.addModifiers(Modifier.PUBLIC);
      }
      if (!Debug.DEVEL_MODE) {
        targetBuilder.addModifiers(Modifier.FINAL);
      }
      generateMembers();
    }
    var fileBuilder = JavaFile.builder(targetClassName.packageName(), targetBuilder.build())
        .addFileComment("Generated by ch.raffael.compose, " + new Date(timestamp.toEpochMilli()));
    if (DEVEL_MODE) {
      fileBuilder.addFileComment("\n\nDevelopment mode enabled:\n"
          + " - the class is not final\n"
          + " - some usually private members are exposed as package local\n"
          + "\n"
          + "This enables extending the class to amend things manually that aren't generated yet");
    }
    return fileBuilder.build().toString();
  }

  private void generateMembers() {
    targetBuilder.addField(CONFIG_CLASS_NAME, CONFIG_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL);
    generateConstructor();
    var mounts = generateMountClasses();
    generateProvisionMethods();
    generateMountFields();
    generateMountMethods();
    mounts.map(TypeSpec.Builder::build).forEach(targetBuilder::addType);
  }

  private void generateConstructor() {
    var code = CodeBlock.builder()
        .addStatement("$L", "this." + CONFIG_FIELD_NAME + " = " + CONFIG_FIELD_NAME);
    if (sourceModel.composeMethods().nonEmpty()) {
      code.beginControlFlow("try");
      code.addStatement("System.out.println($S)", "Calling the compose methods");
      code.endControlFlow();
      code.beginControlFlow("catch ($T | $T e)", RuntimeException.class, Error.class);
      code.addStatement("throw e");
      code.endControlFlow();
      code.beginControlFlow("catch ($T e)", Exception.class);
      code.beginControlFlow("if (e instanceof $T)", CompositionException.class);
      code.addStatement("throw e");
      code.endControlFlow();
      code.beginControlFlow("else");
      code.addStatement("throw new $T(e)", CompositionException.class);
      code.endControlFlow();
      code.endControlFlow();
    }
    targetBuilder.addMethod(MethodSpec.constructorBuilder()
        .addException(CompositionException.class)
        .addParameter(CONFIG_CLASS_NAME, CONFIG_FIELD_NAME)
        .addCode(code.build())
        .build());
  }

  private void generateProvisionMethods() {
    BiConsumer<ProvisionMethod, MethodSpec.Builder> annotator = (c, m) -> m.addAnnotation(Provision.class);
    sourceModel.provisionMethods()
        .map(ProvisionMethod::element)
        .filter(Elements::isAbstract)
        .forEach(m -> forwardToMounts(m,
            CompositionTypeModel::provisionMethods,
            annotator,
            sourceModel.mountMethods()));
    sourceModel.provisionMethods()
        .reject(m -> Elements.isAbstract(m.element()))
        .forEach(m -> {
          MethodSpec.Builder overriding = MethodSpec.overriding(m.element());
          annotator.accept(m, overriding);
          targetBuilder.addMethod(overriding
              .addCode("return super.$L();\n", m.element().getSimpleName())
              .build());
        });
  }

  private void generateMountFields() {
    sourceModel.mountMethods().forEach(t -> {
      var n = ClassName.get(targetClassName.packageName(), targetClassName.simpleName(), t.className());
      targetBuilder.addField(FieldSpec.builder(n, t.memberName())
          .addModifiers(localOnDevel(Modifier.FINAL))
          .initializer("new $T()", n)
          .build());
    });
  }

  private void generateMountMethods() {
    sourceModel.mountMethods().forEach(t ->
        targetBuilder.addMethod(MethodSpec.overriding(t.element())
            .addAnnotation(Module.Mount.class)
            .addCode("return $L;\n", t.memberName())
            .build()));
  }

  private Seq<TypeSpec.Builder> generateMountClasses() {
    return sourceModel.mountMethods()
        .filter(m -> Elements.isAbstract(m.element()))
        .map(m -> {
          TypeSpec.Builder builder = TypeSpec.classBuilder(m.className())
              .addModifiers(localOnDevel(Modifier.FINAL))
              .superclass(TypeName.get(m.element().getReturnType()))
              .addModifiers();
          CompositionTypeModel info = env.compositionTypeModelPool().modelOf(m.typeElement());
          forwardToAssembly(builder, info.provisionMethods());
          forwardToAssembly(builder, info.mountMethods());
          forwardToAssembly(builder, info.extensionPointProvisionMethods());
          provisions(builder, info.provisionMethods());
          provisions(builder, info.extensionPointProvisionMethods());
          return builder;
        });
  }

  private <T extends ModelElement.OfExecutable> void forwardToAssembly(TypeSpec.Builder builder, Traversable<? extends T> methods) {
    methods.filter(m -> Elements.isAbstract(m.element()))
        .forEach(m ->
            builder.addMethod(MethodSpec.overriding((ExecutableElement) m.element())
                .addAnnotation(m.config().type().annotationType())
                .addCode("return $L.this.$L();\n", targetClassName.simpleName(), m.element().getSimpleName())
                .build()));
  }

  private void provisions(TypeSpec.Builder builder, Traversable<? extends ModelElement.OfExecutable<? extends AbstractProvisionConfig>> methods) {
    methods.reject(m -> Elements.isAbstract(m.element()))
        .forEach(m ->
            builder
                .addField(FieldSpec.builder(ParameterizedTypeName.get(
                    env.types().getDeclaredType((TypeElement) env.known().rtProvision().asElement(), m.element().getReturnType())),
                    m.memberName())
                    .addModifiers(localOnDevel(Modifier.FINAL))
                    .initializer("\n    $T.$L($T.class, $S, () -> super.$L())", env.types().erasure(env.known().rtProvision()),
                        m.config().provisionMethodName(), env.types().erasure(m.element().getEnclosingElement().asType()),
                        m.element().getSimpleName(), m.element().getSimpleName())
                    .build())
                .addMethod(MethodSpec.overriding(m.element())
                    .addAnnotation(m.config().type().annotationType())
                    .addCode("return this.$L.get();\n", m.memberName())
                    .build())
                .build());
  }


  private <C extends ModelElementConfig, E extends ModelElement.OfExecutable<C>> void forwardToMounts(
      ExecutableElement method,
      Function<? super CompositionTypeModel, ? extends Traversable<? extends E>> type,
      BiConsumer<E, ? super MethodSpec.Builder> methodCustomiser,
      Traversable<? extends MountMethod> mountMethods) {
    var candidates = mountMethods
        .flatMap(tm -> type.apply(env.compositionTypeModelPool().modelOf((DeclaredType) tm.typeElement().asType()))
                .reject(m -> Elements.isAbstract(m.element()))
                .filter(m -> m.element().getSimpleName().equals(method.getSimpleName()))
                .map(m -> Tuple.of(tm, m)));
    if (candidates.size() == 0) {
      env.problems().error(sourceElement, "No suitable implementation found for " + method);
      ON_DEVEL_MODE.accept(() ->
          generateStubForward(targetBuilder, method, "No suitable implementation"));
    } else if (candidates.size() > 1) {
      env.problems().error(sourceElement, "Multiple suitable implementations found for " + method + ": "
          + candidates.map(Tuple2::_2).mkString(", "));
      ON_DEVEL_MODE.accept(() ->
          generateStubForward(targetBuilder, method, "Multiple suitable implementations: " + candidates.mkString(", ")));
    } else {
      var methodBuilder = MethodSpec.overriding(method);
      methodCustomiser.accept(candidates.head()._2, methodBuilder);
      methodBuilder.addCode("return $L.$L();\n",
          candidates.head()._1.memberName(), candidates.head()._2.element().getSimpleName());
      targetBuilder.addMethod(methodBuilder.build());
    }
  }

  private void generateStubForward(TypeSpec.Builder builder, ExecutableElement method, String reason) {
    builder.addMethod(MethodSpec.overriding(method)
        .addAnnotation(Provision.class)
        .addCode("// " + reason + "\nreturn null;\n")
        .build());
  }

  private Modifier[] localOnDevel(Modifier... modifiers) {
    if (DEVEL_MODE) {
      return modifiers;
    } else {
      Modifier[] newModifiers = new Modifier[modifiers.length + 1];
      newModifiers[0] = Modifier.PRIVATE;
      System.arraycopy(modifiers, 0, newModifiers, 1, modifiers.length);
      return newModifiers;
    }
  }

}
