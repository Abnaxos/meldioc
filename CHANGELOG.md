Changelog
=========

[0.2.0] (unreleased)
--------------------

### All

- Everything marked with `@Deprecated(forRemoval=true)` has been removed

### Core

- Features with a protected constructor are no longer rejected for mounting
- Extending a feature without `@Import` is now an error

### HTTP

- Comprehensive overhaul
- Gson codec has moved to a separate library (meld-library-codec-gson) and
  package

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
- Upgrade dependencies: Log4j2 1.14.1, Immutables 2.8.8, Typesafe Config 1.4.1

### HTTP

#### Changes

- (#90) Upgrade dependencies: Undertow 2.2.8, Jackson 2.12.3, Gson 2.8.7


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



[0.2.0]: https://github.com/Abnaxos/meldioc/compare/release/0.1.7...develop/next
[0.1.7]: https://github.com/Abnaxos/meldioc/compare/release/0.1.6...release/0.1.7
[0.1.6]: https://github.com/Abnaxos/meldioc/compare/release/0.1.5...release/0.1.6
[0.1.5]: https://github.com/Abnaxos/meldioc/tags
