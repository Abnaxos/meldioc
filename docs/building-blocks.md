Building Blocks
===============

The building blocks of Meld are:

  * **Features** AKA modules: They declare provisions and may provide
    (partial) implementations.
    
  * **Configurations**: These are special cases of features. You can think
    of features always being abstract, while configurations are concrete.
    The `@Configuration` annotation triggers the code generation. For each
    configuration, a configuration shell will be generated that fully
    implements the configuration from all the provided fragments.
    
  * **Provisions**: Providing component implementations.
  
  * **Extension points**: Extending the functionality of a feature. A
    typical example of such an extension point is a *Servlets* extension
    point of an HTTP server feature. Other features will contribute their
    servlets to this extension point.
    
  * **Setup methods**: Methods called very early on initialisation of a
    feature. These methods are mainly used for contributing to extension
    points. They can take *extension acceptors* as arguments, the generated
    code will figure this out.
    
  * **Parameters**: Configuration parameters from a configuration file.


Features and Configurations
---------------------------

Features are comparable to modules. They describe some functionality that
the program or other features can use. The main components of a feature
are *provisions* and *extension points*.

Features should always be declared as an interface annotated with
`@Feature`. Features may extend other features or depend on them. This is
declared in the extends clause. If it's a dependency, this should be
expressed by adding a `@DependsOn` annotation:

```java
@Feature
public interface HttpServerFeature {
}

@Feature
public interface SslContextFeature {
  
  @Provision
  SSLContext sslContext();
}

@Feature
public interface HttpsServerFeature extends HttpServerFeature, @DependsOn SslContextFeatre {
}
```

Note that in the current implementation, `@DependsOn` is purely
informational. In future versions, Meld will perform further checks.


### Partial Implementations

Partial implementations are also annotated with `@Feature` and written as
abstract classes implementing the feature interfaces. They don't have to
implement everything, various parts of a feature may be implemented at
different places to keep things flexible.

Although Meld doesn't require this, it's recommended to always have a
feature interface.

```java
@Feature
public abstract class StandardSslContextFeature implements SslContextFeature {
  
  @Provison(shared = true)
  @Override
  public SSLContext sslContext() {
    return SSLContext.getDefault();
  }
}

@Feature
abstract class UndertowHttpsServerFeature implements HttpsServerFeature {
  
  // Note that this is an internal provision specific to this implementation
  // That's why it's not exposed, it won't be available to other features
  @Provision(shared = true)
  protected undertowServer() {
    // not implemented here, but the method is available through inheritance:
    var sslContext = sslContext(); 
    return Undertow.builder()
      // configure Undertow
      .build();
  }
}
```


### Configurations

Configurations put together a concrete composition of features. They do so
by mounting specific implementations:

```java
@Feature
public interface MyAppContext extends HttpsServerFeature /*, SslContextFeature*/ {
}

@Configuration
abstract class DefaultMyAppContext implements MyAppContext {

  @Mount
  abstract StandardSslContextFeature standardSslContextFeature();
  
  @Mount
  abstract UndertowHttpsServerFeature undertowFeature();
  
}
```

Configurations and mount methods are usually package local. They're very
intimate details.

Mount methods can only occur in configuration classes

### The Generated Code

The `@Configuration` annotation triggers the code generation. The generated
configuration will be available in a package-local class called
`MyAppContextShell`. See [The Generated Code](generated-code.md) for
details.


### Injected Mounts and Scopes

Sometimes, you'll want to take control of what's mounted, do some manual
initialisation or similar things. You can do this using injected mounts:

```java
@Configuration
abstract class MyContext {
  
  @Mount(injected = true)
  abstract SomeFeature someFeature();
  
  static MyContext create(Config config, SomeFeature someFeature) {
    return MyContextShell
      .builder()
      .config(config)
      .someFeature(someFeature)
      .build();
  }
}

```

`SomeFeature` may very well be another configuration, but it can also
be some manual implementation.

This strategy can also be used for scopes:

```java
@Configuration
abstract class MyRequestContext extends @DependsOn MyAppContext {
  
  @Mount
  abstract MyAppContext parent();
  
  static MyRequestContext create(Config config, MyAppContext parent) {
    return MyRequestContextShell
      .builder()
      .config(config)
      .parent(parent)
      .build();
  }
}

```


Provisions
----------

Provisions are the components that implement the functionality that features
provide. Some people (especially in Spring) call them "Services".

Provisions can be shared or unshared, the latter being the default. Shared
provisions are similar to singletons, there's one shared instance in the
context. Unshared provisions are newly instantiated every time it's
requested.

In Spring terms:

- unshared == prototype
- shared == singleton scope

In Guice terms:

- unshared == no scope
- shared == singleton

**Provisions are always lazy.**

Provisions are provided by methods that are annotated with `@Provision`. If
the method is abstract, this is just a declaration that such a component is
expected to be provided by some some other feature. If the method is implemented,
that implementation is the factory method. Shared provision methods will be
overridden to retain the instance (thread-safely, of course).

Provisions are unshared by default. It's recommended to avoid singletons,
but of course, singletons *will* be necessary. To declare a shared
provision, use `@Provision(shared = true)`.

For now, provision methods cannot take parameters. Parametrised provisions
might be added in the future, though.


### Inheritance

When overriding provisions, the *shared* attribute must be specified again.
In Java, annotations are not inherited, and this is Java.

To avoid provisions getting unshared by accident, e.g. by forgetting the
*shared* attribute, there's an additional attribute *override* that must
be explicitly set to true when "downgrading" a provision from shared to
unshared:

```java
@Feature
interface MyFeature {

  @Provision(shared = true)
  Foo foo();
}

@Feature
interface Erroneous1 extends MyFeature {
  @Provision // COMPILER ERROR HERE
  @Override
  Foo foo();
}

@Feature
interface Erroneous2 extends MyFeature {
  @Provision(shared = false) // COMPILER ERROR HERE
  @Override
  Foo foo();
}

@Feature
interface Correct extends MyFeature {
  @Provision(shared = false, override = true) // this is fine
  @Override
  Foo foo();
}

@Feature
interface CorrectBadStyle extends MyFeature {
  @Provision(override = true) // this is fine, but bad style
  @Override
  Foo foo();
}
```

**Important:** Think twice or more before overriding a shared provision as
unshared. That's usually a very bad idea.


### Eager Loading and Context Startup

There is no built-in mechanism for eager loading. Remember: *put the 'C' back
into IoC*. Automating this would also mean we'd need some way to declare an
eager loading order etc.

The pattern to use instead is to provide some startup method.

**Warning: do not eagery load things in setup methods.** Setup methods are
called too early in the initialisation process and the provisions may not
be ready yet to be eagerly loaded.

So, this is how we start the web server in our example:

```java
@Configuration
abstract class DefaultMyAppContext implements MyAppContext {

  @Mount
  abstract StandardSslContextFeature standardSslContextFeature();
  
  @Mount
  abstract UndertowHttpsServerFeature undertowFeature();

  DefaultMyAppContext start() {
    // start the web server by eager-loading it
    undertowFeature().undertowServer();
    return this;
  }
}

public class MyApp {
  
  public static void main(String[] args) {
    DefaultMyAppContextShell.builder()
      .config(ConfigFactory.load())
      .build()
      .start();
  }
}

```


Extension Points and Setup Methods
----------------------------------

Extension points need to components: the extension point declaration itself
(a method annotated with `@ExtensionPoint`) and an extension acceptor (an
instance of a class annotated with `@ExtensionPoint.Acceptor`).

Generally, extension points are like shared provisions, except that they're
usually not public. The main difference is that they're only intended to be
used by setup methods and that they're automatically passed as parameters to
the setup methods.

**Extension acceptors must not be modified outside the setup methods.**

A good example for extension points could be a credit card handler:

```java
@Feature
public abstract class MyPaymentMethodsFeature implements @DependsOn BillingFeature {
  
  @Provision
  protected MyCreditCardPaymentHandler myCreditCardPaymentHandler() {
    return new MyCreditCardHandler();
  }
  
  @Provision
  protected InvoicePaymentHandler invoicePaymentHandler() {
    return new MyCreditCardHandler();
  }
  
  @Setup
  protected void setupPaymentMethods(PaymentMethods paymentMethods) {
    paymentMethods
      .add("MYCC", this::myCreditCardPaymentHandler)
      .add("INVOICE", this::invoicePaymentHandler);
  }
}

@ExtensionPoint.Acceptor
public interface PaymentMethods {
  
  PaymentMethods add(String id, Supplier<? extends PaymentHandler> handler);
  
  static PaymentMethods using(BiFunction<? super String, ? super Supplier<? extends PaymentHandler>, Boolean> adder) {
    return new PaymentMethods() {
      public PaymentMethods add(String id, Supplier<? extends PaymentHandler> handler) {
        if (!adder.apply(id, handler))  {
          throw new IllegalStateException("Duplicate payment handler " + id);
        }
        return this;
      }
    };
  }
}

@Feature
public abstract class DefaultBillingFeature {
  
  private final ConcurrentMap<String, Supplier<? extends PaymentHandler>> paymentMethods =
    new ConcurrentHashMap(4, .7f, 1); 
  
  @ExtensionPoint
  protected PaymentMethods paymentMethods() {
    return PaymentMethods.using((id, h) -> paymentMethods.putIfAbsent(id, h) == null);
  }
  
  @Provision
  public PaymentProvider paymentProvider() {
    return new PaymentProvider(Map.copyOf(paymentMethods));
  }
}

``` 

Note that there is no automatic collection of all payment methods. This is
very much by design, remember: *put the 'C' back into IoC*. This would be
exactly the kind of magic that Meld wants to avoid.


### Extension Acceptors and Thread-Safety

All setup methods are called in a single thread from the constructor of the
generated configuration and all fields in the generated configuration are
declared *final*. So all actions taken during initialisation will be visible
to other threads according to the rules of *final* (see
[JLS §17.5](https://docs.oracle.com/javase/specs/jls/se11/html/jls-17.html#jls-17.5)).

Therefore, no measures are required to ensure thread-safety and visibility,
of extension acceptors as long as no internal states escape the initialising
thread and no further modifications are done after initialisation (which
should both be self-evident). Meld cannot guarantee that an extension
acceptor isn't used outside the setup method, but it is to be considered a
bug to do so.


### Conventions for Extension Acceptors

- don't accept instances but suppliers
- provide an expressive, DSL-like, fluent API
- fail fast if possible


### More About Setup Methods

Setup methods are called very early during initialisation. **They must not
instantiate any provisions**, it's too early for that. As the above example
shows, you might e.g. lose some contributions. The generated code currently
does not check this, but it might in the future.

Preferably, they should fail early with an exception if something's wrong.
They can also throw checked exceptions. Checked exceptions will be declared
in the *build()* method of the configuration's builder, so there's no
wrapping of checked exception, you'll get the original (the same goes for
constructors of features, BTW).

If a setup method returns a value, this will be warned by the compiler. The
return value will be ignored.

The order in which setup methods are called is currently unspecified.


Parameters
----------

Almost all real-world applications need to be parametrised using
configuration files. Meld directly supports this using [Typesafe
Config](https://github.com/lightbend/config). This is optional. If Typesafe
Config is not on the classpath during compilation, there will be no support
for it. You'll usually want it, tough – it's very small yet extremely
powerful. Currently, the configuration backend is not pluggable.

You declare a configuration parameter using an abstract or concrete method
in a feature that's annotated with `@Parameter`. The return value of such a
method must be a type supported by Typesafe Config (including lists,
(un)boxing and `Config`). These methods will be overridden in the generated
code to lookup the values from the configuration. If the method is concrete,
this implementation will call the super method when a parameter is missing.
Otherwise, an exception is thrown.

The name of the value looked up in the configuration is by default the name
of the method. This can be overridden by specifying a name in the
annotation. In the near future, the default name will be "dashified" to
comply with Typesafe Config conventions (i.e. "myConfigValue" →
"my-config-value"). A class-wide prefix can be set that will be prepended to
all configuration value names.

```java
@Feature
public abstract class ParameterizableFeature {
  
  @Parameter
  protected abstract int myInt();
  
  @Parameter
  protected String myStringWithDefault() {
    return "my default value";
  }
  
  @Parameter("another-name")
  protected abstract String someInternalName();
  
  @Feature
  @Parameter.Prefix("foo")
  public static abstract class WithPrefix {
    @Parameter // -> foo.flag
    protected abstract boolean flag();
  }
}
```

The parametrisation is initialised first, so it's safe to call parameter
methods from setup methods and even feature constructors.


The Generated Code
------------------

See [The Generated Code](generated-code.md).
