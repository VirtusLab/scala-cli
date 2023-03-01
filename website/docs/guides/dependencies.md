---
title: Managing dependencies
sidebar_position: 3
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

### Excluding Transitive Dependencies

To exclude a transitive dependency from a Scala CLI project use the `exclude` parameter:

- `exclude=org%%name` - for Scala modules
- `exclude=org%name` - for Java modules

It requires passing the organization and module name of the dependency to be excluded. For example, let's say you have
the following Scala code:

```scala title=Main.scala
//> using dep "com.lihaoyi::pprint:0.8.1"
object Main extends App {
  println("Hello")
}
```

If you want to compile it with the `pprint` library but exclude its `sourcecode` dependency, you can use
the `exclude` parameter as follows:

```scala title=Main.scala
//> using dep "com.lihaoyi::pprint:0.8.1,exclude=com.lihaoyi%%sourcecode"
object Main extends App {
  println("Hello")
}
```

To exclude Scala modules, you can also use a single `%` but with the full name of the module name, like this:

```scala title=Main.scala
//> using dep "com.lihaoyi::pprint:0.8.1,exclude=com.lihaoyi%sourcecode_3"
object Main extends App {
  println("Hello")
}
```


### Dependency classifiers

To specify a classifier of a dependency in a Scala CLI project, use the `classifier` parameter:

- `classifier={classifier_name}`

If you want to use the `pytorch` dependency with the classifier `linux-x86_64`, use the `classifier` parameter as follows:

```scala title=Main.scala
//> using dep "org.bytedeco:pytorch:1.12.1-1.5.8,classifier=linux-x86_64"
object Main extends App {
  println("Hello")
}
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