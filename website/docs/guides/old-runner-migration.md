---
title: Migrating from the old Scala runner
sidebar_position: 15
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

# Migrating from the old `scala` runner

As of [SIP-46](https://github.com/scala/improvement-proposals/pull/46), Scala CLI has been accepted as the new `scala`
command.

In that context, the purpose of this guide is to highlight the key differences between the old `scala` script
and Scala CLI to make the migration as smooth as possible for users.

:::note
If you are looking for an overview of Scala CLI basics, refer to [the Basics page](../commands/basics.md).
If you merely want to get started with Scala CLI, you might want to first look
at [the Getting started page](../getting_started.md).
:::

## How to test Scala CLI as the new `scala` command?

There is a dedicated `scala-experimental` distribution of Scala CLI, which can install it as `scala` on your machine.
For instructions on how to try it out, refer to [the relevant doc](../reference/scala-command/index.md).

## How has the passing of arguments been changed from the old `scala` runner to Scala CLI?

Let us take a closer look on how the old runner handled arguments when compared to Scala CLI.

### The old ways

In the old `scala` runner, the first argument was treated as the input source, while the second and following arguments
were considered program arguments.

```bash ignore
scala Source.scala programArg1 programArg2
```

Since everything after the first argument had to be arbitrarily read as a program argument, regardless of format, all
runner options had to be passed before the source input.

```bash ignore
scala -save script.sc programArg1 programArg2
```

### The ways of Scala CLI

With Scala CLI's default way of handling arguments, inputs and program arguments have to be
divided by `--`. There is no limit for the number of either.

```bash ignore
scala-cli Source1.scala Source2.scala -- programArg1 programArg2
```

Additionally, a Scala CLI sub-command can be passed before the inputs section.
For example, to call the above example specifying the `run` sub-command explicitly, pass it like this:

```bash ignore
scala-cli run Source1.scala Source2.scala -- programArg1 programArg2
```

More on sub-commands can be found [here](../commands/basics.md).

Runner options can be passed on whatever position in the inputs section (before `--`).
For example, all the following examples are correct ways to specify the Scala version explicitly as `3.2`

```bash ignore
scala-cli -S 3.2 Source1.scala Source2.scala -- programArg1 programArg2
scala-cli Source1.scala -S 3.2 Source2.scala -- programArg1 programArg2
scala-cli Source1.scala Source2.scala -S 3.2 -- programArg1 programArg2
```

:::note
The exception to this rule are the launcher options, like `--power` or `--cli-version`.
Those have to be passed before the inputs section (before any source inputs).

For example, to explicitly specify the launcher should run Scala CLI `v0.1.20`, pass it like this:

```bash ignore
scala-cli --cli-version 0.1.20 Source1.scala Source2.scala -- programArg1 programArg2
```

Also, if a Scala CLI sub-command is being passed explicitly, all launcher options have to be passed before the
sub-command. This is especially important for `power` mode commands, as those need to be called with the `--power`
launcher option.

For example, to call [the `package` sub-command](../commands/package.md), do it like this:

```bash
scala-cli --power package --help
```

:::

### The Scala CLI `shebang` sub-command

To provide better support for shebang scripts, Scala CLI
has [a dedicated `shebang` sub-command](../commands/shebang.md), which handles arguments similarly to the old `scala`
script.

```bash ignore
scala-cli shebang Source.scala programArg1 programArg2
```

The purpose of the `shebang` sub-command is essentially to only be used in a shebang header (more
details on that can be found [in a later section of this guide](#example-shebang-script-with-scala-cli) or in the
separate [shebang scripts' guide](./shebang.md)), but nothing is really stopping you from using it from the command
line, if you're used to how the old `scala` runner handled arguments. Just bear in mind that it is not the intended user
experience.

## How are the old `scala` runner options supported?

For backwards compatibility's sake, Scala CLI accepts all the old `scala` runner options, although many of them have
been deprecated and are no longer supported in the new runner. This includes accepting all the Scala `2.13.x` and `3.x`
respective runners' specific options.

### Fully supported old `scala` runner options

The following old `scala` runner options are fully supported by Scala CLI, meaning that they deliver similar or expanded
functionalities with backwards-compatible syntax:

- `-e`, which is an alias for Scala CLI's `--execute-script` and a close synonym
  for [`--script-snippet`](../guides/snippets.md#examples)
- `-v` / `-verbose` / `--verbose`, which can be passed multiple times with Scala CLI, increasing the verbosity
- `-cp` / `-classpath` / `--class-path`, which adds compiled classes and jars to the class path
- `-version` / `--version`, which prints the currently run Scala CLI [version information](../commands/version.md)
- `-with-compiler`, which adds the Scala compiler dependency to the Scala CLI project
- Scala compiler options (with some requiring to be passed with `-O`, more info
  in [the section below](#scala-compiler-options))
- `-J<arg>` Java options
- `-Dname=prop` Java properties

### Old `scala` runner options which have a different meaning in Scala CLI

The following old `scala` runner options not only are not supported with their old functionalities, but have a different
meaning in Scala CLI:

- `-i`, which is now an alias for Scala CLI's [`--interactive` mode](../reference/cli-options.md#--interactive)
- `-h` / `-help`
    - in the old Scala `2.13.x` `scala` runner, it used to print the help of the runner
    - in the old Scala `3.x` `scala` runner however, it used to print the Scala compiler help instead
    - Scala CLI takes an approach similar to the old Scala `2.13.x` runner, and it prints Scala CLI help
    - to view the Scala compiler help with Scala CLI, pass
      the [--scalac-help](../commands/compile.md#scala-compiler-help) option instead

### Deprecated and unsupported old `scala` runner options

The following old `scala` runner options have been deprecated and even though they are accepted by Scala CLI (passing
them will not cause an error), they are ignored with an appropriate warning:

- `-save`, refer to [the `package` sub-command](../commands/package.md#library-jars) on how to package a Scala CLI
  project to a JAR
- `-nosave`, a JAR file is now never saved unless [the `package` sub-command](../commands/package.md) is called
- `-howtorun` / `--how-to-run`
    - Scala CLI assumes how a file is to be run based on its file extension (and optionally its shebang header). This
      cannot be overridden with a command line option, so ensure your inputs use the correct file extension or have
      the [shebang header](#example-shebang-script-with-scala-cli) defined. This is sort of the equivalent of the
      old `-howtorun guess`.
    - To run the `REPL`, refer to [the `repl` sub-command](../commands/repl.md)
    - This option has been largely replaced with Scala CLI's [sub-commands](../commands/basics.md)
- `-I`, to preload the extra files for the `REPL`, try passing them as inputs
  for [the repl sub-command](../commands/repl.md)
- `-nc` / `nocompdaemon`, the underlying script runner class can no longer be picked explicitly, as with the old `scala`
  runner
- `-run` - Scala CLI does not support explicitly forcing the old run mode. Just pass your sources as inputs and ensure
  they are in the correct format and extension.

### Scala compiler options

All compiler options are supported when passed with the `--scalac-option` flag (or the `-O` alias for short).
However, many compiler options can also be passed directly.
For more information, refer
to [the Scala compiler options section of the `compile` sub-command doc](../commands/compile.md#scala-compiler-options).

## How does Scala CLI detect if it's running a script or a main method?

To answer this question, some disambiguation is necessary.
The most important thing to note is that this has been handled differently by the 2 old `scala` runners (for
Scala `2.13.x` and for `3.x`), so a
consistent behaviour hasn't really been established before Scala CLI.

The Scala `2.13.x` old `scala` runner was the most flexible, automatically detecting if what is being run is a script or
an
object based on the source contents. This automatic detection was also possible to be overridden with the `-howtorun`
runner option (which has been
deprecated and is not supported in Scala CLI,
as [noted in an earlier section](#deprecated-and-unsupported-old-scala-runner-options)).
This also means that the `2.13.x` old `scala` runner did not really care about file extensions much.

In contrast, the Scala `3.x` old `scala` runner always expects to find a main method, potentially but not necessarily
using [the Scala 3 idiomatic `@main` annotation](https://docs.scala-lang.org/scala3/book/methods-main-methods.html).
This means that the Scala `3.x` runner respected main methods defined in `.sc` files, but did not support script
syntax (top level definitions with no explicit main method).

Scala CLI's approach is perhaps the most restrictive here.
It accepts explicitly defined main methods in `.scala` sources and script syntax in `.sc` sources, without any
additional flexibility.

The only exception would be files with no file extension, but with a shebang header, ran with the `shebang` sub-command.
Those are always treated as scripts (more details about this can be
found [in [the shebang scripts' guide](./shebang.md)]).

Now, to give some examples.

### Main class in a `.scala` input

Of course, the simplest case is putting a main class into a `.scala` source, which is supported by both of the old
runners and by Scala CLI.

```scala title=Main.scala
object Main {
  def main(args: Array[String]): Unit = println(args.mkString(" "))
}
```

<ChainedSnippets>

```bash ignore
scala Main.scala Hello world
```

```bash
scala-cli Main.scala -- Hello world
```

```text
Hello world
```

</ChainedSnippets>

### Main class in a `.sc` input

```scala title=main-in-script.sc
object Main {
  def main(args: Array[String]): Unit = println(args.mkString(" "))
}
```

This case has been supported by both of the old `scala` runners, but is not supported by Scala CLI, which expects a
script in a `.sc` input and wraps its contents in a main class of its own, not inspecting further for a nested one.
In other words, when explicitly declaring a main class when working with Scala CLI, you have to do it in a `.scala`
file.

```bash
scala-cli main-in-script.sc -- Hello world 
# no output will be printed
```

Running such an `.sc` file will not fail by the way, but neither will it print any output, since the appropriate method
hasn't been called explicitly in the script.

### Script syntax in an `.sc` file

```scala title=script.sc
println(args.mkString(" "))
```

This syntax is supported by the old Scala `2.13.x` runner, but **not** by the old Scala `3.x` one.
The Scala `3.x` runner does not allow for top level definitions without an explicit main class.

However, it is supported by Scala CLI.

<ChainedSnippets>

```bash
scala-cli script.sc -- Hello world
```

```text
Hello world
```

</ChainedSnippets>

### Script syntax in a `.scala` file

Now for the inverted case, where script-style top level definitions are put in a `.scala` input.

```scala title=script.scala
println(args.mkString(" "))
```

This has actually been supported by the old Scala `2.13.x` runner.
However, both the old Scala `3.x` runner as well as Scala CLI do not support it.

<ChainedSnippets>

```bash fail
scala-cli script.scala -- Hello world
```

```text
[error] ./ScriptInScala.scala:1:1
[error] Illegal start of toplevel definition
[error] println(args.mkString(" "))
[error] ^^^^^^^
Error compiling project (Scala 3.2.2, JVM)
Compilation failed
```

</ChainedSnippets>

### Inputs with no extension

```scala title=no-extension-script
println(args.mkString(" "))
```

```scala title=no-extension-main-class
object Main {
  def main(args: Array[String]): Unit = println(args.mkString(" "))
}
```

Files with no extensions have been supported in the `2.13.x` old runner, but not in `3.x`.

Script syntax in files with no extension (or with extensions not indicating other kinds of sources, like `.java`) are
supported in Scala CLI via the `shebang` sub-command (and not otherwise).
However, a shebang header is necessary. An example is given
in [a later section of this guide](#example-shebang-script-with-scala-cli).

## How to migrate scripts with the old `scala` runner in the shebang header to Scala CLI?

As described
in [an earlier section of this guide](#how-has-the-passing-of-arguments-been-changed-from-the-old-scala-runner-to-scala-cli),
the way the old `scala` runner handles arguments differs from Scala CLI.

The old `scala` script accepted arguments with syntax making it easy to use it in a shebang header.
That is, all arguments starting with the second were treated as program args, rather than input sources.
This is in contrast with the Scala CLI default way of handling arguments, where inputs and program arguments have to be
divided by `--`.

```bash ignore
scala-cli Source1.scala Source2.scala -- programArg1 programArg2
```

To better support shebang scripts, Scala CLI has a dedicated `shebang` sub-command, which handles arguments similarly to
the old `scala` script.

```bash ignore
scala-cli shebang Source.scala programArg1 programArg2
```

For more concrete examples on how to change the shebang header in your existing scripts, look below.

### Example shebang script with the Scala `2.13.x` old `scala` runner

This is how an example shebang script could have looked like for the old `scala` runner with Scala `2.13.x`

```scala compile title=old-scala-shebang-213.sc
#!/usr/bin/env scala
println("Args: " + args.mkString(" "))
```

### Example shebang script with the Scala `3.x` old `scala` runner

This in turn is the Scala `3.x` equivalent for its own old `scala` runner.

```scala compile title=old-scala-shebang-3.sc
#!/usr/bin/env scala
@main def main(args: String*): Unit = println("Args: " + args.mkString(" "))
```

### Example shebang script with Scala CLI

This is an example of how a Scala CLI script with a shebang header looks like.

```scala compile title=scala-cli-shebang.sc
#!/usr/bin/env -S scala-cli shebang
  println("Args: " + args.mkString(" "))
```

The example above refers `scala-cli`, as per the current default Scala CLI distribution.
If you have Scala CLI installed as `scala`, then that should be changed to the following:

```scala compile title=scala-cli-as-scala-shebang.sc
#!/usr/bin/env -S scala shebang
println("Args: " + args.mkString(" "))
```

For more information about the `shebang` sub-command, refer to [the appropriate doc](../commands/shebang.md).
For more details on how to use Scala CLI in shebang scripts, refer to [the relevant guide](../guides/shebang.md).
