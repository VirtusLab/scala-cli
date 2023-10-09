---
title: Fix ⚡️
sidebar_position: 28
---

:::caution
The Fix command is experimental and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

The `fix` command scans your project for `using` directives and extracts them into the `project.scala` file placed in the project root directory.
This allows to fix warnings tied to having `using` directives present in multiple files.
Additionally, `fix` will format the result file, thus allowing to quickly spot configuration options that may be duplicated.

Files containing `using target` directives, e.g. `//> using target.scala 3.0.0` will not be changed by `fix`.

The command respects the original scope of each extracted directive and will transform them into their `test.` equivalent if needed.
