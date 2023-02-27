---
title: Shebang
sidebar_position: 30
---


This guide explains the differences between the `run` and `shebang` sub-commands, mainly covering how each of them
parses its arguments.

### `shebang` script headers

Before proceeding, let's discuss how `scala-cli` works in a script without the `shebang` command.
Here is a simple `hello.sc` script with a `shebang` header:

```scala title=hello.sc
#!/usr/bin/env -S scala-cli -S 3

println(args.size)
println(args.headOption)
```

And it works correctly:

<ChainedSnippets>

```bash 
chmod +x hello.sc
./hello.sc    
```

```text
0
None
```

<!-- Expected:
0
None
-->

</ChainedSnippets>

And it also works:

<ChainedSnippets>

```bash
./hello.sc -- Hello World
```

```text
2
Some(Hello)
```

<!-- Expected:
2
Some(Hello)
-->

</ChainedSnippets>

Note that the extra `--` must be added to make it work. If it is not supplied, the result is:

<ChainedSnippets>

```bash run-fail
./hello.sc Hello World
```

```text
[error] Hello: input file not found
World: input file not found
```

<!-- Expected:
Hello: input file not found
World: input file not found
-->

</ChainedSnippets>

If we modify our script slightly and use the `shebang` sub-command in the header, we will get the following:

```scala title=hello.sc
#!/usr/bin/env -S scala-cli shebang -S 3

println(args.size)
println(args.headOption)
```

<ChainedSnippets>

```bash
./hello.sc Hello World
```

```text
2
Some(Hello)
```
<!-- Expected:
2
Some(Hello)
-->

</ChainedSnippets>


### `shebang` and the command line

Let's now see how the `shebang` command works straight from the command line.

```scala title=Main.scala 
object Main {
  def main(args: Array[String]): Unit = println(args.mkString(" "))
}  
```

<ChainedSnippets>

```bash                                                                                                                                                                                                                                                                
scala-cli shebang Main.scala Hello world
```

```text
Hello world
```

<!-- Expected:
Hello world
-->

</ChainedSnippets>


:::note
Please note that `shebang` changing how arguments are parsed means that every option after the first input will be treated as
an argument to the app.

<ChainedSnippets>

```bash
scala-cli shebang Main.scala -S 2.13 #-S 2.13 is not recognised as an option, but as app arguments
```

```text
-S 2.13
```

<!-- Expected:
-S 2.13
-->

</ChainedSnippets>
:::

If we try to do the same with the `run` sub-command, we get the following error:

<ChainedSnippets>

```bash run-fail
scala-cli run Main.scala Hello world
```

```text
[error]  Hello: input file not found
world: input file not found
```

<!-- Expected:
[error]  Hello: input file not found
world: input file not found
-->

</ChainedSnippets>

### Script files' extensions

When running the `shebang` subcommand, script files don't need the `.sc` extension,
but they are then REQUIRED to start with a shebang line:

```scala title=hello-with-shebang
#!/usr/bin/env -S scala-cli shebang -S 3

println(args.size)
println(args.headOption)
```

<ChainedSnippets>

```bash
chmod +x hello-with-shebang
./hello-with-shebang Hello World
```

```text
2
Some(Hello)
```
<!-- Expected:
2
Some(Hello)
-->

</ChainedSnippets>

```scala title=hello-no-shebang
println(args.size)
println(args.headOption)
```

<ChainedSnippets>

```bash run-fail
chmod +x hello-no-shebang
scala-cli shebang hello-no-shebang Hello World
```

```text
hello-no-shebang: unrecognized source type (expected .scala or .sc extension, or a directory)
```
<!-- Expected:
hello-no-shebang: unrecognized source type (expected .scala or .sc extension, or a directory)
-->
</ChainedSnippets>

:::note
Files with no extensions are always run as scripts even though they may contain e.g. valid `.scala` program.
:::
