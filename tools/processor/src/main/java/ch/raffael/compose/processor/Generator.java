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
import ch.raffael.compose.Parameter;
import ch.raffael.compose.meta.Generated;
import ch.raffael.compose.model.ClassRef;
import ch.raffael.compose.model.ModelMethod;
import ch.raffael.compose.model.ModelType;
import ch.raffael.compose.model.config.ConfigurationConfig;
import ch.raffael.compose.model.config.ProvisionConfig;
import ch.raffael.compose.processor.env.Environment;
import ch.raffael.compose.processor.env.KnownElements;
import ch.raffael.compose.processor.util.Elements;
import ch.raffael.compose.rt.UndeclaredThrowableDuringProvisionException;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vavr.Tuple3;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;
import io.vavr.collection.Vector;

import javax.annotation.Nonnull;
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
import static ch.raffael.compose.processor.util.Elements.asExecutableType;
import static ch.raffael.compose.processor.util.Elements.asTypeElement;
import static io.vavr.API.*;
import static java.util.function.Function.identity;

/**
 * The code generator.
 */
public class Generator {

  public static final String CONFIG_FIELD_NAME = "config";

  public static final String DISPATCHER_CLASS_NAME = "Dispatcher";
  public static final String DISPATCHER_FIELD_NAME = "dispatcher";
  public static final String BUILDER_CLASS_NAME = "Builder";
  public static final String BUILDER_METHOD_NAME = "builder";
  public static final String BUILD_METHOD_NAME = "build";
  public static final String NEW_DISPATCHER_METHOD = "$newDispatcher";
  public static final String SETUP_METHOD = "$setup";
  public static final String BUILD_NEW_SHELL_METHOD = "$newShell";
  public static final String CATCH_EXCEPTION = "$ex";

  private final Class<?> generatorClass;
  private final Environment env;
  private final TypeElement sourceElement;
  private final DeclaredType sourceType;
  private final ModelType<Element, TypeMirror> sourceModel;

  private final ConfigurationConfig<Element> configurationConfig;
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
    sourceModel = env.model().modelOf(sourceType);
    configurationConfig = sourceModel.element().configurationConfigOption().getOrElseThrow(
        () -> new IllegalStateException(sourceElement + " not annotated with " + Configuration.class.getSimpleName()));
    ClassRef targetRef = configurationConfig.shellClassRef(
        env.elements().getPackageOf(sourceElement).getQualifiedName().toString(), sourceElement.getSimpleName().toString());
    shellClassName = ClassName.get(targetRef.packageName(), targetRef.className());
    shellBuilder = TypeSpec.classBuilder(shellClassName);
    builderClassName = shellClassName.nestedClass(BUILDER_CLASS_NAME);
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
        .map(m -> Tuple(TypeName.get(Elements.asDeclaredType(m.element().type())),
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
  private MethodSpec generateShellConstructor(Traversable<DeclaredType> throwing) {
    var builder = MethodSpec.constructorBuilder()
        .addModifiers(conditionalModifiers(!DEVEL_MODE, Modifier.PRIVATE));
    throwing.forEach(e -> builder.addException(TypeName.get(e)));
    var code = CodeBlock.builder();
    shellParameters.forEach(tpl -> tpl.apply((t, n, m) -> {
      builder.addParameter(t, n);
      builder.addStatement("this.$L = $T.requireNonNull($L, $S)", n, Objects.class, n, n + " is null");
      return null;
    }));
    shellParameters.find(f -> f._2.equals(CONFIG_FIELD_NAME)).forEach((tpl) ->
        builder.addStatement("if (!this.$L.isResolved()) throw new $T($S)",
            CONFIG_FIELD_NAME, KnownElements.CONFIG_NOT_RESOLVED_EXCEPTION_TYPE,
            "Configuration has not been resolved, you need to call Config#resolve(),"
                + " see API docs for Config#resolve()"));
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
                    Seq((Object) shellClassName).appendAll(shellParameters.map(Tuple3::_2)).toJavaArray()).build());
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
            return Tuple("$T.this.$L.$L", Seq(shellClassName, DISPATCHER_FIELD_NAME,
                cm.element().name()));
          },
          (via) -> {
            catchHelper.add(
                asExecutableType(env.types().asMemberOf(asDeclaredType(via.element().type()), cm.element().source()))
                    .getThrownTypes().stream());
            return Tuple("$T.this.$L.$L.$L", Seq(shellClassName, DISPATCHER_FIELD_NAME,
                MemberNames.forMount(via.element()), cm.element().name()));
          })
          .apply((call, callArgs) -> {
            var args = new ArrayList<>(callArgs.asJava());
            var pattern = cm.arguments().map(e -> e.fold(
                method -> method.via().map(via -> {
                  args.add(shellClassName);
                  args.add(MemberNames.forMount(via.element()));
                  args.add(method.element().name());
                  return "$T.this.$L.$L()";
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
          var methodBuilder = MethodSpec.overriding(
              m.element().source(ExecutableElement.class), model.type(DeclaredType.class), env.types())
              .addAnnotations(generatedAnnotations(m));
          if (m.element().provisionConfigOption().map(ProvisionConfig::shared).getOrElse(true)) {
            builder.addField(
                FieldSpec.builder(TypeName.get(env.known().rtProvision(m.element().type())), m.element().name(),
                    Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("$T.of(() -> super.$L())",
                        env.types().erasure(env.known().rtShared()),
                        m.element().name())
                    .build());
            methodBuilder.beginControlFlow("try");
            methodBuilder.addStatement("return $L.get()", m.element().name());
            methodBuilder.endControlFlow();
            new CatchHelper(env)
                .add(Elements.asExecutableType(m.element().source().asType()).getThrownTypes()
                    .stream().map(Elements::asDeclaredType))
                .withAll(e -> {
                  methodBuilder.beginControlFlow("catch (" + e.toArray().map(__ -> "$T").mkString(" | ") + " $L)",
                      Vector.<Object>ofAll(e).append(CATCH_EXCEPTION).toJavaArray());
                  methodBuilder.addStatement("throw $L", CATCH_EXCEPTION);
                  methodBuilder.endControlFlow();
                })
                .catchAll(() -> {
                  methodBuilder.beginControlFlow("catch ($T $L)", Throwable.class, CATCH_EXCEPTION);
                  methodBuilder.addStatement("throw new $T($L)", UndeclaredThrowableDuringProvisionException.class, CATCH_EXCEPTION);
                  methodBuilder.endControlFlow();
                });

          } else {
            methodBuilder.addStatement("return super.$L()", m.element().name());
          }
          builder.addMethod(methodBuilder.build());
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
          generateParameterMethods(builder, mountedModel);
          return builder;
        });
  }

  private void generateParameterMethods(TypeSpec.Builder builder, ModelType<Element, TypeMirror> model) {
    model.parameterMethods()
        .filter(m -> m.via().isEmpty())
        .forEach( cm -> {
          var mbuilder = MethodSpec.overriding(
              cm.element().source(ExecutableElement.class), model.type(DeclaredType.class), env.types());
          mbuilder.addAnnotations(generatedAnnotations(cm));
          var n = cm.element().parameterConfig().fullPath(cm.element());
          if (n.equals(Parameter.ALL)) {
            mbuilder.addStatement("return $T.this.$L", shellClassName, CONFIG_FIELD_NAME);
          } else {
            if (!cm.element().isAbstract()) {
              mbuilder.beginControlFlow("if ($T.this.$L.hasPath($S))", shellClassName, CONFIG_FIELD_NAME, n);
            }
            mbuilder.addStatement("return $T.this.$L.$L($S)", shellClassName, CONFIG_FIELD_NAME,
                model.model().configSupportedType(cm.element()).configMethodName(), n);
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
