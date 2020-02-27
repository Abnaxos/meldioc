Setting up IDEA
===============

Import the project into IDEA from Gradle. I use the following settings:

- Group modules using explicit module groups

- **Do not** create separate module per source set (the imported project
  will probably not work otherwise)
  
- Use a JDK 11 as Gradle JVM

Code style, inspection profile and and copyright configurations are in Git.

It's strongly recommended to have IDEA delegate build/run actions to
Gradle (*Build, Execution, Deployment* → *Build Tools* → *Gradle*).


IDEA plugin
-----------

Add this to your *~/.gradle/gradle.properties* to enable development of the
IDEA plugin:

```
ch.raffael.meldioc.build-idea-plugin=true
```

There's a Gradle task *tools:idea:syncTestProject* that sets up an
independent sandbox project based on the current sources that you can load
into your debug IDEA instance, just like you imported the main project.
