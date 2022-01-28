---
title: Scala Native
sidebar_position: 22
---

Scala Native works with Scala `3.1.x`, `2.13.x` and `2.12.x`. Scripts are unavailable for Scala `2.12.x`.

Scala Native requires the LLVM toolchain - see requirements on Scala Native website.
## Configuration

Enable Scala Native support by passing `--native` to `scala-cli`, such as:

```scala
scala-cli Test.scala --native
```

A Scala Native version can be set by passing `--native-version` with an argument:

```scala
scala-cli Test.scala --native --native-version 0.4.3
```

Platform compatibility and supported Scala language versions depend on this version.  
It is recommended to use the newest stable version.

## Dependencies

This section is currently a work in progress, but here are some initial notes:

- Beware platform dependencies
- `compile` / `run` / `test` / `package` should all work
