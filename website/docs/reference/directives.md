---
title: Directives
sidebar_position: 2
---

## using directives

### Benchmarking options

Add benchmarking options

`//> using jmh` _value_

`//> using jmhVersion` _value_

#### Examples
`//> using jmh`

`//> using jmh true`

`//> using jmhVersion 1.37`

### BuildInfo

Generate BuildInfo for project

`//> using buildInfo`

#### Examples
`//> using buildInfo`

### Compiler options

Add Scala compiler options

`//> using scalacOption` _option_

`//> using option` _option_

`//> using scalacOptions` _option1_ _option2_ …

`//> using options` _option1_ _option2_ …

`//> using test.scalacOption` _option_

`//> using test.option` _option_

`//> using test.scalacOptions` _option1_ _option2_ …

`//> using test.options` _option1_ _option2_ …



#### Examples
`//> using option -Xasync`

`//> using options -Xasync -Xfatal-warnings`

`//> using test.option -Xasync`

`//> using test.options -Xasync -Xfatal-warnings`

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

`//> using test.jar` _path_

`//> using test.jars` _path1_ _path2_ …

`//> using source.jar` _path_

`//> using source.jars` _path1_ _path2_ …

`//> using test.source.jar` _path_

`//> using test.source.jars` _path1_ _path2_ …


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

`//> using deps` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using dependencies` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using test.dep` _org_`:`name`:`ver

`//> using test.deps` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using test.dependencies` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using compileOnly.dep` _org_`:`name`:`ver

`//> using compileOnly.deps` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using compileOnly.dependencies` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using scalafix.dep` _org_`:`name`:`ver

`//> using scalafix.deps` _org_`:`name`:`ver _org_`:`name`:`ver

`//> using scalafix.dependencies` _org_`:`name`:`ver _org_`:`name`:`ver


#### Examples
`//> using dep com.lihaoyi::os-lib:0.9.1`

`//> using dep tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar`

`//> using test.dep org.scalatest::scalatest:3.2.10`

`//> using test.dep org.scalameta::munit:0.7.29`

`//> using compileOnly.dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.23.2`

`//> using scalafix.dep com.github.xuwei-k::scalafix-rules:0.5.1`

### Exclude sources

Exclude sources from the project

`//> using exclude` _pattern_

`//> using exclude` _pattern1_ _pattern2_ …


#### Examples
`//> using exclude utils.scala`

`//> using exclude examples/* */resources/*`

`//> using exclude *.sc`

### JVM version

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`. scala-cli uses [coursier](https://get-coursier.io/) to fetch JVMs, so you can use `cs java --available` to list the available JVMs.

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
`//> using javaOptions` _options_`

`//> using test.javaOpt` _options_
`//> using test.javaOptions` _options_`


#### Examples
`//> using javaOpt -Xmx2g -Dsomething=a`

`//> using test.javaOpt -Dsomething=a`

### Java properties

Add Java properties

`//> using javaProp` _key=value_

`//> using javaProp` _key_

`//> using test.javaProp` _key=value_

`//> using test.javaProp` _key_


#### Examples
`//> using javaProp foo1=bar foo2`

`//> using test.javaProp foo3=bar foo4`

### Javac options

Add Javac options which will be passed when compiling sources.

`//> using javacOpt` _options_

`//> using javacOptions` _options_

`//> using test.javacOpt` _options_

`//> using test.javacOptions` _options_


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

`//> using packaging.provided` _module_

`//> using packaging.graalvmArgs` _args_

`//> using packaging.dockerFrom` _base-docker-image_

`//> using packaging.dockerImageTag` _image-tag_

`//> using packaging.dockerImageRegistry` _image-registry_

`//> using packaging.dockerImageRepository` _image-repository_

`//> using packaging.dockerCmd` _docker-command_



#### Examples
`//> using packaging.packageType assembly`

`//> using packaging.output foo`

`//> using packaging.provided org.apache.spark::spark-sql`

`//> using packaging.graalvmArgs --no-fallback`

`//> using packaging.dockerFrom openjdk:11`

`//> using packaging.dockerImageTag 1.0.0`

`//> using packaging.dockerImageRegistry virtuslab`

`//> using packaging.dockerImageRepository scala-cli`

`//> using packaging.dockerCmd sh`

`//> using packaging.dockerCmd node`

### Platform

Set the default platform to Scala.js or Scala Native

`//> using platform` (`jvm`|`scala-js`|`js`|`scala-native`|`native`)+

`//> using platforms` (`jvm`|`scala-js`|`js`|`scala-native`|`native`)+


#### Examples
`//> using platform scala-js`

`//> using platforms jvm scala-native`

### Publish

Set parameters for publishing

`//> using publish.organization` value

`//> using publish.name` value

`//> using publish.moduleName` value

`//> using publish.version` value

`//> using publish.url` value

`//> using publish.license` value

`//> using publish.vcs` value

`//> using publish.scm` value

`//> using publish.versionControl` value

`//> using publish.description` value

`//> using publish.developer` value

`//> using publish.developers` value1 value2

`//> using publish.scalaVersionSuffix` value

`//> using publish.scalaPlatformSuffix` value



#### Examples
`//> using publish.organization io.github.myself`

`//> using publish.name my-library`

`//> using publish.moduleName scala-cli_3`

`//> using publish.version 0.1.1`

`//> using publish.url https://github.com/VirtusLab/scala-cli`

`//> using publish.license MIT`

`//> using publish.vcs https://github.com/VirtusLab/scala-cli.git`

`//> using publish.vcs github:VirtusLab/scala-cli`

`//> using publish.description "Lorem ipsum dolor sit amet"`

`//> using publish.developer alexme|Alex Me|https://alex.me`

`//> using publish.developers alexme|Alex Me|https://alex.me Gedochao|Gedo Chao|https://github.com/Gedochao`

`//> using publish.scalaVersionSuffix _2.13`

`//> using publish.scalaVersionSuffix _3`

`//> using publish.scalaPlatformSuffix _sjs1`

`//> using publish.scalaPlatformSuffix _native0.4`

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

`//> using test.resourceDir` _path_

`//> using test.resourceDirs` _path1_ _path2_ …



#### Examples
`//> using resourceDir ./resources`

`//> using test.resourceDir ./resources`

### Scala Native options

Add Scala Native options

`//> using nativeGc` **immix**_|commix|boehm|none_

`//> using nativeMode` **debug**_|release-fast|release-size|release-full_

`//> using nativeLto` **none**_|full|thin_

`//> using nativeVersion` _value_

`//> using nativeCompile` _value1_ _value2_ …

`//> using nativeLinking` _value1_ _value2_ …

`//> using nativeClang` _value_

`//> using nativeClangPP` _value_

`//> using nativeClangPp` _value_

`//> using nativeEmbedResources` _true|false_

`//> using nativeEmbedResources`

`//> using nativeTarget` _application|library-dynamic|library-static_

`//> using nativeMultithreading` _true|false_

`//> using nativeMultithreading`

#### Examples
`//> using nativeGc immix`

`//> using nativeMode debug`

`//> using nativeLto full`

`//> using nativeVersion 0.5.7`

`//> using nativeCompile -flto=thin`

`//> using nativeLinking -flto=thin`

`//> using nativeClang ./clang`

`//> using nativeClangPP ./clang++`

`//> using nativeEmbedResources`

`//> using nativeEmbedResources true`

`//> using nativeTarget library-dynamic`

`//> using nativeMultithreading`

`//> using nativeMultithreading false`

### Scala version

Set the default Scala version

`//> using scala` _version_+

#### Examples
`//> using scala 3.0.2`

`//> using scala 2.13`

`//> using scala 2`

`//> using scala 2.13.6 2.12.16`

### Scala.js options

Add Scala.js options


`//> using jsVersion` _value_

`//> using jsMode` _value_

`//> using jsNoOpt` _true|false_

`//> using jsNoOpt`

`//> using jsModuleKind` _value_

`//> using jsCheckIr` _true|false_

`//> using jsCheckIr`

`//> using jsEmitSourceMaps` _true|false_

`//> using jsEmitSourceMaps`

`//> using jsEsModuleImportMap` _value_

`//> using jsSmallModuleForPackage` _value1_ _value2_ …

`//> using jsDom` _true|false_

`//> using jsDom`

`//> using jsHeader` _value_

`//> using jsAllowBigIntsForLongs` _true|false_

`//> using jsAllowBigIntsForLongs`

`//> using jsAvoidClasses` _true|false_

`//> using jsAvoidClasses`

`//> using jsAvoidLetsAndConsts` _true|false_

`//> using jsAvoidLetsAndConsts`

`//> using jsModuleSplitStyleStr` _value_

`//> using jsEsVersionStr` _value_
    
`//> using jsEmitWasm` _true|false_

`//> using jsEmitWasm`


#### Examples
`//> using jsVersion 1.18.2`

`//> using jsMode mode`

`//> using jsNoOpt`

`//> using jsModuleKind common`

`//> using jsCheckIr`

`//> using jsEmitSourceMaps`

`//> using jsEsModuleImportMap importmap.json`

`//> using jsSmallModuleForPackage test`

`//> using jsDom`

`//> using jsHeader "#!/usr/bin/env node
"`

`//> using jsAllowBigIntsForLongs`

`//> using jsAvoidClasses`

`//> using jsAvoidLetsAndConsts`

`//> using jsModuleSplitStyleStr smallestmodules`

`//> using jsEsVersionStr es2017`

`//> using jsEmitWasm`

### Test framework

Set the test framework

`//> using testFramework`  _class-name_

#### Examples
`//> using testFramework utest.runner.Framework`

### Toolkit

Use a toolkit as dependency (not supported in Scala 2.12), 'default' version for Scala toolkit: 0.7.0, 'default' version for typelevel toolkit: 0.1.29

`//> using toolkit` _version_

//> using test.toolkit` _version_


#### Examples
`//> using toolkit 0.7.0`

`//> using toolkit default`

`//> using test.toolkit default`


## target directives

### Platform

Require a Scala platform for the current file

`//> using target.platform` _platform_

#### Examples
`//> using target.platform scala-js`

`//> using target.platform scala-js scala-native`

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

