---
title: Getting started
sidebar_position: 2
---

import {ChainedSnippets, GiflikeVideo} from "../src/components/MarkdownComponents.js";

:::info
This article requires knowledge of the Scala language (how to define a class or method) as well as Scala tooling (the REPL, and basics of dependency management and unit tests).
::: 

In this article we show how to use Scala CLI to create a basic script, followed by small project with features like dependencies, tests, and IDE support. We aim to provide you with a knowledge of how to create and develop your first projects using Scala CLI.

First, let's verify if Scala CLI is properly [installed](/install) with a simple "hello world" test:

<ChainedSnippets>

```bash
echo 'println("Hello")' | scala-cli -
```

```
Hello
```

<!-- Expected:
Hello
-->

</ChainedSnippets>

Running this command the first time may take a bit longer then usual and print a fair number of logging output because Scala CLI needs to download all the artifacts it needs to compile and run the code.

## Scripting

In that example we actually just created a Scala Script. To demonstrate this more fully, let's create a script in a `hello.sc` file that greets more properly:

```scala title=hello.sc
def helloMessage(names: Seq[String]) = names match
  case Nil => 
    "Hello!"
  case names =>
    names.mkString("Hello: ", ", ", "!")

println(helloMessage(args.toSeq))
```

When that script is given no names, it prints `"Hello!"`, and when it’s given one or more names it prints the string that's created in the second `case` statement. With Scala CLI we run the script like this:

<ChainedSnippets>

```bash
scala-cli hello.sc
```

```
Hello
```
</ChainedSnippets>

To provide arguments to the script we add them after `--`:

<ChainedSnippets>

```bash
scala-cli hello.sc -- Jenny Jake
```

```
Hello Jenny, Jake!
```

</ChainedSnippets>

You may wonder, what kind of Scala version was used under the hood in that example? The answer is the latest stable one. If we want to specify the Scala version, we can use the `-S` (or `--scala`) option. You can read more about setting the Scala version in our dedicated [Scala Versions cookbook](./cookbooks/scala-versions.md).

Scala CLI offers many more features dedicated for scripting, as described in the [dedicated guide](./guides/scripts.md).

## Dependencies

Now let's build something more serious. For this example, it's best to start with some prototyping inside the REPL. We can start a REPL session by running `scala-cli repl`. (If desired, you can also set the Scala version with `-S` or `--scala`.)

:::note
Scala CLI reuses most of its options across all its comments.
:::

One of the main strengths of Scala is its ecosystem. Scala CLI is designed in a way to expose the Scala ecosystem to all usages of Scala, and running the REPL is no exception.

To demonstrate this, let's start prototyping with [os-lib](https://github.com/com-lihaoyi/os-lib) — a Scala interface to common OS filesystem and subprocess methods. To experiment with `os-lib` in the REPL, we simply need to add the parameter `--dep com.lihaoyi::os-lib:0.7.8`, as shown here:

<ChainedSnippets>

```bash ignore
scala-cli repl --dep com.lihaoyi::os-lib:0.7.8
```

```scala ignore
scala> os.pwd
val res0: os.Path = ...

scala> os.walk(os.pwd)
val res1: IndexedSeq[os.Path] = ArraySeq(...)
```

</ChainedSnippets>

## A project

Now it's time to write some logic, based on the prototyping we just did. We'll create a filter function to display all files with the given filename extension in the current directory.

For the consistency of our results, let's create a new directory and `cd` to it:

```bash
mkdir scala-cli-getting-started
cd scala-cli-getting-started
```
<!-- clear -->

Now we can write our logic in a file named `files.scala`:

```scala title=files.scala
// using lib com.lihaoyi::os-lib:0.7.8

def filesByExtension(
  extension: String, 
  dir: os.Path = os.pwd): Seq[os.Path] = 
    os.walk(dir).filter { f =>
      f.last.endsWith(s".$extension") && os.isFile(f)
    }
```

As you may have noticed, we specified a dependency within `files.scala` using the `// using lib com.lihaoyi::os-lib:0.7.8` syntax. With Scala CLI, you can provide configuration information with `using` directives — a dedicated syntax that can be embedded in any `.scala` file. For more details, see our dedicated [guide for `using` directives](./guides/using-directives.md).

Now let's check if our code compiles. We do that by running:

```bash
scala-cli compile .
```

Notice that this time we didn’t provide a path to single files, but rather used a directory; in this case, the current directory. For project-like use cases, we recommend providing directories rather than individual files. For most cases, specifying the current directory (`.`) is a best choice.

## IDE support

Some people are fine using the command line only, but most Scala developers use an IDE. To demonstrate this, let's open Metals with your favorite editor inside `scala-cli-getting-started` directory:

<GiflikeVideo url='/img/scala-cli-getting-started-1.mp4'/>

At the present moment, support for IntelliJ is often problematic. But know that we are working on making it as rock-solid as Metals.

Actually, in this case, we cheated a bit by running the compilation first. In order for Metals or IntelliJ to pick up a Scala CLI project, we need to generate a BSP connection detail file. Scala CLI generates these details by default every time `compile`, `run`, or `test` are run. We also expose a `setup-ide` command to manually control creation of the connection details file. For more information on this, see our [IDE guide](./guides/ide.md).

## Tests

With our IDE in place, how can we test if our code works correctly? The best way is to create a unit test. The simplest way to add a test using scala-cli is by creating a file whose name ends with `.test.scala`, such as `files.test.scala`. (There are also other ways to mark source code files as containing a test, as described in [tests guide](./commands/test.md#test-sources).)

We also need to add a test framework. Scala CLI support most popular test frameworks, and for this guide we will stick with [munit](https://scalameta.org/munit/). To add a test framework, we just need an ordinary dependency, and once again we'll add that with the `using` directive:

```scala title=files.test.scala
// using lib org.scalameta::munit:1.0.0-M1

class TestSuite extends munit.FunSuite {
  test("hello") {
    val expected = Seq("files.scala", "files.test.scala")
    val obtained = filesByExtension("scala").map(_.last)
    assertEquals(obtained, expected)
  }
}
```

Now we can run our tests at the command line:

<ChainedSnippets>

```bash
scala-cli test .
```

```
Compiling project (test, Scala 3.0.2, JVM)
Compiled project (test, Scala 3.0.2, JVM)
TestSuite:
  + hello 0.058s
```

</ChainedSnippets>

or directly within Metals:

<GiflikeVideo url='/img/scala-cli-getting-started-2.mp4'/>

## A project, vol 2

With our code ready and tested, now it's time to turn it into a command-line tool that counts files by their extension. For this we can write a simple script. A great feature of Scala CLI is that scripts and Scala sources can be mixed:

```scala title=countByExtension.sc
val (ext, directory) = args.toSeq match 
  case Seq(ext) => (ext, os.pwd)
  case Seq(ext, directory) => (ext, os.Path(directory))
  case other => 
    println(s"Expected: `extension [directory]` but get: `${other.mkString(" ")}`")
    sys.exit(1)

val files = filesByExtension(ext, directory)
files.map(_.relativeTo(directory)).foreach(println)
```

As you probably noticed, we are using `os-lib` in our script without any `using` directive; how is that possible? The way this works is that configuration details provided by `using` directives are global, and apply to all files. Since `files.scala` and `countByExtension.sc` are compiled together, the `using` directives in `files.scala` are used when compiling both files. (Note that defining a library dependency in more than one file is an anti-pattern.)

<!-- TODO add piece about scala-cli warnings in such case -->

Now let's run our code, looking for all files that end with the `.scala` extension:

<ChainedSnippets>

```bash
scala-cli . -- scala
```

```
files.scala
.scala/project_940fb43dce/src_generated/main/countByExtension.scala
files.test.scala
```

</ChainedSnippets>

Seeing that output, you may wonder, why do we have an additional `.scala` file under the `.scala` dir? The way this works is that under the hood, Scala CLI sometimes needs to preprocess source code files — such as scripts. So these preprocessed files are created under the `.scala` directory, and then compiled from there.

## Packaging

We could stop here and call `scala-cli` on our set of sources every time. Scala CLI uses caches aggressively, so rollup runs are reasonably fast — less than 1,500 milliseconds on my machine — but sometimes this isn't fast enough, or shipping sources and compiling them may be not convenient.

For these use cases, Scala CLI offers means to package your project. For example, we can run this command to generate a thin, executable jar file, with the compiled code inside:

```bash
scala-cli package . -o countByExtension
```

The default binary name is `app`, so in this example we provide the `-o` flag to make the binary name `countByExtension`. Now we can run our project like this:

```bash
./countByExtension scala   
```

This time it only took 350 milliseconds, so this is a big improvement. When you create a binary file (a runnable jar) like this, it's self-contained, and can be shipped to your colleagues or deployed.

We can reduce the startup time even further using [Scala Native](./guides/scala-native.md), or by packaging our application to other formats like [Docker container](./commands/package.md#docker-container), [assembly](./commands/package.md#assemblies), or even [OS-specific packages](./commands/package.md#os-specific-packages) (.dep, .pkg, etc.). See those resources for more information.


## Summary

With this guide we've only scratched the surface of what Scala CLI can do. For many more details, we've prepared a set of [cookbooks](./cookbooks/intro.md) that showcase solutions to common problems, as well as a detailed set of [guides](./guides/intro.md) for our [commands](./commands/basics.md). 

We also have a dedicated [room on Scala discord](https://discord.gg/KzQdYkZZza) where you can ask for help or discuss anything that's related to Scala CLI. For more in-depth discussions, we're using [Github discussions in our repo](https://github.com/VirtusLab/scala-cli/discussions); this is the best place to suggest a new feature or any improvements.
