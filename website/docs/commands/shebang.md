---
title: Shebang
sidebar_position: 26
---

This command is equivalent to `run`, but it changes the way Scala CLI parses options (used to configure the tool) and
inputs (the sources of your project) in order to be compatible with `shebang` scripts.

The command `shebang` also allows script files to be executed even if they have no file extension,
provided they start with the [`shebang` header](../guides/shebang.md#shebang-script-headers).
Note that those files are always run as scripts even though they may contain e.g. valid `.scala` program.

Normally, inputs and `scala-cli` options can be mixed. Program arguments (to be passed to your app) have to be specified
after `--` (double dash) separator.

```bash ignore
scala-cli [command] [scala_cli_options | input]... -- [program_arguments]...
```

For the `shebang` command, only a single input can be set. All Scala CLI options must be set before
the input, while everything after the input is considered a program argument.

```bash ignore
scala-cli shebang [scala_cli_options]... input [program_arguments]...
```

More details can be found in [Shebang guide](/docs/guides/shebang).

