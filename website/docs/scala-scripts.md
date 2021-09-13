---
title: ScalaScripts
sidebar_position: 13
---

The Scala CLI can compile, run, package, etc. ScalaScripts sources.

Script can be easily run from one command line, you don't have to setup build tool for Scala.

## Quick Start

```bash
cat HelloScript.sc
# println("Hello world")

scala-cli run HelloScript.sc
# Hello world
```

## Self executable ScalaScript

You can define file with shebang header to self executable. It could be also run as a normal script.

```bash
cat HelloScript.sc
# #!/usr/bin/env scala-cli
# println("Hello world")

scala-cli run HelloScript.sc
# Hello world
./HelloScript.sc
# Hello world
```