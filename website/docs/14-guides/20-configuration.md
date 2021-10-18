---
title: Configuration
---

`scala-cli` can be configured in several ways:
- on the command-line
- directly in `.scala` and `.sc` files

Parameters on the command-line take precedence over parameters in sources.
That way, you can quickly override parameters from the command-line.

Note that the configuration options and syntax in `.scala` files is likely
to evolve in the future.

## Command-line

Pass `--help` to any sub-command of `scala-cli` to list its options:
```bash
scala-cli --help
scala-cli package --help
```

For example, you can specify the Scala version, or add dependencies, on the command-line:
```bash
scala-cli --scala 3.0.0 Test.scala
scala-cli --dependency org.typelevel::cats-core:2.3.0 Test.scala
```

The reference documentation lists [all available options](17-reference/01-cli-options.md).

## Special imports

Dependencies can be added right from `.scala` and `.sc` files, using the same
syntax as Ammonite and Metals worksheets:

```scala
import $dep.`com.lihaoyi::upickle:1.4.0`
import $ivy.`com.lihaoyi::pprint:0.6.6`
import ujson._
```

Both `import $ivy` and `import $dep` are accepted, and are equivalent.

## Using directives

Scala CLI can be configured from `.scala` files. You can specify `using` directives at the
top of a `.scala` file, before any `package` or `import` statement.

```scala
using scala 2.13
using scala-js
using options -Xasync
```

The reference documentation lists [all available using directives](/reference/directives#using-directives).
