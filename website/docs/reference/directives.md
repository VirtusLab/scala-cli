---
title: Directives
sidebar_position: 2
---

## using directives

### Compiler options

Add Scala compiler options

`//> using option `_option_

`//> using options `_option1_, _option2_ …

#### Examples
`//> using option "-Xasync"`

`//> using options "-Xasync", "-Xfatal-warnings"`

### Compiler plugins

Adds compiler plugins

`using plugin `_org_`:`name`:`ver

#### Examples
`//> using plugin "org.typelevel:::kind-projector:0.13.2"`

### Custom JAR

Manually add JAR(s) to the class path

`//> using jar <path>

//> using jars <path1>, <path2>`

#### Examples
`//> using jar "/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar"`

### Dependency

Add dependencies

`//> using lib "`_org_`:`name`:`ver"

#### Examples
`//> using lib "org.scalatest::scalatest:3.2.10"`

`//> using lib "org.scalameta::munit:0.7.29"`

`//> using lib "tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar"`

### Java home

Sets Java home used to run your application or tests

`//> using java-home `_path_

`//> using javaHome `_path_

#### Examples
`//> using java-home "/Users/Me/jdks/11"`

### Java options

Add Java options

`//> using java-opt `_options_

`//> using javaOpt `_options_

#### Examples
`//> using javaOpt "-Xmx2g", "-Dsomething=a"`

### Java properties

Add Java properties

`//> using javaProp_ `_key=value_
`//> using javaProp_ `_key_

#### Examples
`//> using javaProp "foo1=bar", "foo2"`

### Location of clang

Path to the Clang command

`//> using nativeClang "path"

//> using native-clang "path"`

#### Examples
`//> using nativeClang "/usr/bin/clang"`

`//> using native-clang "/usr/bin/clang"`

### Location of clang++

TODO no arg

`//> using nativeClangPP "path"

//> using native-clang-pp "path"`

#### Examples
`//> using nativeClangPP "/usr/bin/clang++"`

`//> using native-clang-pp "/usr/bin/clang++"`

### Main class

Specify default main class

`//> using main-class `_main class_

`//> using mainClass `_main class_

#### Examples
`//> using main-class "helloWorld"`

### Platform

Set the default platform to Scala.js or Scala Native

`//> using platform `(`jvm`|`scala-js`|`scala-native`)+

#### Examples
`//> using platform "scala-js"`

`//> using platform "jvm", "scala-native"`

### Publish

Set parameters for publishing

`//> using publish.organization `"value"
`//> using publish.moduleName `"value"
`//> using publish.version `"value"


#### Examples
`//> using publish.organization "io.github.myself"`

`//> using publish.moduleName "my-library"`

`//> using publish.version "0.1.1"`

### Repository

Add a repository for dependency resolution

`//> using repository `_repository_

#### Examples
`//> using repository "jitpack"`

`//> using repository "sonatype:snapshots"`

`//> using repository "https://maven-central.storage-download.googleapis.com/maven2"`

### Resource directories

Manually add a resource directory to the class path

`//> using resourceDir `_path_

`//> using resourceDirs `_path1_, _path2_ …

#### Examples
`//> using resourceDir "./resources"`

### Scala Native GC

TODO no arg

`//> using nativeGC "gc"

//> using native-g-c "gc"`

#### Examples
`//> using nativeGC "none"`

`//> using native-g-c "boehm"`

### Scala Native Mode

Set Scala Native compilation mode

`//> using nativeMode "mode"

//> using native-mode "mode"`

#### Examples
`//> using nativeMode "release"`

`//> using native-mode "release-fast"`

### Scala Native Version

Set the Scala Native version

`//> using nativeVersion "version"

//> using native-version "version"`

#### Examples
`//> using nativeVersion "0.4.0"`

`//> using native-version "0.4.1"`

### Scala Native compile

List of compile options

`//> using nativeCompile "option0"

//> using native-compile "option0, option1"`

#### Examples
`//> using nativeCompile List("TODO", "TODO", "TODO")`

`//> using native-compile List("TODO", "TODO", "TODO")`

### Scala Native linking

Extra options passed to `clang` verbatim during linking

`//> using nativeLinking "option0"

//> using native-linking "option0, option1"`

#### Examples
`//> using nativeLinking List("TODO", "TODO", "TODO")`

`//> using native-linking List("TODO", "TODO", "TODO")`

### Scala version

Set the default Scala version

`//> using scala <version>

//> using scala <base-version>, <cross-version>

//> using scala <base-version>, <cross-version1>, <cross-version2>`

#### Examples
`//> using scala "3.0.2"`

`//> using scala 3`

`//> using scala 3.1`

`//> using scala "2.13"`

`//> using scala "2"`

`//> using scala "2.13.6", "2.12.15"`

### Scala.js AvoidClasses

Avoid class'es when using functions and prototypes has the same observable semantics.

`//> using jsAvoidClasses [true|false]

//> using js-avoid-classes [true|false]`

#### Examples
`//> using jsAvoidClasses`

`//> using js-avoid-classes false`

### Scala.js AvoidLetsAndConsts

Avoid lets and consts when using vars has the same observable semantics.

`//> using jsAvoidLetsAndConsts [true|false]

//> using js-avoid-lets-and-consts [true|false]`

#### Examples
`//> using jsAvoidLetsAndConsts`

`//> using js-avoid-lets-and-consts false`

### Scala.js CheckIr

TODO nod help

`//> using jsCheckIr [true|false]

//> using js-check-ir [true|false]`

#### Examples
`//> using jsCheckIr`

`//> using js-check-ir false`

### Scala.js Dom

Enable jsdom

`//> using jsDom [true|false]

//> using js-dom [true|false]`

#### Examples
`//> using jsDom`

`//> using js-dom false`

### Scala.js EmitSourceMaps

Emit source maps

`//> using jsEmitSourceMaps [true|false]

//> using js-emit-source-maps [true|false]`

#### Examples
`//> using jsEmitSourceMaps`

`//> using js-emit-source-maps false`

### Scala.js EsVersionStr

TODO no arg

`//> using jsEsVersionStr "version"

//> using js-es-version-str "version"`

#### Examples
`//> using jsEsVersionStr "TODO"`

`//> using js-es-version-str "TODO"`

### Scala.js Header

A header that will be added at the top of generated .js files

`//> using jsHeader "header"

//> using js-header "header"`

#### Examples
`//> using jsHeader "TODO"`

`//> using js-header "TODO"`

### Scala.js Mode

The Scala.js mode, either `dev` or `release`

`//> using jsMode "mode"

//> using js-mode "mode"`

#### Examples
`//> using jsMode "TODO"`

`//> using js-mode "TODO"`

### Scala.js ModuleKind

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

`//> using jsModuleKind "kind"

//> using js-module-kind "kind"`

#### Examples
`//> using jsModuleKind "TODO"`

`//> using js-module-kind "TODO"`

### Scala.js SmallModuleForPackage

Create as many small modules as possible for the classes in the passed packages and their subpackages.

`//> using jsSmallModuleForPackage "value0, value1"

//> using js-small-module-for-package "value0"`

#### Examples
`//> using jsSmallModuleForPackage List("pckA", "pckB", "packC")`

`//> using js-small-module-for-package List("pckA", "pckB", "packC")`

### Scala.js Version

The Scala.js version

`//> using jsVersion "version"

//> using js-version "version"`

#### Examples
`//> using jsVersion "3.2.1"`

`//> using js-version "TODO"`

### Test framework

Set the test framework

`using testFramework <class_name> 

using test-framework <class_name>`

#### Examples
`//> using testFramework "utest.runner.Framework"`


## target directives

### Platform

Require a Scala platform for the current file

`//> using target.platform `_platform_

#### Examples
`//> using target.platform "scala-js"`

`//> using target.platform "scala-js", "scala-native"`

`//> using target.platform "jvm"`

### Scala version

Require a Scala version for the current file

`//> using target.scala `_version_

#### Examples
`//> using target.scala "3"`

`//> using target.scala.>= "2.13"`

`//> using target.scala.< "3.0.2"`

### Scope

Require a scope for the current file

`//> using target.scope `_scope_

#### Examples
`//> using target.scope "test"`

