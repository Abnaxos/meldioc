Changelog
=========

[0.2.0]
--------------------

### All

#### Changes

- (#92) Everything marked as `@Deprecated(forRemoval=true)` has been removed
- (#61) Add *Automatic-Module-Name* attribute to all jar manifests

### Core

#### Fixes

- (#56) Fix code generation when Typesafe Config isn't in the classpath
- Features with a protected constructor are no longer rejected for mounting

#### Enhancements

- (#108) Support for Java 19

#### Changes

- (#104, #99) Overriding provisions in is now an error and all mounted
  provisions are now implied provisions in the configuration
- (#80, #92) Extending a feature without `@Import` is now an error
- Declaring parameters in features is no longer an error when Typesafe Config
  isn't in the classpath -- *mounting* features that have *required* parameters
  is, though

### Library

#### Changes

- (#27) Split `ThreadingFeature` into `WorkExecutorFeature` and
  `ForkJoinPoolFeature`, remove default implementation from
  `ForkJoinPoolFeature` interface; `ThreadingFeature` is still present but
  deprecated for removal
- Remove all default implementations for provision methods from feature
  interfaces, provide separate mountable features instead (using default
  implementations causes conflicts due to #104/#99, see also #109)
- Remove `Immutables.Public` / `Immutables.Local` in favour of new
  `Immutables.Pure` designed for the modern style sandwich pattern
  (see immutables
  [readme](https://github.com/immutables/immutables/blob/b21e6bbfbb9038100532416c81e8270951c3e03a/README.md))

### HTTP

#### Changes

- Comprehensive overhaul, not backwards compatible
- (#73) Gson codec has moved to a separate library (meld-library-codec-gson)
  and package


[0.1.7] (2021-07-21)
----------------------------

### Core

#### Enhancements

- (#51) Unify `ExtensionPoint` annotations
- (#93) Emit a compiler warning if a feature interface doesn't contain only
  provisions
- (#13) Honour `SuppressWarnings`

### Base Library

#### Changes

- Deprecate `VavrX::touch`, use `tap()` instead (align with Scala)
- Upgrade dependencies: Log4j2 1.18.0, Immutables 2.9.2, Typesafe Config 1.4.1

### HTTP

#### Changes

- (#90) Upgrade dependencies: Undertow 2.2.19, Jackson 2.13.1, Gson 2.8.7


[0.1.6] (2021-05-22)
---------------

### Core

#### Enhancements

- (#88) Support language features up to Java 17

### Library

#### Changes

- (#89) Upgrade to Vavr 0.10.3


[0.1.5] (2021-01-30)
------------------

Sorry, no changelog (yet).



[0.2.0]: https://github.com/Abnaxos/meldioc/compare/release/0.1.7...release/0.2.0
[0.1.7]: https://github.com/Abnaxos/meldioc/compare/release/0.1.6...release/0.1.7
[0.1.6]: https://github.com/Abnaxos/meldioc/compare/release/0.1.5...release/0.1.6
[0.1.5]: https://github.com/Abnaxos/meldioc/releases/tag/release/0.1.5
