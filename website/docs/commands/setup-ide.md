---
title: IDE Setup
sidebar_position: 14
---

Whether it's VS Code or IntelliJ, Scala CLI can help you setup your IDE of choice by generating the files that are necessary for it, providing you with full-blown IDE support.

Using Scala CLI should be as simple as possible, so under the hood this command is run before every `run`, `compile`, or `test` command.
As a result, in most cases you don't need to run this command manually.

But if you want to, invoke `setup-ide` like:

```bash
scala-cli setup-ide . --scala 2.13
```

Keep in mind that if you change any of those options, you may need to restart your IDE, or re-import your project.

### IDE support internals

After invoking `setup-ide`, two files should be generated:
- `.bsp/scala-cli.json`
- `.scala/ide-options-v2.json`

The first file is specifically created for Build Server Protocol (BSP) support in your IDE.
BSP is supported by VS Code (via the Metals extension) and IntelliJ (with the Scala plugin), and defines the way in which IDEs gather information about the project you are working on.

The second file is designed to store settings used by the Scala CLI while generating BSP configuration.
This includes all options, such as the Scala version, custom arguments, and more, but fortunately you shouldn't need to edit it.
