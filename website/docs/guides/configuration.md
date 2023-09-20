---
title: Configuration
sidebar_position: 2
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

Scala CLI can be configured in two ways:
- on the command-line
- directly in `.scala` and `.sc` files

Parameters on the command line take precedence over parameters in sources.
That way, you can quickly override parameters from the command line.

:::warning
The configuration options and syntax in `.scala` (and `.sc`) files is likely to evolve in the future.
:::

## Command-line

Pass `--help` to any sub-command of Scala CLI to list its options:
```bash
scala-cli --help
scala-cli --power package --help
```

As an example of command line configuration, one thing you can do with Scala CLI command line options is to specify the Scala version:
```scala title=Test.scala
@main def test = println("test")
```

<ChainedSnippets>

```bash
scala-cli --scala 3.0.0 Test.scala
```

```text
test
```

</ChainedSnippets>

Another thing you can do is to specify dependencies:

<ChainedSnippets>

```bash
scala-cli --dependency org.typelevel::cats-core:2.10.0 Test.scala
```

```text
test
```

</ChainedSnippets>

The reference documentation lists [all of the available options](/docs/reference/cli-options.md).


## In .scala and .sc files

Configuration information can also be put in `.scala` and `.sc` files using special imports, and the `using` directive.

### Using directives

Scala CLI can be configured inside `.scala` files.
This is achieved by specifying `using` directives inside comments at the top of a `.scala` file, 
before any `package` or `import` statement:

```scala compile
//> using scala 2.13
//> using platform scala-js
//> using options -Xasync

// package and import statements follow here ...
```

The reference documentation lists [all available using directives](/docs/reference/directives.md#using-directives).

Also, there are some directives which only target tests, like `using test.dep`. 
Those can be useful when defining configuration specific to your test runs.
```scala compile
//> using test.dep com.lihaoyi::utest::0.8.1
```

More details can be found in the [`using` directives guide](./using-directives.md#directives-with-a-test-scope-equivalent).

### Special imports

Dependencies can be added right from `.scala` and `.sc` files with [`using` directives](#using-directives):

```scala compile
//> using dep com.lihaoyi::upickle::3.1.2
//> using dep com.lihaoyi::pprint::0.8.1
import ujson._
```

Both `import $ivy` and `import $dep` are not supported.
