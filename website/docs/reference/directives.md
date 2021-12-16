---
title: Directives
sidebar_position: 2
---

## using directives

### Compiler options

Add Scala compiler options

`// using option `_option_

`// using options `_option1_, _option2_ …

#### Examples
`// using option "-Xasync"`

`// using options "-Xasync", "-Xfatal-warnings"`

### Compiler plugins

Adds compiler plugins

`using plugin `_org_`:`name`:`ver

#### Examples
`// using plugin "org.typelevel:::kind-projector:0.13.2"`

### Custom JAR

Manually add JAR(s) to the class path

// using jar _path_

// using jars _path1_, _path2_ …

#### Examples
`// using jar "/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar"`

### Dependency

Add dependencies

`// using lib "`_org_`:`name`:`ver"

#### Examples
`// using lib "org.scalatest::scalatest:3.2.10"`

`// using lib "org.scalameta::munit:0.7.29"`

### Java home

Sets Java home used to run your application or tests

`// using java-home `_path_

`// using javaHome `_path_

#### Examples
`// using java-home "/Users/Me/jdks/11"`

### Java options

Add Java options

`// using java-opt `_options_

`// using javaOpt `_options_

#### Examples
`// using javaOpt "-Xmx2g", "-Dsomething=a"`

### Java properties

Add Java properties

`// using javaProp_ `_key=value_
`// using javaProp_ `_key_

#### Examples
`// using javaProp "foo1=bar", "foo2"`

### Main class

Specify default main class

`// using main-class `_main class_

`// using mainClass `_main class_

#### Examples
`// using main-class "helloWorld"`

### Platform

Set the default platform to Scala.JS or Scala Native

`// using platform `(`jvm`|`scala-js`|`scala-native`)+

#### Examples
`// using platform "scala-js"`

`// using platform "jvm", "scala-native"`

### Repository

Add a repository for dependency resolution

`// using repository `_repository_

#### Examples
`// using repository "jitpack"`

`// using repository "sonatype:snapshots"`

`// using repository "https://maven-central.storage-download.googleapis.com/maven2"`

### Resource directories

Manually add a resource directory to the class path

`// using resourceDir `_path_

`// using resourceDirs `_path1_, _path2_ …

#### Examples
`// using resourceDir "./resources"`

### Scala JS options

Add Scala JS options


`// using jsVersion `_value_
`// using jsMode `_value_
`// using jsModuleKind `_value_
`// using jsCheckIr true|false`
`// using jsEmitSourceMaps true|false``
`// using jsDom true|false``


#### Examples
`// using jsModuleKind "common"`

### Scala Native options

Add Scala Native options

`// using nativeGc` _value_

`// using nativeVersion` _value_

`// using nativeCompile` _value1_, _value2_

`// using nativeLinking` _value1_, _value2_

#### Examples
`// using nativeVersion "0.4.0"`

### Scala version

Set the default Scala version

`// using scala `_version_+

#### Examples
`// using scala "3.0.2"`

`// using scala "2.13"`

`// using scala "2"`

`// using scala "2.13.6", "2.12.15"`

### Test framework

Set the test framework

`// using testFramework `_class_name_ | ``// using `test-framework` ``_class_name_

#### Examples
`// using testFramework "utest.runner.Framework"`


## target directives

### Platform

Require a Scala platform for the current file

`// using target.platform `_platform_

#### Examples
`// using target.platform "scala-js"`

`// using target.platform "scala-js", "scala-native"`

`// using target.platform "jvm"`

### Scala version

Require a Scala version for the current file

`// using target.scala `_version_

#### Examples
`// using target.scala "3"`

`// using target.scala.>= "2.13"`

`// using target.scala.< "3.0.2"`

### Scope

Require a scope for the current file

`// using target.scope `_scope_

#### Examples
`// using target.scope "test"`

