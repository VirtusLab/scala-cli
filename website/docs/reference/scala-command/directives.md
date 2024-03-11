---
title: Directives
sidebar_position: 2
---

**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**

This document is a specification of the `scala` runner.
For now it uses documentation specific to Scala CLI but at some point it may be refactored to provide more abstract documentation.
Documentation is split into sections in the spirit of RFC keywords (`MUST`, `SHOULD`).

## MUST have directives:

### Compiler options

Add Scala compiler options

`//> using option` _option_

`//> using options` _option1_ _option2_ …

#### Examples
`//> using option -Xasync`

`//> using test.option -Xasync`

`//> using options -Xasync -Xfatal-warnings`

### Compiler plugins

Adds compiler plugins

`using plugin` _org_`:`_name_`:`_ver_

#### Examples
`//> using plugin org.typelevel:::kind-projector:0.13.2`

### Dependency

Add dependencies

`//> using dep` _org_`:`name`:`ver

#### Examples
`//> using dep com.lihaoyi::os-lib:0.9.1`

`//> using test.dep org.scalatest::scalatest:3.2.10`

`//> using test.dep org.scalameta::munit:0.7.29`

`//> using dep tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar`

### Java options

Add Java options which will be passed when running an application.

`//> using javaOpt` _options_

#### Examples
`//> using javaOpt -Xmx2g, -Dsomething=a`

`//> using test.javaOpt -Dsomething=a`

### Java properties

Add Java properties

`//> using javaProp` _key=value_

`//> using javaProp` _key_


#### Examples
`//> using javaProp foo1=bar, foo2`

`//> using test.javaProp foo3=bar foo4`

### Main class

Specify default main class

`//> using mainClass` _main-class_

#### Examples
`//> using mainClass HelloWorld`

### Scala version

Set the default Scala version

`//> using scala` _version_+

#### Examples
`//> using scala 3.0.2`

`//> using scala 2.13`

`//> using scala 2`

`//> using scala 2.13.6, 2.12.16`

## SHOULD have directives:

### Custom JAR

Manually add JAR(s) to the class path

`//> using jar` _path_

`//> using jars` _path1_ _path2_ …


#### Examples
`//> using jar /Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar`

`//> using test.jar /Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar`

`//> using sourceJar /path/to/custom-jar-sources.jar`

`//> using sourceJars /path/to/custom-jar-sources.jar /path/to/another-jar-sources.jar`

`//> using test.sourceJar /path/to/test-custom-jar-sources.jar`

### Custom sources

Manually add sources to the project. Does not support chaining, sources are added only once, not recursively.

`//> using file` _path_

`//> using files` _path1_ _path2_ …


#### Examples
`//> using file utils.scala`

### Exclude sources

Exclude sources from the project

`//> using exclude` _pattern_

`//> using exclude` _pattern1_ _pattern2_ …


#### Examples
`//> using exclude utils.scala`

`//> using exclude "examples/*" "*/resources/*"`

`//> using exclude "*.sc"`

### JVM version

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

`//> using jvm` _value_

#### Examples
`//> using jvm 11`

`//> using jvm adopt:11`

`//> using jvm graalvm:21`

### Java home

Sets Java home used to run your application or tests

`//> using javaHome` _path_

#### Examples
`//> using javaHome /Users/Me/jdks/11`

### Javac options

Add Javac options which will be passed when compiling sources.

`//> using javacOpt` _options_

#### Examples
`//> using javacOpt -source 1.8 -target 1.8`

`//> using test.javacOpt -source 1.8 -target 1.8`

### Platform

Set the default platform to Scala.js or Scala Native

`//> using platform` (`jvm`|`scala-js`|`js`|`scala-native`|`native`)+

#### Examples
`//> using platform scala-js`

`//> using platform jvm scala-native`

### Repository

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

`//> using repository` _repository_

#### Examples
`//> using repository jitpack`

`//> using repository sonatype:snapshots`

`//> using repository m2Local`

`//> using repository https://maven-central.storage-download.googleapis.com/maven2`

### Resource directories

Manually add a resource directory to the class path

`//> using resourceDir` _path_

`//> using resourceDirs` _path1_ _path2_ …

#### Examples
`//> using resourceDir ./resources`

`//> using test.resourceDir ./resources`

### Scala Native options

Add Scala Native options

`//> using nativeGc` _value_

`//> using nativeMode` _value_

`//> using nativeLto` _value_

`//> using nativeVersion` _value_

`//> using nativeCompile` _value1_ _value2_ …

`//> using nativeLinking` _value1_ _value2_ …

`//> using nativeClang` _value_

`//> using nativeClangPP` _value_

`//> using nativeEmbedResources` _true|false_

`//> using nativeTarget` _application|library-dynamic|library-static_

#### Examples
`//> using nativeVersion 0.4.0`

### Scala.js options

Add Scala.js options


`//> using jsVersion` _value_

`//> using jsMode` _value_

`//> using jsNoOpt` _true|false_

`//> using jsModuleKind` _value_

`//> using jsSmallModuleForPackage` _value1_ _value2_ …

`//> using jsCheckIr` _true|false_

`//> using jsEmitSourceMaps` _true|false_

`//> using jsDom` _true|false_

`//> using jsHeader` _value_

`//> using jsAllowBigIntsForLongs` _true|false_

`//> using jsAvoidClasses` _true|false_

`//> using jsAvoidLetsAndConsts` _true|false_

`//> using jsModuleSplitStyleStr` _value_

`//> using jsEsVersionStr` _value_

`//> using jsEsModuleImportMap` _value_


#### Examples
`//> using jsModuleKind common`

### Test framework

Set the test framework

`//> using testFramework`  _class-name_

#### Examples
`//> using testFramework utest.runner.Framework`

### Toolkit

Use a toolkit as dependency (not supported in Scala 2.12), 'default' version for Scala toolkit: 0.2.1, 'default' version for typelevel toolkit: 0.1.20

`//> using toolkit` _version_

#### Examples
`//> using toolkit 0.1.0`

`//> using toolkit default`

`//> using test.toolkit default`

