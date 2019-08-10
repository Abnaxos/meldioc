Setting up IDEA
===============

Import the project into IDEA from Gradle. I use the following settings:

- Group modules using explicit module groups

- **Do not** create separate module per source set (the imported project
  will probably not work otherwise)
  
- Use a JDK 11 as Gradle JVM

Code style, inspection profile and and copyright configurations are in Git.

Some additional project configurations that need to be set manually:

- *Build, Execution, Deployment* → *Gradle* → *Runner*: **Do not** delegate
  build/run IDE actions to Gradle

- *Build, Execution, Deployment* → *Compiler* → *Annotation Processors*:
  
  - **Enable** annotation processing
  - Obtain processors from project classpath
  - Store generated sources relative to **module content root**
  - "out/production/generated" and "out/test/generated" as production and
    test sources directory

- **Exclude test cases from compile**: *Build, Execution, Deployment* →
  *Compiler* → *Excludes*: Exclude the directory
  *tools/processor/src/test/cases* recursively. It contains deliberate
  compiler errors. These classes will be compiled by the Spock specs.


IDEA plugin
-----------

Add this to your *~/.gradle/gradle.properties* to enable development of the
IDEA plugin:

```
ch.raffael.compose.build-idea-plugin=true
```

There's a Gradle task *tools:idea:syncTestProject* that sets up an
independent sandbox project based on the current sources that you can load
into your debug IDEA instance, just like you imported the main project.
