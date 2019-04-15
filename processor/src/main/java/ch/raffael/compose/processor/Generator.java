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

import ch.raffael.compose.$generated.$Provision;
import ch.raffael.compose.Assembly;
import ch.raffael.compose.Configuration;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.model.ClassRef;
import ch.raffael.compose.model.ModelMethod;
import ch.raffael.compose.model.ModelType;
import ch.raffael.compose.model.config.AssemblyConfig;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.env.KnownElements;
import ch.raffael.compose.processor.util.Elements;
import ch.raffael.compose.runtime.CompositionException;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vavr.Tuple3;
import io.vavr.collection.Seq;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import static ch.raffael.compose.processor.Debug.DEVEL_MODE;
import static ch.raffael.compose.processor.util.Elements.asDeclaredType;
import static ch.raffael.compose.processor.util.Elements.asTypeElement;
import static io.vavr.API.*;
import static java.util.function.Function.identity;

/**
 * TODO javadoc
 */
public class Generator {

  public static final String CONFIG_FIELD_NAME = "config";

  public static final String DISPATCHER_CLASS_NAME = "Dispatcher";
  public static final String DISPATCHER_FIELD_NAME = "dispatcher";
  public static final String BUILDER_CLASS_NAME = "Builder";
  public static final String BUILDER_METHOD_NAME = "builder";
  public static final String BUILD_ASSEMBLY_METHOD_NAME = "buildAssembly";
  public static final String NEW_DISPATCHER_METHOD = "$newDispatcher";
  public static final String COMPOSE_METHOD = "$compose";
  public static final String BUILD_NEW_SHELL_METHOD = "$newShell";

  private final Class<?> generatorClass;
  private final Environment env;
  private final TypeElement sourceElement;
  private final DeclaredType sourceType;
  private final ModelType<Element, TypeMirror> sourceModel;

  private final AssemblyConfig<AnnotationMirror> assemblyConfig;
  private final ClassName shellClassName;
  private final TypeSpec.Builder shellBuilder;
  private final ClassName builderClassName;
  private final ClassName dispatcherClassName;
  private final TypeSpec.Builder dispatcherBuilder;

  private Seq<Tuple3<TypeName, String, String>> shellParameters = Seq(Tuple(KnownElements.CONFIG_TYPE, CONFIG_FIELD_NAME, CONFIG_FIELD_NAME));

  Generator(Class<?> generatorClass, Environment env, TypeElement sourceElement) {
    this.generatorClass = generatorClass;
    this.env = env;
    this.sourceElement = sourceElement;
    this.sourceType = (DeclaredType) sourceElement.asType();
    assemblyConfig = env.adaptors().findConfig(sourceElement, env.adaptors()::assemblyConfigOf)
        .getOrElseThrow(() -> new IllegalStateException(sourceElement + " not annotated with " + Assembly.class.getSimpleName()));
    ClassRef targetRef = assemblyConfig.shellClassRef(
        env.elements().getPackageOf(sourceElement).getQualifiedName().toString(), sourceElement.getSimpleName().toString());
    shellClassName = ClassName.get(targetRef.packageName(), targetRef.className());
    shellBuilder = TypeSpec.classBuilder(shellClassName);
    builderClassName = shellClassName.nestedClass(BUILDER_CLASS_NAME);
    dispatcherClassName = shellClassName.nestedClass(DISPATCHER_CLASS_NAME);
    dispatcherBuilder = TypeSpec.classBuilder(dispatcherClassName);
    sourceModel = env.model().modelOf(asDeclaredType(sourceElement.asType()));
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
    Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    if (!DEVEL_MODE) {
      shellBuilder.addModifiers(Modifier.FINAL);
    }
    if (!assemblyConfig.packageLocal()) {
      shellBuilder.addModifiers(Modifier.PUBLIC);
    }
    shellBuilder.addAnnotation(AnnotationSpec.builder(Generated.class)
        .addMember(Generated.TIMESTAMP_ATTR, "$S",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp.atZone(ZoneId.systemDefault())))
        .addMember(Generated.VERSION_ATTR, "$S", "PROTO")
        .build());
    env.known().javaxGenerated().forEach(at -> shellBuilder.addAnnotation(AnnotationSpec.builder(
        ClassName.get(javax.annotation.processing.Generated.class))
        .addMember("value", "$S", generatorClass.getCanonicalName())
        .addMember("date", "$S",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp.atZone(ZoneId.systemDefault())))
        .build()));
    shellBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "all")
        .build());

    var mounts = generateMountClasses();
    sourceModel.mountMethods()
        .filter(m -> !m.element().mountConfig().external())
        .forEach( m -> {
              var className = shellClassName.nestedClass(MemberNames.forMountClass(m.element()));
              shellBuilder.addField(FieldSpec.builder(className, MemberNames.forMount(m.element()),
                  Modifier.PRIVATE, Modifier.FINAL)
                  .initializer("new $T()", className)
                  .build());
            }
        );
    shellParameters = shellParameters.appendAll(sourceModel.mountMethods()
        .filter(m -> m.element().mountConfig().external())
        .map(m -> Tuple(TypeName.get(Elements.asDeclaredType(m.element().type())),
            MemberNames.forMount(m.element()),
            MemberNames.forMount(m.element()))));
    shellParameters.forEach(tuple -> tuple.apply((t, n, _0) -> shellBuilder.
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
    shellParameters.forEach(tp -> tp.apply((t, n, m) -> {
      builder.addParameter(t, n);
      builder.addStatement("this.$L = $T.requireNonNull($L, $S)", n, Objects.class, n, n + " is null");
      return null;
    }));
    shellParameters.find(f -> f._2.equals(CONFIG_FIELD_NAME)).forEach((tp) ->
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
    shellParameters.forEach(f -> builder.addField(f._1, f._2, conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE)));
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
    shellParameters.forEach(f -> f.apply((t, n, bn) -> builder.addMethod(MethodSpec.methodBuilder(bn)
        .addModifiers(conditionalModifiers(!assemblyConfig.packageLocal(), Modifier.PUBLIC))
        .addParameter(t, bn)
        .returns(builderClassName)
        .addCode(CodeBlock.builder()
            .addStatement("this.$L = $L", n, bn)
            .addStatement("return this")
            .build())
        .build())));
    MethodSpec.Builder build = MethodSpec.methodBuilder(BUILD_ASSEMBLY_METHOD_NAME)
        .addModifiers(conditionalModifiers(!assemblyConfig.packageLocal(), Modifier.PUBLIC))
        .addException(CompositionException.class)
        .returns(ClassName.get(sourceType));
    shellParameters
        .map(Tuple3::_2)
        .forEach(f -> build.addStatement("if ($L == null) throw new $T($S)",
            f, IllegalStateException.class, f + " is not set"));
    build.addStatement("return $L().$L", BUILD_NEW_SHELL_METHOD, DISPATCHER_FIELD_NAME);
    builder.addMethod(build.build());
    builder.addMethod(MethodSpec.methodBuilder(BUILD_NEW_SHELL_METHOD)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .addException(CompositionException.class)
        .returns(shellClassName)
        .addCode(CodeBlock.builder()
            .addStatement(shellParameters.map(f -> "$L").mkString("return new $T(", ", ", ")"),
                Seq((Object) shellClassName).appendAll(shellParameters.map(Tuple3::_2)).toJavaArray())
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
    generateSelfProvisions(builder, sourceModel);
    generateForwardedProvisions(builder, sourceModel);
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
      sourceModel.composeMethods().forEach(cm -> cm.via().fold(
          () -> Tuple("$T.this.$L.$L", Seq(shellClassName, DISPATCHER_FIELD_NAME,
              cm.element().name())),
          (m) -> Tuple("$T.this.$L.$L.$L", Seq(shellClassName, DISPATCHER_FIELD_NAME,
              MemberNames.forMount(m.element()), cm.element().name())))
          .apply((call, callArgs) -> {
            var args = new ArrayList<>(callArgs.asJava());
            var pattern = cm.arguments().map(e -> e.fold(
                method -> method.via().map(via -> {
                  args.add(shellClassName);
                  args.add(MemberNames.forMount(via.element()));
                  args.add(method.element().name());
                  return "$T.this.$L.$L.get()";
                }).getOrElse(() -> {
                  args.add(cm.element().name());
                  return "$L.get()";
                }),
                builtin -> {
                  //noinspection SwitchStatementWithTooFewBranches
                  switch (builtin) {
                    case CONFIG:
                      args.add(shellClassName);
                      args.add(CONFIG_FIELD_NAME);
                      return "$T.this.$L";
                    default:
                      args.add("$error");
                      return "$L";
                  }
                }
            )).mkString(call + "(", ", ", ")");
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            code.addStatement(pattern, args.toArray(new Object[args.size()]));
            return null;
          }));
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

  private void generateForwardedProvisions(TypeSpec.Builder builder, ModelType<Element, TypeMirror> model) {
    model.provisionMethods().appendAll(model.extensionPointProvisionMethods())
        .map(m -> m.via().map(v -> Tuple(m, v)))
        .flatMap(identity())
        .forEach(tp -> tp.apply((m, via) ->
            builder.addMethod(MethodSpec.overriding(
                m.element().source(ExecutableElement.class), model.type(DeclaredType.class), env.types())
                .addAnnotations(generatedAnnotations(m))
                .addStatement("return $T.this.$L.$L()", shellClassName,
                    MemberNames.forMount(via.element()), m.element().name())
                .build())));
  }

  private void generateSelfProvisions(TypeSpec.Builder builder, ModelType<Element, TypeMirror> model) {
    model.provisionMethods().appendAll(model.extensionPointProvisionMethods())
        .filter(m -> !m.element().isAbstract())
        .filter(m -> m.via().isEmpty())
        .forEach(m -> {
          var ft = ParameterizedTypeName.get(ClassName.get($Provision.class), TypeName.get(m.element().type()));
          builder.addField(
              FieldSpec.builder(TypeName.get(env.known().rtProvision(m.element().type())), m.element().name(),
                  Modifier.PRIVATE, Modifier.FINAL)
                  .initializer("\n$T.$L($T.class, $S,\n() -> super.$L())",
                      env.types().erasure(env.known().rtProvision()),
                      m.element().provisionConfigOption().filter(c -> !c.shared()).map(__ -> "direct").getOrElse("shared"),
                      env.types().erasure(m.element().parent().type()),
                      m.element().name(),
                      m.element().name())
                  .build());
          builder.addMethod(
              MethodSpec.overriding(m.element().source(ExecutableElement.class), model.type(DeclaredType.class), env.types())
                  .addAnnotations(generatedAnnotations(m))
                  .addStatement("return $L.get()", m.element().name())
                  .build());
        });

  }

  private Seq<AnnotationSpec> generatedAnnotations(ModelMethod<Element, ?> method) {
    // TODO (2019-04-14) implement this
    return Seq();
  }

  private void generateMountMethods(TypeSpec.Builder builder) {
    sourceModel.mountMethods().forEach(m -> {
      var mb = MethodSpec.overriding(m.element().source(ExecutableElement.class), sourceType, env.types())
          .addAnnotations(generatedAnnotations(m));
      if (m.element().mountConfig().external()) {
        mb.addStatement("return $T.this.$L",
            shellClassName, MemberNames.forMount(m.element()));
      } else {
        mb.addStatement("return $L", MemberNames.forMount(m.element()));
      }
      builder.addMethod(mb.build());
    });
  }

  private Seq<TypeSpec.Builder> generateMountClasses() {
    return sourceModel.mountMethods()
        .filter(m -> !m.element().mountConfig().external())
        .map(mount -> {
          TypeSpec.Builder builder =
              TypeSpec.classBuilder(shellClassName.nestedClass(MemberNames.forMountClass(mount.element())))
                  .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL))
                  .superclass(TypeName.get(mount.element().type()))
                  .addModifiers();
          var mountedModel = env.model()
              .modelOf(env.types().asMemberOf(sourceModel.type(DeclaredType.class),
                  asTypeElement(asDeclaredType(mount.element().type()).asElement())));
          mountedModel.mountMethods().appendAll(mountedModel.provisionMethods()).appendAll(mountedModel.extensionPointProvisionMethods())
              .filter(m -> m.element().isAbstract())
              .forEach(m -> builder.addMethod(
                  MethodSpec.overriding(m.element().source(ExecutableElement.class), mountedModel.type(DeclaredType.class), env.types())
                      .addAnnotations(generatedAnnotations(m))
                      .addStatement("return $T.this.$L.$L()",
                          shellClassName, DISPATCHER_FIELD_NAME, m.element().name())
                      .build()));
          generateSelfProvisions(builder, mountedModel);
          generateConfigurationsMethods(builder, mountedModel);
          return builder;
        });
  }

  private void generateConfigurationsMethods(TypeSpec.Builder builder, ModelType<Element, TypeMirror> model) {
    model.configurationMethods()
        .filter(m -> m.via().isEmpty())
        .forEach( m -> {
          var mb = MethodSpec.overriding(
              m.element().source(ExecutableElement.class), model.type(DeclaredType.class), env.types());
          mb.addAnnotations(generatedAnnotations(m));
          // TODO FIXME (2019-04-14) include prefix
          var n = m.element().configurationConfig().fullPath(m.element());
          if (n.equals(Configuration.ALL)) {
            mb.addStatement("return $T.this.$L", shellClassName, CONFIG_FIELD_NAME);
          } else {
            if (!m.element().isAbstract()) {
              mb.beginControlFlow("if ($T.this.$L.hasPath($S))", shellClassName, CONFIG_FIELD_NAME, n);
            }
            mb.addStatement("return $T.this.$L.$L($S)", shellClassName, CONFIG_FIELD_NAME,
                model.model().configSupportedType(m.element()).configMethodName(), n);
            if (!m.element().isAbstract()) {
              mb.endControlFlow();
              mb.beginControlFlow("else");
              mb.addStatement("return super.$L()", m.element().name());
              mb.endControlFlow();
            }
          }
          builder.addMethod(mb.build());
        });
//    model.configurationMethods().forEach(confMethod -> {
//      Types types = env.types();
//      var fullType = ((ExecutableType) types.asMemberOf(model.type(), confMethod.element())).getReturnType();
//      final TypeMirror type;
//      var list = false;
//      if (types.isSubtype(fullType, types.erasure(env.known().iterable()))) {
//        var iteratorType = ((ExecutableType) types.asMemberOf((DeclaredType)fullType, env.known().iterableIterator()))
//            .getReturnType();
//        type = ((ExecutableType) types.asMemberOf((DeclaredType) iteratorType, env.known().iteratorNext()))
//            .getReturnType();
//        list = true;
//      } else {
//        type = fullType;
//      }
//      // TODO (2019-03-31) ? special meaning of types: getBytes()->MemSize, getDuration(with unit), deprecated millis/nanos
//      // TODO (2019-03-31) "internals"? -> ConfigObject, ConfigValue, ConfigOrigin; use Config and continue from there?
//      String getter;
//      Option<Tuple2<String, Seq<?>>> argsPattern = None();
//      String configPath = confMethod.fullPath();
//      if (configPath.equals(Configuration.ALL) && env.known().config().map(t -> isApplicable(type, t)).getOrElse(false)) {
//        // special case: all configuration
//        getter = "";
//      } else if (isPrimitive(TypeKind.INT, type)) {
//        getter = "getInt";
//      } else if (isPrimitive(TypeKind.LONG, type)) {
//        getter = "getLong";
////      } else if (isPrimitive(TypeKind.SHORT, type)) {
////        getter = "(short)getInt";
////      } else if (isPrimitive(TypeKind.BYTE, type)) {
////
//      } else if (isPrimitive(TypeKind.DOUBLE, type)) {
//        getter = "getDouble";
////      } else if (isPrimitive(TypeKind.FLOAT, type)) {
////
//      } else if (isPrimitive(TypeKind.BOOLEAN, type)) {
//        getter = "getBoolean";
////      } else if (isPrimitive(TypeKind.CHAR, type)) {
////        getter = "get"
//      } else if (isApplicable(type, env.known().number())) {
//        getter = "getNumber";
//      } else if (isApplicable(type, env.known().string(), env.known().charSequence())) {
//        getter = "getString";
//      } else if (isApplicable(type, env.known().duration())) {
//        getter = "getDuration";
//      } else if (isApplicable(type, env.known().period())) {
//        getter = "getPeriod";
//      } else if (isApplicable(type, env.known().temporalAmount())) {
//        getter = "getTemporal";
//      } else if (env.known().config().map(t -> isApplicable(type, t)).getOrElse(false)) {
//        getter = "getConfig";
//      } else if (env.known().configMemorySize().map(t -> isApplicable(type, t)).getOrElse(false)) {
//        getter = "getMemorySize";
//      } else if (isApplicable(type, env.known().enumeration())
//          && !(types.isSameType(types.erasure(env.known().enumeration()), types.erasure(type)))) {
//        getter = "getEnum";
//        argsPattern = Some(Tuple("$T.class, $S", Seq(erasure(type), configPath)));
//      } else if (isApplicable(type, env.known().object())) {
//        getter = "getAnyRef";
//      } else {
//        env.problems().error(sourceElement, "Cannot map configuration method " + confMethod.element());
//        return;
//      }
//      var methodBuilder = MethodSpec.overriding(confMethod.element(), model.type(), types);
//      methodBuilder.addAnnotation(generatedAnnotations(confMethod));
//      if (confMethod.hasDefault()) {
//        methodBuilder.addStatement("if (!$T.this.$L.hasPath($S)) return super.$L()",
//            shellClassName, CONFIG_FIELD_NAME, configPath, confMethod.element().getSimpleName());
//      }
//      if (getter.equals("")) {
//        methodBuilder.addStatement("return $T.this.$L", shellClassName, CONFIG_FIELD_NAME);
//      } else {
//        if (list) {
//          getter = getter + "List";
//        }
//        argsPattern = Some(argsPattern.getOrElse(() -> Tuple("$S", Seq(configPath))));
//        if (list) {
//          argsPattern = argsPattern.map(t -> t.map1(m -> m + "List"));
//        }
//        methodBuilder.addStatement("return $T.this.$L.$L(" + argsPattern.get()._1 + ")",
//            Seq((Object)shellClassName, CONFIG_FIELD_NAME, getter).appendAll(argsPattern.get()._2).toJavaArray());
//      }
//      builder.addMethod(methodBuilder.build());
//    });
  }


  private Modifier[] conditionalModifiers(boolean condition, Modifier... modifiers) {
    //noinspection ZeroLengthArrayAllocation
    return condition ? modifiers : new Modifier[0];
  }

  @Override
  public String toString() {
    return "Generator{" +
        "sourceElement=" + sourceElement +
        '}';
  }

}
