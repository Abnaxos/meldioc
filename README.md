[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.raffael.meldioc/meldioc-tools-processor/badge.svg?style=flat)](https://search.maven.org/search?q=g:ch.raffael.meldioc")
[![CircleCI](https://circleci.com/gh/Abnaxos/meldioc.svg?style=shield)](https://circleci.com/gh/Abnaxos/meldioc)

Meld IoC
========

Meld is a new approach to IoC (Inversion of Control). The concept is
inspired by the Cake Pattern from Scala, but it improves on it.

This project is experimental and in an early development stage. Feel free to
play with it and use it, but expect things to change. A stable API is
currently *not* a priority. If I think something needs to be renamed or
restructured, I'll do it. If I think so again, I'll do it again. If I think
it was best at the beginning, I'll go back. After all, writing programs is
like composing music: you have to write it, play it, listen to it, improve,
repeat.

Thoughts, contributions and other input are of course welcome.

The code is licensed under the MIT license.


Introduction
------------

For more in-depth documentation, see [full documentation](docs/index.md).

### Feature Summary

  - compile-time only, (almost) no runtime dependencies, no reflection
  - flexible, lightweight and fast
  - isolation: Java access modifiers apply
  - full type safety including generics
  - full support for checked exceptions
  - no magic, only generates source code
  - compatible, it's a standard Java annotation processor
  - IDE support for IntelliJ IDEA
  
### State

Meld is in a usable but incomplete state and there are well-known bugs. The
known bugs are not breaking, though: they concern situations where the
user's code is incorrect. In such situations, the annotation processor
should report an error explaining exactly what's wrong. Currently, this
isn't always true and instead of reporting a good explanation of what's
wrong, the processor just generates code that doesn't compile (I've never
seen it generate code that compiles but misbehaves, however, I can't rule
this out). These *are* bugs and *must* be fixed, but it's **good enough for
an 0.1 release** -- release early, release often.

The library is undertested. Things seem to work as intended, but test
coverage is very bad and only happy paths have been tested. Most of the
library just wraps well-known 3rd party libraries, but there is room for
bugs.

- *Base library:* The APIs in the base library should be stable, but as with
  everything in the project: if breaking changes are necessary, breaking
  changes will be done. A scheduler will be added soon.

- *HTTP / codec:* Works, but not feature-complete. Expect breaking changes
  here.

The IDEA plugin is very primitive for now. It just displays compiler errors
in the code and provides some quick fixes and marks Meld annotated types and
members with icons. There's a lot of potential here.

### Some Examples

IoC/DI frameworks always have a concept of a context. In Spring, it's the
*ApplicationContext*, in Guice, it's the *Injector*, HiveMind had a
*Registry*, the list goes on. This context is usually hidden and should be
used by the developer only in exceptional situations.

Meld exposes this context. The context is where the 'C' in IoC happens.
So, it gives control back to the developer. But it maintains the inversion
by strictly separating the context (where the control is) from the
components implementing the functionality.

Let's have a look:

```java
@Feature
public interface MyAppContext extends HttpServerFeature, JdbcFeature {
}
```

Obviously, this declares an application context that provides an HTTP
server and some way to access a database via JDBC. Let's go a step
further and make this thing more concrete by creating a configuration:

```java
@Configuration
abstract class DefaultMyAppContext implements MyAppContext {

  @Mount
  abstract UndertowServerFeature undertowServer();
  
  @Mount
  abstract HikariCPFeature hikariCP();
}
```

Now we've got one possible configuration that uses Undertow as HTTP server
and HikariCP for JDBC. In its core, Meld is an annotation processor that
will generate a class `DefaultMyAppContextShell` that can be instantiated:

```java
public class MyApp {
  public static void main(String[] args) {
    DefaultMyAppContext ctx = DefaultMyAppContextShell.builder()
      .config(ConfigFactory.load())
      .build();
    // start the application
  }
}
```

Now, how do we implement the required functionality? Here's a short example
for the HikariCP feature:

```java
@Feature
public interface JdbcFeature {
  
  @Provision
  DataSource jdbcDataSource();
}

@Feature
public abstract class HikariCPFeature implements JdbcFeature {
  
  @Parameter // will be read from a configuration file
  protected abstract String jdbcUrl();
  // more parameters
  
  @Provision(shared = true) // shared -> singleton within this context
  public DataSource jdbcDataSource() {
    return buildHikariCP(jdbcUrl());
  }
}
```

When compiling an `@Configuration`, Meld will generate the code to
delegate all provisions to the features they're implemented in, manage
singletons, get configuration parameters etc.

Let's exchange some components:

```java
@Configuration
abstract class AlternativeMyAppContext implements MyAppContext {

  @Mount
  abstract JettyServerFeature jettyServer();
  
  @Mount
  abstract DbcpFeature dbcp();
}
```

We're now using Jetty instead of Undertow as HTTP server and Apache DBCP
instead of HikariCP as JDBC connection pool.

For testing, we'll simply mock the original *MyAppContext* interface.

Using pure JDBC is tedious. Let's add some abstraction to it. It could be
Hibernate, I'll use JDBI in this example:

```java

@Feature
public interface JdbiFeature implements @DependsOn JdbcFeature {
}

@Feature
public interface MyAppContext extends HttpServerFeature, JdbiFeature {
}

@Configuration
abstract class DefaultMyAppContext implements MyAppContext {

  @Mount
  abstract UndertowServerFeature undertowServer();
  
  @Mount
  abstract HikariCPFeature hikariCP();
  
  @Mount
  abstract DefaultJdbiFeature jdbi();  
}
```

For details, see [the full documentation](docs/index.md).

### Scopes

There are no scopes per se in Meld. But you can use sub contexts to
achieve the same effect:

```java
@Feature
public interface MyHttpRequestContext implements @DependsOn MyAppContext {
  // ...
}

@Configuration
abstract class DefaultMyHttpRequestContext implements MyHttpRequestContext {
  
  @Mount(injected = true)
  abstract MyAppContext parent();

  // ...    
  
  static DefaultMyHttpRequestContext create(Config config, MyAppContext parent) {
    return DefaultMyHttpRequestContextShell.builder()
      .config(config)
      .parent(parent)
      .build();
  }
}

```

### Maven/Gradle

Releases are available on
[Maven central](https://search.maven.org/search?q=g:ch.raffael.meldioc),
snapshots on
[Sonatype OSS](https://oss.sonatype.org/content/repositories/snapshots/ch/raffael/meldioc/):

```groovy
repositories {
    // for releases:
    mavenCentral()
    // for SNAPSHOTS:
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
    // insert your version here
    def meldVersion = '0.1.0'

    compileOnly group: 'ch.raffael.meldioc', name: 'meld-annotations', version: meldVersion
    implementation group: 'ch.raffael.meldioc', name: 'meld-library-base', version: meldVersion

    // make sure to enable annotation processing:
    annotationProcessor group: 'ch.raffael.meldioc', name: 'meld-tools-processor', version: meldVersion
}
```


Design Goals
------------

### Focus on IoC

IoC (Inversion of Control) and DI (Dependency Injection) are not the same
thing. Let's look at a simple fragment:

```
private final Foo myFoo = ioc.getFoo();
```

Here, the `ioc.getFoo()` is the IoC part. This way, the consumer doesn't
have to know where that *foo* comes from, where it goes to, how it's
initialised etc. This is IoC.

There's some redundancy here. `private final Foo` already expresses that we
need some *Foo*. This is where DI comes into play: DI is the idea to
automatically resolve such dependencies and "inject" them implicitly. DI
usually implies IoC, but it's not the same thing.

DI has a lot of problems, but here and now not the place and time to discuss
this. Just note that this project focuses on IoC, not DI. Support for JSR
330 injection will be added at some point.


### Put the 'C' back into IoC

Most IoC frameworks tend to not only invert control, but to actually
transfer control to the framework. This project wants to avoid this by
creating two distinct layers that are both under the developer's full
control:

 *  the implementation layer contains the actual functionality
 *  the composition layer defines the components and how to play together
 
This also means that Meld avoids implications. If you want something,
write it down. On the upside, everything that happens is visible in the
code, there's no invisible magic going on. On the downside, it's more
verbose, Meld doesn't anticipate your wishes.


### If it doesn't Compute, it doesn't Compile

Unresolvable dependencies, conflicts etc. are detected at compile-time. The
goal is that the annotation processor complains about all errors in advance
to avoid cryptic subsequent compiler errors in the generated code. More
importantly, we don't want a successfully compiled program to immediately
fail on startup with some runtime exceptions, if possible.


### Slim and Flexible

Meld doesn't have a specific class of applications in mind. In its very
core, it's nothing but a code generator.

To support parametrisation of the application, [Typesafe
Config](https://github.com/lightbend/config) is used. This is an optional
dependency. The `@Parameter` annotation won't be supported if it's not on
the classpath, but everything remains usable.

The core library goes a bit further, it provides some very basic
functionality. It introduces [slf4j](https://www.slf4j.org/) and
[Vavr](https://www.vavr.io/) as dependencies (Vavr might be removed Vavr
from the core library to keep them slim). Again, if you wish to implement
these things yourself, you're free to do so.

Further reaching functionality will be added, e.g. there's already an HTTP
server module based on [Jetty](https://www.eclipse.org/jetty/) (Jetty is
currently being replaced with [Undertow](http://undertow.io/)).
[HikariCP](https://github.com/brettwooldridge/HikariCP) and
[JDBI](http://jdbi.org/) modules are also planned. Here, we're closing in to
the realm of microservices.

Android developers may be interested in this concept, too. It can also be
used to build single components, e.g. a single Servlet or EJB.

The runtime overhead is around zero.


### Stay Close to the Language

Keep language features intact, specifically the type system including full
support for generics (most current DI framework handle that quite well these
days) and exception handling (a weakness of all DI frameworks). Java's
access modifiers fully apply. Keep annotations and the additional semantics
they introduce at a minimum.

Many IoC/DI frameworks are on the verge to being whole new languages. Avoid
that, this is Java. Java SE, specifically.


### Explicit Extension Points

All IoC/DI framework provide a way for a module's functionality being
extended by another module. But they usually don't make this explicit, it's
just possible to do so by using the normal IoC toolset the frameworks
provide. HiveMind was the only exception I know of, it allowed to declare
*configuration points* on one hand, *contributions* to configuration points
on the other hand. Meld also has such a concept via `@ExtensionPoint`.


License
-------

Copyright (c) 2019 Raffael Herzog

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
