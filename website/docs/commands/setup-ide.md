---
title: Setup IDE
sidebar_position: 15
---

Scala CLI can help you setup your IDE of choice, whether it is VS Code or IntelliJ,
by generating files necessary for it to provide you with full-blown support.

Using Scala CLI should be as simple as possible,
therefore under the hood this command is also run before first `run`, `compile` or `test` commands.
As a result in most cases you do not need to run this command manually.

You can invoke `setup-ide` like:

```bash
scala-cli setup-ide . --scala 2.13
```

Please keep in mind that if you change any of those options it may be required to restart or reimport
the project within IDE.

### IDE support internals

After invoking `setup-ide` two files should be generated:
- `.bsp/scala-cli.json`
- `.scala/ide-options.json`

First one is a file specifically created for Build Server Protocol (BSP) support in your IDE.
This protocol is supported by two most popular IDEs: VS Code (with Metals extension) and IntelliJ (with Scala plugin)
and defines a way in which IDEs gather information about the project you are working on.

Second file is designed to store settings used by Scala CLI while generating BSP configuration.
This covers all options like Scala version, custom arguments and more but fortunately you shouldn't
be forced to edit it.
