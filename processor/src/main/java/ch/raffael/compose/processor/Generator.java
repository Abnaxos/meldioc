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
import ch.raffael.compose.Configuration;
import ch.raffael.compose.ExtensionPoint;
import ch.raffael.compose.Module;
import ch.raffael.compose.Provision;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.env.KnownElements;
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
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static ch.raffael.compose.processor.Debug.DEVEL_MODE;
import static ch.raffael.compose.processor.Debug.ON_DEVEL_MODE;
import static ch.raffael.compose.processor.util.Elements.asDeclaredType;
import static ch.raffael.compose.processor.util.Elements.asTypeElement;
import static io.vavr.API.*;

/**
 * TODO javadoc
 */
public class Generator {

  private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> GENERATED_ANNOTATIONS_MAP =
      Map(Provision.class, Generated.Provision.class,
          Module.Mount.class, Generated.Mount.class,
          ExtensionPoint.Provision.class, Generated.ExtensionPointApiProvision.class,
          Configuration.class, Generated.Configuration.class);

  public static final String CONFIG_FIELD_NAME = "config";

  public static final String DISPATCHER_CLASS_NAME = "Dispatcher";
  public static final String DISPATCHER_FIELD_NAME = "dispatcher";
  public static final String BUILDER_CLASS_NAME = "Builder";
  public static final String BUILDER_METHOD_NAME = "builder";
  public static final String BUILD_ASSEMBLY_METHOD_NAME = "buildAssembly";
  public static final String NEW_DISPATCHER_METHOD = "$newDispatcher";
  public static final String COMPOSE_METHOD = "$compose";
  public static final String BUILD_NEW_SHELL_METHOD = "$newShell";

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

  private Seq<Tuple2<ClassName, String>> shellFields = Seq(Tuple(KnownElements.CONFIG_TYPE, CONFIG_FIELD_NAME));

  Generator(Environment env, TypeElement sourceElement) {
    this.env = env;
    this.sourceElement = sourceElement;
    this.sourceType = (DeclaredType) sourceElement.asType();
    assemblyConfig = env.adaptors().findConfig(sourceElement, env.adaptors()::assemblyConfigOf)
        .getOrElseThrow(() -> new IllegalStateException(sourceElement + " not annotated with " + Assembly.class.getSimpleName()));
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
    dispatcherClassName = shellClassName.nestedClass(DISPATCHER_CLASS_NAME);
    dispatcherBuilder = TypeSpec.classBuilder(dispatcherClassName);
    sourceModel = env.compositionTypeModels().modelOf(asDeclaredType(sourceElement.asType()));
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
        .addMember(Generated.TIMESTAMP_ATTR, "$S",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(ZoneId.systemDefault())))
        .addMember(Generated.VERSION_ATTR, "$S", "PROTO")
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
        .addCode(CodeBlock.builder()
            .addStatement("return new $T()", dispatcherClassName)
            .build())
        .build());
    generateComposeMethod(shellBuilder);
    generateBuilder();
    generateDispatcher();

    shellBuilder.addType(dispatcherBuilder.build());
    mounts.map(TypeSpec.Builder::build).forEach(shellBuilder::addType);

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
      builder.addStatement("this.$L = $T.requireNonNull($L, $S)", n, Objects.class, n, n + " is null");
      return null;
    }));
    shellFields.find(f -> f._2.equals(CONFIG_FIELD_NAME)).forEach((tp) ->
        builder.addStatement("if (!this.$L.isResolved()) throw new $T($S)",
            CONFIG_FIELD_NAME, KnownElements.CONFIG_NOT_RESOLVED_EXCEPTION_TYPE,
            "Configuration has not been resolved, you need to call Config#resolve(),"
                + " see API docs for Config#resolve()"));
    code.addStatement("$L = $L()", DISPATCHER_FIELD_NAME, NEW_DISPATCHER_METHOD);
    code.addStatement("$L()", COMPOSE_METHOD);
    builder.addCode(code.build());
    return builder.build();
  }

  private void generateBuilder() {
    var builder = TypeSpec.classBuilder(builderClassName);
    builder.addModifiers(Modifier.STATIC);
    builder.addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL));
    builder.addModifiers(conditionalModifiers(!assemblyConfig.packageLocal(), Modifier.PUBLIC));
    shellFields.forEach(f -> builder.addField(f._1, f._2, conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE)));
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
    shellFields.forEach(f -> f.apply((t, n) -> builder.addMethod(MethodSpec.methodBuilder(n)
        .addModifiers(conditionalModifiers(!assemblyConfig.packageLocal(), Modifier.PUBLIC))
        .addParameter(t, n)
        .returns(builderClassName)
        .addCode(CodeBlock.builder()
            .addStatement("this.$L = $L", n, n)
            .addStatement("return this")
            .build())
        .build())));
    MethodSpec.Builder build = MethodSpec.methodBuilder(BUILD_ASSEMBLY_METHOD_NAME)
        .addModifiers(conditionalModifiers(!assemblyConfig.packageLocal(), Modifier.PUBLIC))
        .addException(CompositionException.class)
        .returns(ClassName.get(sourceType));
    shellFields
        .map(Tuple2::_2)
        .forEach(f -> build.addStatement("if ($L == null) throw new $T($S)",
            f, IllegalStateException.class, f + " is not set"));
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
      generateDispatcherMembers(dispatcherBuilder);
    }
  }

  private void generateDispatcherMembers(TypeSpec.Builder builder) {
    generateDispatcherConstructor(builder);
    generateProvisionMethods(builder);
    generateMountFields(builder);
    generateMountMethods(builder);
    generateConfigurationsMethods(builder, sourceModel);
  }

  private void generateDispatcherConstructor(TypeSpec.Builder builder) {
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
  }

  private void generateComposeMethod(TypeSpec.Builder builder) {
    var code = CodeBlock.builder();
    if (sourceModel.composeMethods().nonEmpty()) {
      code.beginControlFlow("try");
      sourceModel.composeMethods().forEach(m -> {
        Seq<Tuple2<String, ? extends Seq<?>>> args =
            ((ExecutableType) env.types().asMemberOf(sourceType, m.element())).getParameterTypes().stream()
                .map(pt -> {
                  var candidates = sourceModel.allExtensionPointProvisionMethods()
                      .filter(epm ->
                          env.types().isSubtype(
                              ((ExecutableType) epm._1.element().asType()).getReturnType(), pt))
                      .collect(List.collector());
                  if (env.known().config().map(ct -> env.types().isSubtype(ct, pt)).getOrElse(false)) {
                    // TODO (2019-03-30) better error messages
                    // config
                    if (!candidates.isEmpty()) {
                      env.problems().error(m.element(),
                          "Multiple extension points (including configuration) match type " + pt);
                      return Tuple("?ambiguous", Seq());
                    } else {
                      return Tuple("$T.this.$L", Seq(shellClassName, CONFIG_FIELD_NAME));
                    }
                  } else {
                    if ( candidates.isEmpty()) {
                      env.problems().error(m.element(), "No extension point matches type " + pt);
                      return  Tuple("?unmapped", Seq());
                    } else if (candidates.size() > 1) {
                      env.problems().error(m.element(), "Multiple extension points match type " + pt);
                      return Tuple("?ambiguous", Seq());
                    } else {
                      var epm = candidates.head()._1;
                      var mount = candidates.head()._2;
                      return mount
                          .map(mm -> Tuple("$T.this.$L.$L.$L()", Seq(shellClassName, DISPATCHER_FIELD_NAME,
                              mm.memberName(), epm.element().getSimpleName())))
                          .getOrElse(() -> Tuple("$T.this.$L.$L()", Seq(shellClassName, DISPATCHER_FIELD_NAME,
                              epm.element().getSimpleName())));
                    }
                  }
                })
            .collect(List.collector());
        code.addStatement("this.$L.$L(" + args.map(Tuple2::_1).mkString(", ") + ")",
            (Object[])Seq((Object) DISPATCHER_FIELD_NAME, m.element().getSimpleName())
                .appendAll(args.flatMap(Tuple2::_2))
                .toJavaArray());
      });
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
    builder.addMethod(MethodSpec.methodBuilder(COMPOSE_METHOD)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .addException(CompositionException.class)
        .addCode(code.build())
        .build());
  }

  private void generateProvisionMethods(TypeSpec.Builder builder) {
    BiConsumer<ProvisionMethod, MethodSpec.Builder> annotator = (c, m) ->
        m.addAnnotation(generatedAnnotation(c));
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
          MethodSpec.Builder overriding = MethodSpec.overriding(m.element(), sourceType, env.types());
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
          .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL))
          .initializer("new $T()", n)
          .build());
    });
  }

  private void generateMountMethods(TypeSpec.Builder builder) {
    sourceModel.mountMethods().forEach(t ->
        builder.addMethod(MethodSpec.overriding(t.element(), sourceType, env.types())
            .addAnnotation(generatedAnnotation(t))
            .addCode("return this.$L;\n", t.memberName())
            .build()));
  }

  private Seq<TypeSpec.Builder> generateMountClasses() {
    return sourceModel.mountMethods()
        .filter(m -> Elements.isAbstract(m.element()))
        .map(m -> {
          TypeSpec.Builder builder = TypeSpec.classBuilder(m.className())
              .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL))
              .superclass(TypeName.get(m.element().getReturnType()))
              .addModifiers();
          CompositionTypeModel model = env.compositionTypeModels()
              .modelOf((DeclaredType) env.types().asMemberOf(sourceModel.type(),
                  asTypeElement(asDeclaredType(m.element().getReturnType()).asElement())));
          forwardToDispatcher(builder, model.provisionMethods());
          forwardToDispatcher(builder, model.mountMethods());
          forwardToDispatcher(builder, model.extensionPointProvisionMethods());
          provisions(builder, model.provisionMethods());
          provisions(builder, model.extensionPointProvisionMethods());
          generateConfigurationsMethods(builder, model);
          return builder;
        });
  }

  private <T extends ModelElement.OfExecutable> void forwardToDispatcher(
      TypeSpec.Builder builder, Traversable<? extends T> methods) {
    methods.filter(m -> Elements.isAbstract(m.element()))
        .forEach(m ->
            builder.addMethod(MethodSpec.overriding((ExecutableElement) m.element(), m.enclosing().type(), env.types())
                .addAnnotation(generatedAnnotation(m))
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
                    .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL))
                    .initializer("\n    $T.$L($T.class, $S, () -> super.$L())", env.types().erasure(env.known().rtProvision()),
                        m.config().provisionMethodName(), env.types().erasure(m.element().getEnclosingElement().asType()),
                        m.element().getSimpleName(), m.element().getSimpleName())
                    .build())
                .addMethod(MethodSpec.overriding(m.element(), m.enclosing().type(), env.types())
                    .addAnnotation(generatedAnnotation(m))
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
        .flatMap(mount -> type.apply(env.compositionTypeModels().modelOf(
            (DeclaredType) asTypeElement(asDeclaredType(mount.element().getReturnType()).asElement()).asType()))
            .reject(m -> Elements.isAbstract(m.element()))
            .filter(m -> m.element().getSimpleName().equals(method.getSimpleName()))
            .map(m -> Tuple.of(mount, m)));
    if (candidates.size() == 0) {
      env.problems().error(sourceElement, "No suitable implementation found for " + method);
      ON_DEVEL_MODE.accept(() ->
          generateErrorForward(builder, method, "No suitable implementation"));
    } else if (candidates.size() > 1) {
      env.problems().error(sourceElement, "Multiple suitable implementations found for " + method + ": "
          + candidates.map(Tuple2::_2).mkString(", "));
      ON_DEVEL_MODE.accept(() ->
          generateErrorForward(builder, method, "Multiple suitable implementations: " + candidates.mkString(", ")));
    } else {
      candidates.head().apply((mount, fwd) -> {
        var methodBuilder = MethodSpec.overriding(method, mount.enclosing().type(), env.types());
        methodCustomiser.accept(fwd, methodBuilder);
        methodBuilder.addCode("return this.$L.$L();\n",
            mount.memberName(), fwd.element().getSimpleName());
        builder.addMethod(methodBuilder.build());
        return null;
      });
    }
  }

  private void generateErrorForward(TypeSpec.Builder builder, ExecutableElement method, String reason) {
    builder.addMethod(MethodSpec.overriding(method)
        .addAnnotation(Provision.class)
        .addCode("// " + reason + "\nreturn null;\n")
        .build());
  }

  private void generateConfigurationsMethods(TypeSpec.Builder builder, CompositionTypeModel model) {
    model.configurationMethods().forEach(confMethod -> {
      Types types = env.types();
      var fullType = ((ExecutableType) types.asMemberOf(model.type(), confMethod.element())).getReturnType();
      final TypeMirror type;
      var list = false;
      if (types.isSubtype(fullType, types.erasure(env.known().iterable()))) {
        var iteratorType = ((ExecutableType) types.asMemberOf((DeclaredType)fullType, env.known().iterableIterator()))
            .getReturnType();
        type = ((ExecutableType) types.asMemberOf((DeclaredType) iteratorType, env.known().iteratorNext()))
            .getReturnType();
        list = true;
      } else {
        type = fullType;
      }
      // TODO (2019-03-31) ? special meaning of types: getBytes()->MemSize, getDuration(with unit), deprecated millis/nanos
      // TODO (2019-03-31) "internals"? -> ConfigObject, ConfigValue, ConfigOrigin; use Config and continue from there?
      String getter;
      Option<Tuple2<String, Seq<?>>> argsPattern = None();
      if (isPrimitive(TypeKind.INT, type)) {
        getter = "getInt";
      } else if (isPrimitive(TypeKind.LONG, type)) {
        getter = "getLong";
//      } else if (isPrimitive(TypeKind.SHORT, type)) {
//        getter = "(short)getInt";
//      } else if (isPrimitive(TypeKind.BYTE, type)) {
//
      } else if (isPrimitive(TypeKind.DOUBLE, type)) {
        getter = "getDouble";
//      } else if (isPrimitive(TypeKind.FLOAT, type)) {
//
      } else if (isPrimitive(TypeKind.BOOLEAN, type)) {
        getter = "getBoolean";
//      } else if (isPrimitive(TypeKind.CHAR, type)) {
//        getter = "get"
      } else if (isApplicable(type, env.known().number())) {
        getter = "getNumber";
      } else if (isApplicable(type, env.known().string(), env.known().charSequence())) {
        getter = "getString";
      } else if (isApplicable(type, env.known().duration())) {
        getter = "getDuration";
      } else if (isApplicable(type, env.known().period())) {
        getter = "getPeriod";
      } else if (isApplicable(type, env.known().temporalAmount())) {
        getter = "getTemporal";
      } else if (env.known().config().map(t -> isApplicable(type, t)).getOrElse(false)) {
        getter = "getConfig";
      } else if (env.known().configMemorySize().map(t -> isApplicable(type, t)).getOrElse(false)) {
        getter = "getMemorySize";
      } else if (isApplicable(type, env.known().enumeration())
          && !(types.isSameType(types.erasure(env.known().enumeration()), types.erasure(type)))) {
        getter = "getEnum";
        argsPattern = Some(Tuple("$T.class, $S", Seq(type, confMethod.fullPath())));
      } else if (isApplicable(type, env.known().object())) {
        getter = "getAnyRef";
      } else {
        env.problems().error(sourceElement, "Cannot map configuration method " + confMethod.element());
        return;
      }
      var methodBuilder = MethodSpec.overriding(confMethod.element(), model.type(), types);
      methodBuilder.addAnnotation(generatedAnnotation(confMethod));
      if (confMethod.hasDefault()) {
        methodBuilder.addStatement("if (!$T.this.$L.hasPath($S)) return super.$L()",
            shellClassName, CONFIG_FIELD_NAME, confMethod.fullPath(), confMethod.element().getSimpleName());
      }
      if (list) {
        getter = getter + "List";
      }
      argsPattern = Some(argsPattern.getOrElse(() -> Tuple("$S", Seq(confMethod.fullPath()))));
      if (list) {
        argsPattern = argsPattern.map(t -> t.map1(m -> m + "List"));
      }
      methodBuilder.addStatement("return $T.this.$L.$L(" + argsPattern.get()._1 + ")",
          Seq((Object)shellClassName, CONFIG_FIELD_NAME, getter).appendAll(argsPattern.get()._2).toJavaArray());
      builder.addMethod(methodBuilder.build());
    });
  }

  private AnnotationSpec generatedAnnotation(Class<? extends Annotation> annotationType, Element source) {
    return AnnotationSpec.builder(annotationType)
        .addMember(Generated.SOURCE_CLASS_ATTR, "$T.class", source.getEnclosingElement())
        .addMember(Generated.SOURCE_MEMBER_ATTR, "$S", source.getSimpleName())
        .build();
  }

  private AnnotationSpec generatedAnnotation(ModelElement element) {
    return AnnotationSpec.builder(GENERATED_ANNOTATIONS_MAP.get(element.config().type().annotationType()).get())
        .addMember(Generated.SOURCE_CLASS_ATTR, "$T.class", element.element().getEnclosingElement())
        .addMember(Generated.SOURCE_MEMBER_ATTR, "$S", element.element())
        .build();
  }

  private boolean isPrimitive(TypeKind kind, TypeMirror type) {
    if (type instanceof PrimitiveType) {
      return type.getKind() == kind;
    } else {
      return env.types().isSubtype(type, env.types().boxedClass(env.types().getPrimitiveType(kind)).asType());
    }
  }

  private boolean isApplicable(TypeMirror type, TypeMirror to, @Nullable TypeMirror... more) {
    if (env.types().isSubtype(type, to)) {
      return true;
    }
    if (more != null && more.length > 0) {
      for (var t : more) {
        if (env.types().isSubtype(type, t)) {
          return true;
        }
      }
    }
    return false;
  }

  private Modifier[] conditionalModifiers(boolean condition, Modifier... modifiers) {
    //noinspection ZeroLengthArrayAllocation
    return condition ? modifiers : new Modifier[0];
  }

}
