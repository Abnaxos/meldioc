Use Case 'plugins'
=================

This demonstrates how to use a Meld-generated context as plugin host with
plugin discovery, using Java's standard `ServiceLoader`.

The idea is to use a setup method to discover and activate the plugins,
forwarding the main (or better: host) context and its extension points to
it.

The use case is structured as follows:

- package `host`: the actual plugin host

- package `spi`: service provider interface for plugins

- package `myplugin`: a sample plugin

The service interface for the `ServiceLoader` is
[`PluginActivator`](src/main/java/ch/raffael/meldioc/usecases/plugins/spi/PluginActivator.java).
It has one method that takes an implementation of our host context interface
(our generated `@Configuration`) and all extension points as arguments. It's
up to this activator how to continue from there.

In this example, the plugin is also generated by Meld. It mounts the host
context using `@Mount(injected=true)`. The SPI provides a pre-made feature
that's designed for this:
[`PluginHostFeature`](src/main/java/ch/raffael/meldioc/usecases/plugins/spi/PluginHostFeature.java).
The plugin's activator simply constructs the plugin context injecting the
host feature (see
[`MyPluginActivator`](src/main/java/ch/raffael/meldioc/usecases/plugins/myplugin/MyPluginActivator.java)).


Potential code generation support
---------------------------------

Doing this involves quite a lot of manually written boilerplate code. There
are two concepts here, where the annotation processor could help without
being too specific:

* *Exposed extension points*: an interface that specifies a bunch of
  extension points. The annotation processor looks for extension points in
  the same way as for setup methods and generates an implementation of this
  interface

* *Mountable wrapper features*: Forwards all provisions to a delegate passed
  to the constructor. Can also forward *exposed extension points*.

Generally, the whole idea of grouping extension points together to a single
class (let's call it "indirect extension points" for now) is worth a thought
or two.