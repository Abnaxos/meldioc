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

import javax.annotation.Nonnull;
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
import static io.vavr.API.*;

/**
 * @since 2019-03-23
 */
public class Generator {

  public static final String CONFIG_FIELD_NAME = "config";
  public static final ClassName CONFIG_TYPE = ClassName.get("com.typesafe.config", "Config");
  public static final String BUILDER_CLASS_NAME = "Builder";
  public static final String BUILDER_METHOD_NAME = "builder";
  public static final String BUILD_ASSEMBLY_METHOD_NAME = "buildAssembly";
  public static final String BUILD_NEW_SHELL_METHOD = "$newShell";
  public static final String DISPATCHER_FIELD_NAME = "$dispatcher";
  public static final String NEW_DISPATCHER_METHOD = "$newDispatcher";

  private final Instant timestamp = Instant.now();
  private final Environment env;
  private final TypeElement sourceElement;
  private final DeclaredType sourceType;
  private final CompositionTypeModel sourceModel;

  private final AssemblyConfig<AnnotationMirror> assemblyConfig;
  private final ClassName shellClassName;
  private final TypeSpec.Builder shellBuilder;
  private final ClassName builderClassName;
  private final ClassName dispatcherClassName;
  private final TypeSpec.Builder dispatcherBuilder;

  private Seq<Tuple2<ClassName, String>> shellFields = Seq(Tuple(CONFIG_TYPE, CONFIG_FIELD_NAME));

  Generator(Environment env, TypeElement sourceElement) {
    this.env = env;
    this.sourceElement = sourceElement;
    this.sourceType = (DeclaredType) sourceElement.asType();
    assemblyConfig = env.adaptors().findConfig(sourceElement, env.adaptors()::assemblyConfigOf)
        .orElseThrow(() -> new InternalErrorException(sourceElement + " not annotated with " + Assembly.class.getSimpleName()));
    ClassRef targetRef = assemblyConfig.shellClassRef(
        env.elements().getPackageOf(sourceElement).getQualifiedName().toString(), sourceElement.getSimpleName().toString());
    var validator = env.problems().validator(sourceElement, assemblyConfig.source());
    shellClassName = ClassName.get(
        validator.validJavaPackageName(targetRef.packageName())
            .substituteOnError(targetRef.packageName(), "$invalid$"),
        validator.validIdentifier(targetRef.className(), "class name")
            .substituteOnError(targetRef.className(), "$Invalid$"));
    shellBuilder = TypeSpec.classBuilder(shellClassName);
    builderClassName = shellClassName.nestedClass(BUILDER_CLASS_NAME);
    dispatcherClassName = shellClassName.nestedClass("$Dispatcher");
    dispatcherBuilder = TypeSpec.classBuilder(dispatcherClassName);
    sourceModel = env.compositionTypeModelPool().modelOf(toDeclaredType(sourceElement.asType()));
  }

  TypeElement sourceElement() {
    return sourceElement;
  }

  public DeclaredType sourceType() {
    return sourceType;
  }

  String targetClassName() {
    return shellClassName.toString();
  }

  String generate() {
    if (!DEVEL_MODE) {
      shellBuilder.addModifiers(Modifier.FINAL);
    }
    if (!assemblyConfig.packageLocal()) {
      shellBuilder.addModifiers(Modifier.PUBLIC);
    }
    shellBuilder.addAnnotation(AnnotationSpec.builder(Generated.class)
        .addMember("timestamp", "$S", timestamp.atZone(ZoneId.systemDefault().normalized()).toString())
        .addMember("version", "$S", "TODO")
        .build());
    shellBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "all")
        .build());
    var mounts = generateMountClasses();

    shellFields.forEach(tuple -> tuple.apply((t, n) -> shellBuilder.
        addField(FieldSpec.builder(t, n, Modifier.FINAL)
            .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
            .build())));
    shellBuilder.addField(FieldSpec.builder(dispatcherClassName, DISPATCHER_FIELD_NAME, Modifier.FINAL)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
    shellBuilder.addMethod(generateShellConstructor());
    shellBuilder.addMethod(MethodSpec.methodBuilder(BUILDER_METHOD_NAME)
        .addModifiers(Modifier.STATIC)
        .addModifiers(conditionalModifiers(!assemblyConfig.packageLocal(), Modifier.PUBLIC))
        .returns(builderClassName)
        .addCode(CodeBlock.builder().addStatement("return new $T()", builderClassName).build())
        .build());
    shellBuilder.addMethod(MethodSpec.methodBuilder(NEW_DISPATCHER_METHOD)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .returns(dispatcherClassName)
        .addException(CompositionException.class)
        .addCode(CodeBlock.builder()
            .addStatement("return new $T()", dispatcherClassName)
            .build())
        .build());
    generateBuilder();
    generateDispatcher();

    mounts.map(TypeSpec.Builder::build).forEach(shellBuilder::addType);
    shellBuilder.addType(dispatcherBuilder.build());

    var fileBuilder = JavaFile.builder(shellClassName.packageName(), shellBuilder.build())
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

  @Nonnull
  private MethodSpec generateShellConstructor() {
    var builder = MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .addException(CompositionException.class);
    var code = CodeBlock.builder();
    shellFields.forEach(tp -> tp.apply((t, n) -> {
      builder.addParameter(t, n);
      builder.addStatement("this.$L = $L", n, n);
      return null;
    }));
    code.addStatement("$L = $L()", DISPATCHER_FIELD_NAME, NEW_DISPATCHER_METHOD);
    builder.addCode(code.build());
    return builder.build();
  }

  private void generateBuilder() {
    var builder = TypeSpec.classBuilder(builderClassName);
    builder.addModifiers(Modifier.STATIC);
    builder.addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL));
    builder.addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE, Modifier.FINAL));
    shellFields.forEach(f -> builder.addField(f._1, f._2, conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE)));
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
    shellFields.forEach(f -> f.apply((t, n) -> builder.addMethod(MethodSpec.methodBuilder(n)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(t, n)
        .returns(builderClassName)
        .addCode(CodeBlock.builder()
            .addStatement("this.$L = $L", n, n)
            .addStatement("return this")
            .build())
        .build())));
    MethodSpec.Builder build = MethodSpec.methodBuilder(BUILD_ASSEMBLY_METHOD_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addException(CompositionException.class)
        .returns(ClassName.get(sourceType));
    shellFields
        .map(Tuple2::_2)
        .forEach(f -> build.addStatement("if ($L == null) throw new $T($S)", f, IllegalStateException.class, f+" is not set"));
    build.addStatement("return $L().$L", BUILD_NEW_SHELL_METHOD, DISPATCHER_FIELD_NAME);
    builder.addMethod(build.build());
    builder.addMethod(MethodSpec.methodBuilder(BUILD_NEW_SHELL_METHOD)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .addException(CompositionException.class)
        .returns(shellClassName)
        .addCode(CodeBlock.builder()
            .addStatement(shellFields.map(f -> "$L").mkString("return new $T(", ", ", ")"),
                Seq((Object) shellClassName).appendAll(shellFields.map(Tuple2::_2)).toJavaArray())
            .build())
        .build());
    shellBuilder.addType(builder.build());
  }

  void generateDispatcher() {
    if (!Debug.FAILSAFE_GEN) {
//      new AssemblyClassValidator(env).validateAll(sourceElement, dis.packageName());
      dispatcherBuilder.superclass(TypeName.get(sourceElement.asType()))
          .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE, Modifier.FINAL));
      generateMembers(dispatcherBuilder);
    }
  }

  private void generateMembers(TypeSpec.Builder builder) {
    generateConstructor(builder);
    generateProvisionMethods(builder);
    generateMountFields(builder);
    generateMountMethods(builder);
  }

  private void generateConstructor(TypeSpec.Builder builder) {
    var code = CodeBlock.builder();
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
    builder.addMethod(MethodSpec.constructorBuilder()
        .addException(CompositionException.class)
        .addCode(code.build())
        .build());
  }

  private void generateProvisionMethods(TypeSpec.Builder builder) {
    BiConsumer<ProvisionMethod, MethodSpec.Builder> annotator = (c, m) -> m.addAnnotation(Provision.class);
    sourceModel.provisionMethods()
        .map(ProvisionMethod::element)
        .filter(Elements::isAbstract)
        .forEach(m -> forwardToMounts(builder, m,
            CompositionTypeModel::provisionMethods,
            annotator,
            sourceModel.mountMethods()));
    sourceModel.provisionMethods()
        .reject(m -> Elements.isAbstract(m.element()))
        .forEach(m -> {
          MethodSpec.Builder overriding = MethodSpec.overriding(m.element());
          annotator.accept(m, overriding);
          builder.addMethod(overriding
              .addCode("return super.$L();\n", m.element().getSimpleName())
              .build());
        });
  }

  private void generateMountFields(TypeSpec.Builder builder) {
    sourceModel.mountMethods().forEach(t -> {
      var n = shellClassName.nestedClass(t.className());
      builder.addField(FieldSpec.builder(n, t.memberName())
          .addModifiers(localOnDevel(Modifier.FINAL))
          .initializer("new $T()", n)
          .build());
    });
  }

  private void generateMountMethods(TypeSpec.Builder builder) {
    sourceModel.mountMethods().forEach(t ->
        builder.addMethod(MethodSpec.overriding(t.element())
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
          forwardToDispatcher(builder, info.provisionMethods());
          forwardToDispatcher(builder, info.mountMethods());
          forwardToDispatcher(builder, info.extensionPointProvisionMethods());
          provisions(builder, info.provisionMethods());
          provisions(builder, info.extensionPointProvisionMethods());
          return builder;
        });
  }

  private <T extends ModelElement.OfExecutable> void forwardToDispatcher(TypeSpec.Builder builder, Traversable<? extends T> methods) {
    methods.filter(m -> Elements.isAbstract(m.element()))
        .forEach(m ->
            builder.addMethod(MethodSpec.overriding((ExecutableElement) m.element())
                .addAnnotation(m.config().type().annotationType())
                .addCode("return $T.this.$L.$L();\n", shellClassName, DISPATCHER_FIELD_NAME, m.element().getSimpleName())
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
      TypeSpec.Builder builder,
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
          generateStubForward(builder, method, "No suitable implementation"));
    } else if (candidates.size() > 1) {
      env.problems().error(sourceElement, "Multiple suitable implementations found for " + method + ": "
          + candidates.map(Tuple2::_2).mkString(", "));
      ON_DEVEL_MODE.accept(() ->
          generateStubForward(builder, method, "Multiple suitable implementations: " + candidates.mkString(", ")));
    } else {
      var methodBuilder = MethodSpec.overriding(method);
      methodCustomiser.accept(candidates.head()._2, methodBuilder);
      methodBuilder.addCode("return $L.$L();\n",
          candidates.head()._1.memberName(), candidates.head()._2.element().getSimpleName());
      builder.addMethod(methodBuilder.build());
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

  private Modifier[] conditionalModifiers(boolean condition, Modifier... modifiers) {
    //noinspection ZeroLengthArrayAllocation
    return condition ? modifiers : new Modifier[0];
  }

}
