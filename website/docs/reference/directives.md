---
title: Directives
sidebar_position: 2
---

## using directives

### BuildInfo

Generate BuildInfo for project

`//> using buildInfo`

#### Examples
`//> using buildInfo`

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

### Compute Version

Method used to compute the version for BuildInfo

`//> using computeVersion` _method_

#### Examples
`//> using computeVersion git`

`//> using computeVersion git:tag`

`//> using computeVersion git:dynver`

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

### Dependency

Add dependencies

`//> using dep` _org_`:`name`:`ver

#### Examples
`//> using dep com.lihaoyi::os-lib:0.9.1`

`//> using test.dep org.scalatest::scalatest:3.2.10`

`//> using test.dep org.scalameta::munit:0.7.29`

`//> using dep tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar`

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

### Javac options

Add Javac options which will be passed when compiling sources.

`//> using javacOpt` _options_

#### Examples
`//> using javacOpt -source 1.8 -target 1.8`

`//> using test.javacOpt -source 1.8 -target 1.8`

### Main class

Specify default main class

`//> using mainClass` _main-class_

#### Examples
`//> using mainClass HelloWorld`

### ObjectWrapper

Set the default code wrapper for scripts to object wrapper

`//> using objectWrapper`

#### Examples
`//> using objectWrapper`

### Packaging

Set parameters for packaging

`//> using packaging.packageType` _package-type_

`//> using packaging.output` _destination-path_



#### Examples
`//> using packaging.packageType assembly`

`//> using packaging.output foo`

`//> using packaging.provided org.apache.spark::spark-sql`

`//> using packaging.dockerFrom openjdk:11`

`//> using packaging.graalvmArgs --no-fallback`

### Platform

Set the default platform to Scala.js or Scala Native

`//> using platform` (`jvm`|`scala-js`|`js`|`scala-native`|`native`)+

#### Examples
`//> using platform scala-js`

`//> using platform jvm scala-native`

### Publish

Set parameters for publishing

`//> using publish.organization` value

`//> using publish.name` value

`//> using publish.version` value



#### Examples
`//> using publish.organization io.github.myself`

`//> using publish.name my-library`

`//> using publish.version 0.1.1`

### Publish (CI)

Set CI parameters for publishing

`//> using publish.ci.computeVersion` value

`//> using publish.ci.repository` value

`//> using publish.ci.secretKey` value



#### Examples
`//> using publish.ci.computeVersion git:tag`

`//> using publish.ci.repository central-s01`

`//> using publish.ci.secretKey env:PUBLISH_SECRET_KEY`

### Publish (contextual)

Set contextual parameters for publishing

`//> using publish.computeVersion` value

`//> using publish.repository` value

`//> using publish.secretKey` value

`//> using publish.doc` boolean



#### Examples
`//> using publish.computeVersion git:tag`

`//> using publish.repository central-s01`

`//> using publish.secretKey env:PUBLISH_SECRET_KEY`

`//> using publish.doc false`

### Python

Enable Python support

`//> using python`

#### Examples
`//> using python`

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

### Scala version

Set the default Scala version

`//> using scala` _version_+

#### Examples
`//> using scala 3.0.2`

`//> using scala 2.13`

`//> using scala 2`

`//> using scala 2.13.6, 2.12.16`

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


## target directives

### Platform

Require a Scala platform for the current file

`//> using target.platform` _platform_

#### Examples
`//> using target.platform scala-js`

`//> using target.platform scala-js, scala-native`

`//> using target.platform jvm`

### Scala version

Require a Scala version for the current file

`//> using target.scala` _version_

#### Examples
`//> using target.scala 3`

### Scala version bounds

Require a Scala version for the current file

`//> using target.scala.>=` _version_

#### Examples
`//> using target.scala.>= 2.13`

`//> using target.scala.< 3.0.2`

### Scope

Require a scope for the current file

`//> using target.scope` _scope_

#### Examples
`//> using target.scope test`

