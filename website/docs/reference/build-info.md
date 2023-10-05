---
title: BuildInfo
sidebar_position: 6
---

:::caution
BuildInfo is a restricted feature and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

During the building process Scala CLI collects information about the project's configuration,
both from the console options and `using directives` found in the project's sources.
You can access this information from your code using the `BuildInfo` object, that's automatically generated for your
build on compile when that information changes.

To enable BuildInfo generation pass the `--build-info` option to Scala CLI or use a
`//> using buildInfo` directive.

## Usage

The generated BuildInfo object is available on the project's classpath. To access it you need to import it first.
It is available in the package `scala.cli.build` so use
```scala
import scala.cli.build.BuildInfo
```
to import it.

Below you can find an example instance of the BuildInfo object, with all fields explained.
Some of the values have been shortened for readability.

```scala
package scala.cli.build

/** Information about the build gathered by Scala CLI */
object BuildInfo {
  /** version of Scala used to compile this project */
  val scalaVersion = "3.3.0"
  /** target platform of this project, it can be "JVM" or "JS" or "Native" */
  val platform = "JVM"
  /** version of JVM, if it's the target platform */
  val jvmVersion = Some("11")
  /** version of Scala.js, if it's the target platform */
  val scalaJsVersion = None
  /** Scala.js ECMA Script version, if Scala.js is the target platform */
  val jsEsVersion = None
  /** version of Scala Native, if it's the target platform */
  val scalaNativeVersion = None
  /** Main class specified for the project */
  val mainClass = Some("Main")
  /** Project version */
  val projectVersion = None

  /** Information about the Main scope */
  object Main {
    /** sources found for the scope */
    val sources = Seq(".../Main.scala")
    /** scalac options for the scope */
    val scalacOptions = Seq("-Werror")
    /** compiler plugins used in this scope */
    val scalaCompilerPlugins = Nil
    /** dependencies used in this scope */
    val dependencies = Seq("com.lihaoyi:os-lib_3:0.9.1")
    /** dependency resolvers used in this scope */
    val resolvers = Seq("https://repo1.maven.org/maven2", "ivy:file:...")
    /** resource directories used in this scope */
    val resourceDirs = Seq(".../resources")
    /** custom jars added to this scope */
    val customJarsDecls = Seq(".../AwesomeJar1.jar", ".../AwesomeJar2.jar")
  }

  /** Information about the Test scope */
  object Test {
    /** sources found for the scope */
    val sources = Seq(".../MyTests.scala")
    /** scalac options for the scope */
    val scalacOptions = Seq("-Vdebug")
    /** compiler plugins used in this scope */
    val scalaCompilerPlugins = Nil
    /** dependencies used in this scope */
    val dependencies = Seq("org.scala-lang:toolkit_3:latest.release")
    /** dependency resolvers used in this scope */
    val resolvers = Seq("https://repo1.maven.org/maven2", "ivy:file:...")
    /** resource directories used in this scope */
    val resourceDirs = Seq(".../test/resources")
    /** custom jars added to this scope */
    val customJarsDecls = Nil
  }
}
```

## Project version

A part of the BuildInfo object is the project version. By default, an attempt is made to deduce it using git tags
of the workspace repository. If this fails (e.g. no git repository is present), the version is set to `0.1.0-SNAPSHOT`.
You can override this behaviour by passing the `--project-version` option to Scala CLI or by using a
`//> using projectVersion` directive.

Please note that only tags that follow the semantic versioning are taken into consideration.

Values available for project version configuration are:
- `git:tag` or `git`: use the latest stable git tag, if it is older than HEAD then try to increment it
    and add a suffix `-SNAPSHOT`, if no tag is available then use `0.1.0-SNAPSHOT`
- `git:dynver`: use the latest (stable or unstable) git tag, if it is older than HEAD then use the output of
    `-{distance from last tag}-g{shortened version of HEAD commit hash}-SNAPSHOT`, if no tag is available then use `0.1.0-SNAPSHOT`

The difference between stable and unstable tags are, that the latter can contain letters, e.g. `v0.1.0-RC1`.
It is also possible to specify the path to the repository, e.g. `git:tag:../my-repo`, `git:dynver:../my-repo`.



