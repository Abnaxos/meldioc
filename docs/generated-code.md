The Generated Code
==================

The `@Configuration` annotation triggers the code generation. The generated
configuration will be available in a package-local class called
`MyAppContextShell`. This class contains:

  * A builder as the public (usually package local, actually) API to the
    generated code. To instantiate a configuration:
    
    ```java
    import com.typesafe.config.ConfigFactory;
    
    public final class MyApp {
    
      public static void main(String[] args) {
        MyAppContextShell.builder()
          .config(ConfigFactory.load())
          .build();
      }
    }
    ```
    
  * All mounted features are implemented as inner classes. Unimplemented
    provisions will be forwarded to the mounted feature that actually
    provides it.
  
  * Shared provisions will be wrapped with a singleton holder calling super
    to retrieve the singleton instance. The same goes for extension point
    acceptors, but these will always be considered singletons.
  
  * `@Parameter` methods will be implemented retrieving the value from 
    the given `com.typesafe.config.Config`. If a `@Parameter` method
    is not abstract, its super return value will be used as default.
    
    It's save to retrieve configuration parameters in setup methods and
    constructors by calling these methods.
   
  * All `@Setup` methods will be called passing extension acceptors.
    Unresolved extension acceptors are compiler errors.
  
  * Checked exceptions declared by constructors or setup methods are
    declared to be thrown by the `build()` method and must therefore be
    handled accordingly.
    
  * Internally, there's an inner class called `Dispatcher` that fully
    implements the `MyAppContext` interface. This is the object that's
    actually returned by the `build()` method. The enclosing
    `MyAppContextShell` object contains some common data like the
    configuration parameters, but it's not visible to the user.

  * The code generation can be fine-tuned using annotation attributes
