---
title: Shebang
sidebar_position: 30
---

This command is equivalent to `run`, but it changes the way Scala CLI parses arguments in order to be
compatible with shebang scripts.

Normally, inputs and `scala-cli` options can be mixed. Program arguments have to be specified after `--`

```bash ignore
scala-cli [command] [scala_cli_options | input]... -- [program_arguments]...
```

For the `shebang` command, only a single input can be set. All Scala CLI options must be set before
the input, while everything after the input is considered a program argument.

```bash ignore
scala-cli shebang [scala_cli_options]... input [program_arguments]...
```

More details can be found in [Shebang guide](../guides/shebang).

