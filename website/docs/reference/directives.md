---
title: Directives
sidebar_position: 2
---

## using directives

### Compiler options

Add Scala compiler options

`using option `_option_

`using options `_option1_ _option2_ …

#### Examples
`using option -Xasync`

`using options -Xasync -Xfatal-warnings`

### Dependency

Adds dependencies

`using `_org_`:`name`:`ver

#### Examples
`using org.typelevel::cats-effect:3.2.9`

`using dev.zio::zio:1.0.12`

### Java home

Sets Java home used to run your application or tests

`using java-home `_path_ | `using javaHome `_path_

#### Examples
`using java-home "/Users/Me/jdks/11"`

### Java options

Adds Java options

`using java-opt `_options_ | `using javaOpt `_options_

#### Examples
`using javaOpt -Xmx2g -Dsomething=a`

### Platform

Set the default platform to Scala.JS or Scala Native

`using scala-js`|`scala-native`

#### Examples
`using scala-js`

`using scala-native`

### Repository

Adds a repository for dependency resolution

`using repository `_repository_

#### Examples
`using repository jitpack`

`using repository sonatype:snapshots`

`using repository https://maven-central.storage-download.googleapis.com/maven2`

### Scala version

Sets the default Scala version

`using scala `_version_

#### Examples
`using scala 3.0.2`

`using scala 2.13`

`using scala 2`


## require directives

### Platform

Require a Scala platform for the current file

`require `_platform_

#### Examples
`require scala-js`

`require scala-js scala-native`

`require jvm`

### Scala version

Require a Scala version for the current file

`require scala `_version_

#### Examples
`require scala 3`

`require scala 2.13`

`require scala 3.0.2`

