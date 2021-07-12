---
title: Configuration
sidebar_position: 4
---

`scala` can be configured in several ways:
- on the command-line
- in a configuration file
- some options can also be specified directly in `.scala` and `.sc` files

Parameters on the command-line take precedence over parameters in configuration files or sources.
That way, you can quickly override parameters from the command-line.

Note that the configuration format and options in `.scala` files are likely
to evolve and be unified in the near future.

## Command-line

Pass `--help` to any sub-command of `scala` to list its options:
```text
$ scala --help
$ scala package --help
```

## In `.scala` files

Dependencies can be added right from `.scala` and `.sc` files, using the same
syntax as Ammonite and Metals worksheets:

```scala
import $ivy.`com.lihaoyi::upickle:1.4.0`
import ujson._
```

## Configuration files

Pass a file named `scala.conf`, or ending in `.scala.conf`, to specify options
via a [HOCON](https://github.com/lightbend/config) configuration file:
```hocon
scala {
  version = "2.13"
  options = [
    "-Xlint:infer-any"
  ]
}
jvm = "14"
repositories = [
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
]
dependencies = [
  "ai.kien::python-native-libs:0.0.0+13-f5d7089a-SNAPSHOT"
]
```
