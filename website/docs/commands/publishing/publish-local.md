---
title: Publish Local ⚡️
sidebar_position: 21
---

:::caution
The Publish Local command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

The `publish local` sub-command publishes a Scala CLI project in the local Ivy2
repository, just like how `sbt publishLocal` or `mill __.publishLocal` do. This
repository usually lives under `~/.ivy2/local`, and is taken into account most of
the time by most Scala tools when fetching artifacts.

## Usage

To publish locally a Scala CLI project, run

<ChainedSnippets>

```sh
scala-cli publish local .
```

```text
Publishing io.github.scala-cli:hello-scala-cli_3:0.1.0-SNAPSHOT
 ✔ Computed 10 checksums
 🚚 Wrote 15 files

 👀 Check results at
  ~/.ivy2/local/io.github.scala-cli/hello-scala-cli_3/0.1.0-SNAPSHOT/
```

</ChainedSnippets>

## Required settings

The `publish local` command needs the [same required settings as the `publish` command](publish.md#required-settings). Like for `publish`, Scala CLI might already be able to compute sensible defaults
for those.
