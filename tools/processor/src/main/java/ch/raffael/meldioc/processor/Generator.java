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

package ch.raffael.meldioc.processor;

import ch.raffael.meldioc.Configuration;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.meta.Generated;
import ch.raffael.meldioc.model.AccessPolicy;
import ch.raffael.meldioc.model.ClassRef;
import ch.raffael.meldioc.model.ModelMethod;
import ch.raffael.meldioc.model.ModelType;
import ch.raffael.meldioc.model.config.ConfigurationConfig;
import ch.raffael.meldioc.model.config.ProvisionConfig;
import ch.raffael.meldioc.processor.env.Environment;
import ch.raffael.meldioc.processor.env.KnownElements;
import ch.raffael.meldioc.processor.util.Elements;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;

import javax.annotation.Nonnull;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

import static ch.raffael.meldioc.processor.Debug.DEVEL_MODE;
import static ch.raffael.meldioc.processor.util.Elements.asDeclaredType;
import static ch.raffael.meldioc.processor.util.Elements.asExecutableType;
import static java.util.function.Function.identity;

/**
 * The code generator.
 */
public class Generator {

  public static final String CONFIG_FIELD_NAME = "config";

  public static final String DISPATCHER_CLASS_NAME = "$Dispatcher";
  public static final String DISPATCHER_FIELD_NAME = "dispatcher";
  public static final String BUILDER_CLASS_NAME = "Builder";
  public static final String BUILDER_METHOD_NAME = "builder";
  public static final String BUILD_METHOD_NAME = "build";
  public static final String NEW_DISPATCHER_METHOD = "newDispatcher";
  public static final String SETUP_METHOD = "setup";
  public static final String BUILD_NEW_SHELL_METHOD = "newShell";
  public static final String CATCH_EXCEPTION = "ex";
  // sometimes, we need to disambiguate class names:
  // if a super class contains an inner/nested class with the same name, this has precedence
  // however, JavaPoet will use the unqualified name -> the generated code will reference the wrong class
  // so, we use a name that won't clash (well, unless the programmer really *wants* to break things)
  public static final String DISAMBIGUATION_PREFIX = "$MeldIoC_";
  public static final String SINGLETON_CLASS_NAME = DISAMBIGUATION_PREFIX + "Singleton";
  public static final String SINGLETON_GETTER_NAME = "getSneakyRethrowing";
  public static final String PROVIDER_CLASS_NAME = DISAMBIGUATION_PREFIX + "Provider";

  private final Class<?> generatorClass;
  private final Environment env;
  private final TypeElement sourceElement;
  private final DeclaredType sourceType;
  private final ModelType<Element, TypeRef> sourceModel;

  private final ConfigurationConfig<Element> configurationConfig;
  private final ClassName shellClassName;
  private final TypeSpec.Builder shellBuilder;
  private final ClassName builderClassName;
  private final ClassName dispatcherClassName;
  private final TypeSpec.Builder dispatcherBuilder;
  private final ClassName singletonClassName;
  private final ClassName providerClassName;

  private boolean generateSingleton = false;

  private Seq<Tuple3<TypeName, String, String>> shellParameters = List.of(
      Tuple.of(KnownElements.CONFIG_TYPE, CONFIG_FIELD_NAME, CONFIG_FIELD_NAME));

  Generator(Class<?> generatorClass, Environment env, TypeElement sourceElement) throws Abort {
    this.generatorClass = generatorClass;
    this.env = env;
    this.sourceElement = sourceElement;
    this.sourceType = (DeclaredType) sourceElement.asType();
    sourceModel = env.model().modelOf(env.typeRef(sourceType));
    configurationConfig = sourceModel.element().configurationConfigOption().getOrElseThrow(
        () -> new Abort(sourceElement + " not annotated with " + Configuration.class.getSimpleName()));
    ClassRef targetRef = configurationConfig.shellClassRef(
        env.elements().getPackageOf(sourceElement).getQualifiedName().toString(), sourceElement.getSimpleName().toString());
    shellClassName = ClassName.get(targetRef.packageName(), targetRef.className());
    shellBuilder = TypeSpec.classBuilder(shellClassName);
    builderClassName = shellClassName.nestedClass(BUILDER_CLASS_NAME);
    singletonClassName = shellClassName.nestedClass(SINGLETON_CLASS_NAME);
    providerClassName = shellClassName.nestedClass(PROVIDER_CLASS_NAME);
    dispatcherClassName = shellClassName.nestedClass(DISPATCHER_CLASS_NAME);
    dispatcherBuilder = TypeSpec.classBuilder(dispatcherClassName);
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

  int errorCount() {
    return env.adaptor().errorCount();
  }

  int warningCount() {
    return env.adaptor().warningCount();
  }

  String generate() {
    Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    if (!DEVEL_MODE) {
      shellBuilder.addModifiers(Modifier.FINAL);
    }
    if (!configurationConfig.packageLocal()) {
      shellBuilder.addModifiers(Modifier.PUBLIC);
    }
    shellBuilder.addAnnotation(AnnotationSpec.builder(Generated.class)
        .addMember(Generated.TIMESTAMP_ATTR, "$S",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp.atZone(ZoneId.systemDefault())))
        .addMember(Generated.VERSION_ATTR, "$S", Version.version())
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
        .filter(m -> !m.element().mountConfig().injected())
        .forEach( m -> {
              var className = shellClassName.nestedClass(MemberNames.forMountClass(m.element()));
              shellBuilder.addField(FieldSpec.builder(className, MemberNames.forMount(m.element()),
                  Modifier.PRIVATE, Modifier.FINAL)
                  .initializer("new $T()", className)
                  .build());
            }
        );
    shellParameters = shellParameters.appendAll(sourceModel.mountMethods()
        .filter(m -> m.element().mountConfig().injected())
        .map(m -> Tuple.of(TypeName.get(Elements.asDeclaredType(m.element().type().mirror())),
            MemberNames.forMount(m.element()),
            MemberNames.forMount(m.element()))));
    shellParameters.forEach(tpl -> tpl.apply((t, n, __) -> shellBuilder.
        addField(FieldSpec.builder(t, n, Modifier.FINAL)
            .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
            .build())));
    shellBuilder.addField(FieldSpec.builder(dispatcherClassName, DISPATCHER_FIELD_NAME, Modifier.FINAL)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
    shellBuilder.addMethod(MethodSpec.methodBuilder(BUILDER_METHOD_NAME)
        .addModifiers(Modifier.STATIC)
        .addModifiers(conditionalModifiers(!configurationConfig.packageLocal(), Modifier.PUBLIC))
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
    var throwing = generateSetupMethod(shellBuilder);
    shellBuilder.addMethod(generateShellConstructor(throwing));
    generateBuilder(throwing);
    generateDispatcher();

    shellBuilder.addType(dispatcherBuilder.build());
    mounts.map(TypeSpec.Builder::build).forEach(shellBuilder::addType);
    if (generateSingleton) {
      generateProvider();
      generateSingleton();
    }

    var fileBuilder = JavaFile.builder(shellClassName.packageName(), shellBuilder.build())
        .addFileComment("Generated by ch.raffael.meldioc, " + new Date(timestamp.toEpochMilli()));
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
  private MethodSpec generateShellConstructor(Traversable<DeclaredType> throwing) {
    var builder = MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE));
    throwing.forEach(e -> builder.addException(TypeName.get(e)));
    var code = CodeBlock.builder();
    shellParameters.forEach(tpl -> tpl.apply((t, n, m) -> {
      builder.addParameter(t, n);
      String statement = "this.$L = $T.requireNonNull($L, $S)";
      if (n.equals(CONFIG_FIELD_NAME)) {
        statement += ".resolve()";
      }
      builder.addStatement(statement, n, Objects.class, n, n + " is null");
      return null;
    }));
    code.addStatement("$L = $L()", DISPATCHER_FIELD_NAME, NEW_DISPATCHER_METHOD);
    code.addStatement("$L()", SETUP_METHOD);
    builder.addCode(code.build());
    return builder.build();
  }

  private void generateBuilder(Traversable<DeclaredType> throwing) {
    var builder = TypeSpec.classBuilder(builderClassName);
    builder.addModifiers(Modifier.STATIC);
    builder.addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.FINAL));
    builder.addModifiers(conditionalModifiers(!configurationConfig.packageLocal(), Modifier.PUBLIC));
    shellParameters.forEach(f -> builder.addField(f._1, f._2, conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE)));
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
    shellParameters.forEach(f -> f.apply((t, n, bn) -> builder.addMethod(MethodSpec.methodBuilder(bn)
        .addModifiers(conditionalModifiers(!configurationConfig.packageLocal(), Modifier.PUBLIC))
        .addParameter(t, bn)
        .returns(builderClassName)
        .addCode(CodeBlock.builder()
            .addStatement("this.$L = $L", n, bn)
            .addStatement("return this")
            .build())
        .build())));
    MethodSpec.Builder buildBuilder = MethodSpec.methodBuilder(BUILD_METHOD_NAME)
        .addModifiers(conditionalModifiers(!configurationConfig.packageLocal(), Modifier.PUBLIC))
        .returns(ClassName.get(sourceType));
    throwing.forEach(e -> buildBuilder.addException(TypeName.get(e)));
    shellParameters
        .map(Tuple3::_2)
        .forEach(f -> buildBuilder.addStatement("if ($L == null) throw new $T($S)",
            f, IllegalStateException.class, f + " is not set"));
    buildBuilder.addStatement("return $L().$L", BUILD_NEW_SHELL_METHOD, DISPATCHER_FIELD_NAME);
    builder.addMethod(buildBuilder.build());
    MethodSpec.Builder newShellBuilder = MethodSpec.methodBuilder(BUILD_NEW_SHELL_METHOD)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .returns(shellClassName)
        .addCode(CodeBlock.builder()
                .addStatement(shellParameters.map(f -> "$L").mkString("return new $T(", ", ", ")"),
                    List.of((Object) shellClassName).appendAll(shellParameters.map(Tuple3::_2)).toJavaArray()).build());
    throwing.forEach(e -> newShellBuilder.addException(TypeName.get(e)));
    builder.addMethod(newShellBuilder.build());
    shellBuilder.addType(builder.build());
  }

  void generateDispatcher() {
    dispatcherBuilder.superclass(TypeName.get(sourceElement.asType()))
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE, Modifier.FINAL));
    generateDispatcherMembers(dispatcherBuilder);
  }

  private void generateDispatcherMembers(TypeSpec.Builder builder) {
    generateDispatcherConstructor(builder);
    generateSelfProvisions(builder, sourceModel);
    generateForwardedProvisions(builder, sourceModel);
    generateMountMethods(builder);
    generateParameterMethods(builder, sourceModel);
  }

  private void generateDispatcherConstructor(TypeSpec.Builder builder) {
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE))
        .build());
  }

  private Traversable<DeclaredType> generateSetupMethod(TypeSpec.Builder builder) {
    var code = CodeBlock.builder();
    CatchHelper catchHelper = new CatchHelper(env);
    if (sourceModel.setupMethods().nonEmpty()) {
      sourceModel.setupMethods().forEach(cm -> cm.via().fold(
          () -> {
            catchHelper.add(cm.element().source(ExecutableElement.class).getThrownTypes().stream());
            return Tuple.of("$T.this.$L.$L", List.of(shellClassName, DISPATCHER_FIELD_NAME,
                cm.element().name()));
          },
          (via) -> {
            catchHelper.add(
                asExecutableType(env.types().asMemberOf(asDeclaredType(via.element().type().mirror()), cm.element().source()))
                    .getThrownTypes().stream());
            return Tuple.of("$T.this.$L.$L", List.of(shellClassName,
                MemberNames.forMount(via.element()), cm.element().name()));
          })
          .apply((call, callArgs) -> {
            var args = new ArrayList<>(callArgs.asJava());
            var pattern = cm.arguments().map(e -> e.fold(
                method -> method.via().map(via -> {
                  args.add(shellClassName);
                  args.add(MemberNames.forMount(via.element()));
                  args.add(method.element().name());
                  catchHelper.add(method.element().source(ExecutableElement.class).getThrownTypes().stream());
                  return "$T.this.$L.$L()";
                }).getOrElse(() -> {
                  args.add(shellClassName);
                  args.add(DISPATCHER_FIELD_NAME);
                  args.add(method.element().name());
                  catchHelper.add(method.element().source(ExecutableElement.class).getThrownTypes().stream());
                  return "$T.this.$L.$L()";
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
            )).mkString(call + "(\n", ",\n", ")");
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            code.addStatement(pattern, args.toArray(new Object[args.size()]));
            return null;
          }));
    }
    var methodBuilder = MethodSpec.methodBuilder(SETUP_METHOD)
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE));
    catchHelper.checked().forEach(e -> methodBuilder.addException(TypeName.get(e)));
    builder.addMethod(methodBuilder
        .addCode(code.build())
        .build());
    return catchHelper.checked();
  }

  private void generateForwardedProvisions(TypeSpec.Builder builder, ModelType<Element, TypeRef> model) {
    Seq<ModelMethod<Element, TypeRef>> allProvisions = model.provisionMethods().appendAll(model.extensionPointMethods());
    allProvisions
        .map(m -> m.via().map(v -> Tuple.of(m, v)))
        .flatMap(identity())
        .forEach(tp -> tp.apply((m, via) ->
            builder.addMethod(MethodSpec.overriding(
                m.element().source(ExecutableElement.class), asDeclaredType(model.type().mirror()), env.types())
                .addAnnotations(generatedAnnotations(m))
                .addStatement("return $T.this.$L.$L()", shellClassName,
                    MemberNames.forMount(via.element()), m.element().name())
                .build())));
  }

  private void generateSelfProvisions(TypeSpec.Builder builder, ModelType<Element, TypeRef> model) {
    model.provisionMethods().appendAll(model.extensionPointMethods())
        .filter(m -> !m.element().isAbstract())
        .filter(m -> m.via().isEmpty())
        .forEach(m -> {
          var methodBuilder = MethodSpec.overriding(
              m.element().source(ExecutableElement.class), asDeclaredType(model.type().mirror()), env.types())
              .addAnnotations(generatedAnnotations(m));
          if (m.element().provisionConfigOption().map(ProvisionConfig::singleton).getOrElse(true)) {
            generateSingleton = true;
            builder.addField(
                FieldSpec.builder(
                    ParameterizedTypeName.get(singletonClassName, TypeName.get(m.element().type().mirror())),
                    m.element().name(),
                    Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T<>(() -> super.$L())",
                        singletonClassName, m.element().name())
                    .build());
//            methodBuilder.beginControlFlow("try");
            methodBuilder.addStatement("return $L.$L()", m.element().name(), SINGLETON_GETTER_NAME);
//            methodBuilder.endControlFlow();
//            new CatchHelper(env)
//                .add(Elements.asExecutableType(m.element().source().asType()).getThrownTypes()
//                    .stream().map(Elements::asDeclaredType))
//                .withAll(e -> {
//                  methodBuilder.beginControlFlow("catch (" + e.toArray().map(__ -> "$T").mkString(" | ") + " $L)",
//                      Vector.<Object>ofAll(e).append(CATCH_EXCEPTION).toJavaArray());
//                  methodBuilder.addStatement("throw $L", CATCH_EXCEPTION);
//                  methodBuilder.endControlFlow();
//                })
//                .catchAll(() -> {
//                  methodBuilder.beginControlFlow("catch ($T $L)", Throwable.class, CATCH_EXCEPTION);
//                  methodBuilder.addStatement("throw ($1T)new $1T($2S+$3L).initCause($3L)",
//                      ASSERTION_ERROR, "Undeclared throwable during provision: ", CATCH_EXCEPTION);
//                  methodBuilder.endControlFlow();
//                });
          } else {
            methodBuilder.addStatement("return super.$L()", m.element().name());
          }
          builder.addMethod(methodBuilder.build());
        });
  }

  private Seq<AnnotationSpec> generatedAnnotations(ModelMethod<Element, ?> method) {
    // TODO (2019-04-14) implement this
    return List.empty();
  }

  private void generateMountMethods(TypeSpec.Builder builder) {
    sourceModel.mountMethods()
        .reject(m -> m.element().synthetic())
        .forEach(m -> {
          var mb = MethodSpec.overriding(m.element().source(ExecutableElement.class), sourceType, env.types())
              .addAnnotations(generatedAnnotations(m));
          if (m.element().mountConfig().injected()) {
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
        .filter(m -> !m.element().mountConfig().injected())
        .map(mount -> {
          TypeSpec.Builder builder =
              TypeSpec.classBuilder(shellClassName.nestedClass(MemberNames.forMountClass(mount.element())))
                  .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE, Modifier.FINAL))
                  .addModifiers();
          if (asDeclaredType(mount.element().type().mirror()).asElement().getKind() == ElementKind.INTERFACE) {
            builder.addSuperinterface(TypeName.get(mount.element().type().mirror()));
          } else {
            builder.superclass(TypeName.get(mount.element().type().mirror()));
          }
          var mountedModel = env.model().modelOf(env.typeRef(asDeclaredType(mount.element().type().mirror())));
          mountedModel.mountMethods().appendAll(mountedModel.provisionMethods()).appendAll(mountedModel.extensionPointMethods())
              .filter(m -> m.element().isAbstract())
              .forEach(m -> builder.addMethod(
                  MethodSpec.overriding(m.element().source(ExecutableElement.class), asDeclaredType(mountedModel.type().mirror()), env.types())
                      .addAnnotations(generatedAnnotations(m))
                      .addStatement("return $T.this.$L.$L()",
                          shellClassName, DISPATCHER_FIELD_NAME, m.element().name())
                      .build()));
          generateSelfProvisions(builder, mountedModel);
          generateParameterMethods(builder, mountedModel);
          generateSetupExposureOverrides(builder, mountedModel);
          return builder;
        });
  }

  private void generateParameterMethods(TypeSpec.Builder builder, ModelType<Element, TypeRef> model) {
    model.parameterMethods()
        .filter(m -> m.via().isEmpty())
        .forEach( cm -> {
          var mbuilder = MethodSpec.overriding(
              cm.element().source(ExecutableElement.class), asDeclaredType(model.type().mirror()), env.types());
          mbuilder.addAnnotations(generatedAnnotations(cm));
          var configRef = model.model().configSupportedTypeOption(cm.element().type()).getOrNull();
          if (configRef == null) {
            // abort
            return;
          }
          var n = cm.element().parameterConfig().fullPath(cm.element());
          if (n.equals(Parameter.ALL)) {
            mbuilder.addStatement("return $T.this.$L", shellClassName, CONFIG_FIELD_NAME);
          } else {
            if (!cm.element().isAbstract()) {
              mbuilder.beginControlFlow("if ($T.this.$L.hasPath($S))", shellClassName, CONFIG_FIELD_NAME, n);
            }
            if (configRef.targetTypeArgument() != null) {
              mbuilder.addStatement("return $T.this.$L.$L($T.class, $S)", shellClassName, CONFIG_FIELD_NAME,
                  configRef.configMethodName(),
                  configRef.targetTypeArgument().mirror(), n);
            } else {
              mbuilder.addStatement("return $T.this.$L.$L($S)", shellClassName, CONFIG_FIELD_NAME,
                  configRef.configMethodName(), n);
            }
            if (!cm.element().isAbstract()) {
              mbuilder.endControlFlow();
              mbuilder.beginControlFlow("else");
              mbuilder.addStatement("return super.$L()", cm.element().name());
              mbuilder.endControlFlow();
            }
          }
          builder.addMethod(mbuilder.build());
        });
  }

  private void generateSetupExposureOverrides(TypeSpec.Builder builder, ModelType<Element, TypeRef> model) {
    var pkg = env.elements().getPackageOf(asDeclaredType(model.type().mirror()).asElement());
    if (!pkg.getQualifiedName().toString().equals(shellClassName.packageName())) {
      model.setupMethods()
          .filter(m -> m.element().accessPolicy() == AccessPolicy.PROTECTED)
          .forEach(sm -> {
            ExecutableElement elem = sm.element().source(ExecutableElement.class);
            var mbuilder = MethodSpec.overriding(elem, asDeclaredType(model.type().mirror()), env.types());
            mbuilder.addStatement(
                (elem.getReturnType().getKind() == TypeKind.VOID ? "" : "return ") + "super.$L($L)",
                elem.getSimpleName().toString(),
                elem.getParameters().stream()
                    .map(p -> p.getSimpleName().toString())
                    .collect(Collectors.joining(", ")));
            builder.addMethod(mbuilder.build());
          });
    }
  }

  /**
   * Basically the same as {@code MethodSpec.overriding()}, but without the
   * {@code @Override} annotation. The methods are also always package local.
   */
  private Tuple3<MethodSpec.Builder, ExecutableType, java.util.List<? extends VariableElement>> methodWithSignatureFrom(
      ModelMethod<Element, TypeRef> m) {
    var mb = MethodSpec.methodBuilder(m.element().name());
//    switch (m.element().accessPolicy()) {
//      case PROTECTED:
//        mb.addModifiers(Modifier.PROTECTED);
//        break;
//      case PUBLIC:
//        mb.addModifiers(Modifier.PUBLIC);
//        break;
//    }
    var exec = asExecutableType(
        env.types().asMemberOf(asDeclaredType(m.modelType().type().mirror()), m.element().source()));
    mb.returns(TypeName.get(exec.getReturnType()));
    var params = m.element().source(ExecutableElement.class).getParameters();
    var paramTypes = exec.getParameterTypes();
    for (int i = 0; i < params.size(); i++) {
      mb.addParameter(TypeName.get(paramTypes.get(i)), params.get(i).getSimpleName().toString());
    }
    exec.getThrownTypes().stream().map(TypeName::get).forEach(mb::addException);
    return Tuple.of(mb, exec, params);
  }

  private Modifier[] conditionalModifiers(boolean condition, Modifier... modifiers) {
    //noinspection ZeroLengthArrayAllocation
    return condition ? modifiers : new Modifier[0];
  }

  private void generateProvider() {
    var typeVar = TypeVariableName.get("T");
    var type = TypeSpec.interfaceBuilder(providerClassName).addTypeVariable(typeVar)
        .addAnnotation(ClassName.get("java.lang", "FunctionalInterface"))
        .addModifiers(Modifier.STATIC, Modifier.PRIVATE);
    type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .addException(TypeName.get(env.known().throwable()))
        .returns(typeVar).build());
    shellBuilder.addType(type.build());
  }

  private void generateSingleton() {
    ClassName objects = ClassName.get("java.util", "Objects");
    var typeVar = TypeVariableName.get("T");
    var type = TypeSpec.classBuilder(singletonClassName).addTypeVariable(typeVar)
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
    type.addField(FieldSpec.builder(ParameterizedTypeName.get(providerClassName, WildcardTypeName.subtypeOf(typeVar)),
        "provider", Modifier.PRIVATE, Modifier.VOLATILE).build());
    type.addField(FieldSpec.builder(typeVar, "instance", Modifier.PRIVATE).build());
    type.addMethod(MethodSpec.constructorBuilder()
        .addParameter(ParameterizedTypeName.get(providerClassName, WildcardTypeName.subtypeOf(typeVar)), "provider")
        .addStatement("this.provider = $T.requireNonNull(provider, $S)", objects, "provider")
        .build());
    type.addMethod(MethodSpec.methodBuilder(SINGLETON_GETTER_NAME)
        .returns(typeVar)
        .addCode(CodeBlock.builder()
            .beginControlFlow("if (provider != null)")
            .beginControlFlow("synchronized (this)")
            .beginControlFlow("if (provider != null)")
            .beginControlFlow("try")
            .addStatement("instance = $T.requireNonNull(provider.get(), $S)", objects, "provider.get()")
            .addStatement("provider = null")
            .nextControlFlow("catch ($T e)", env.known().throwable())
            .addStatement("sneakyThrow(e)")
            .endControlFlow()
            .endControlFlow()
            .endControlFlow()
            .endControlFlow()
            .addStatement("return instance")
            .build())
        .addJavadoc("This sneakyThrows any exceptions thrown in the provider. \n" +
            "The user is responsible for declaring checked exceptions. \n" +
            "Calling generated code <em>will</em> redeclare all declared exceptions, \n" +
            "so undeclared checked exceptions are only thrown, when the user does so \n" +
            "in his code.")
        .build());
    var excpetionTypeVar = TypeVariableName.get("E");
    type.addMethod(MethodSpec.methodBuilder("sneakyThrow")
        .addAnnotation(suppressUnchecked())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addParameter(TypeName.get(env.known().throwable()), CATCH_EXCEPTION)
        .addTypeVariable(excpetionTypeVar.withBounds(TypeName.get(env.known().throwable())))
        .addException(excpetionTypeVar)
        .addStatement("throw ($T)$L", excpetionTypeVar, CATCH_EXCEPTION)
        .build());
    shellBuilder.addType(type.build());
  }

  private AnnotationSpec suppressUnchecked() {
    return AnnotationSpec.builder(ClassName.get("java.lang", "SuppressWarnings"))
        .addMember("value", "$S", "unchecked")
        .build();
  }

  @Override
  public String toString() {
    return "Generator{" +
        "sourceElement=" + sourceElement +
        '}';
  }

  public static class Abort extends Exception {
    public Abort() {
      super();
    }

    public Abort(String message) {
      super(message);
    }

    public Abort(String message, Throwable cause) {
      super(message, cause);
    }

    public Abort(Throwable cause) {
      super(cause);
    }
  }
}
