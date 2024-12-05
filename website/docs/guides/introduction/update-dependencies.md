---
title: Updating dependencies
sidebar_position: 4
---

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

To check if dependencies in using directives are up-to-date, use `dependency-update` command:

```scala title=Hello.scala
//> using dep com.lihaoyi::os-lib:0.7.8
//> using dep com.lihaoyi::utest:0.7.10

object Hello extends App {
  println("Hello World")
}
```

<ChainedSnippets>

```bash
scala-cli --power dependency-update Hello.scala
```

```text
Updates
   * com.lihaoyi::os-lib:0.7.8 -> 0.11.3
   * com.lihaoyi::utest:0.7.10 -> 0.8.4
To update all dependencies run: 
    scala-cli dependency-update --all
```

</ChainedSnippets>

Passing `--all` to the `dependency-update` sub-command updates all dependencies in your sources.

<ChainedSnippets>

```bash
scala-cli --power dependency-update Hello.scala --all
```

```text
Updated dependency to: com.lihaoyi::os-lib:0.11.3
Updated dependency to: com.lihaoyi::utest:0.8.4
```

</ChainedSnippets>

