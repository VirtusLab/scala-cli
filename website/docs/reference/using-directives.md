---
title: Using directives
sidebar_position: 2
---

### Dependency

Adds dependencies

`using `_org_`:`name`:`ver

#### Examples
`using org.typelevel::cats-effect:3.2.9`

`using dev.zio::zio:1.0.12`

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

