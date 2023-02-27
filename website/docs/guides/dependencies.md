---
title: Managing dependencies
sidebar_position: 9
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

# Managing dependencies

## Dependency syntax

Dependencies are declared in Scala CLI according to the following format:

```text
groupID:artifactID:revision
```

This is similar to how you declare dependencies in SBT with the `%` character.
For example:

```text
org.scala-lang.modules:scala-parallel-collections_2.13:1.0.4
```

You can also skip explicitly stating the Scala version in the artifact name by repeating the `:` character after
the `groupID` (similarly to how you can do the same with `%%` in SBT). This is just a shortcut, Scala CLI will still add
the Scala version for you when fetching the dependency. Also, this only applies to Scala dependencies.

```text
org.scala-lang.modules::scala-parallel-collections:1.0.4
```

Java and other non-scala dependencies follow the same syntax (without the `::` for implicit Scala version, of course).
For example:
```text
org.postgresql:postgresql:42.2.8
```

## Specifying dependencies from the command line

You can add dependencies on the command line, with the `--dependency` option:

```scala title=Sample.sc
println("Hello")
```

```bash
scala-cli compile Sample.sc \
  --dependency org.scala-lang.modules::scala-parallel-collections:1.0.4
```

You can also add a URL fallback for a JAR dependency, if it can't be fetched otherwise:

```bash ignore
scala-cli compile Sample.sc \
  -- dependency "org::name::version,url=https://url-to-the-jar"
```

Note that `--dependency` is only meant as a convenience. You should favor adding dependencies in the sources themselves
via [using directives](/docs/guides/configuration.md#special-imports). However, the `--dependency` CLI option takes
precedence over `using` directives, so it can be used to override a `using` directive, such as when you want to work
with a different dependency version.

You can also add repositories on the command-line, via `--repository`:

```bash ignore
scala-cli compile Sample.sc \
  --dependency com.pany::util:33.1.0 --repo https://artifacts.pany.com/maven
```

Lastly, you can also add simple JAR files as dependencies with `--jar`:

```bash ignore
scala-cli compile Sample.sc --jar /path/to/library.jar
```