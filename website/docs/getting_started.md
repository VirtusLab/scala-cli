---
title: Getting started
sidebar_position: 2
---

import {RunnableCommand} from "../src/components/RunnableCommand";

:::info
This article requires knowledge of Scala language (how to define class or method) as well as Scala tooling (repl, basics of dependency management and unit tests). 
::: 

In this article we will show how to get started with Scala CLI - entry point to the Scala Ecosystem. 

We will start with simple scripts, and then we will create a small project with things like tests and IDE support. This guide should provide you knowledge how to create and develop your first project using Scala CLI.

Fist, lets verify if Scala CLI is properly installed with a hello world:

```bash
echo 'println("Hello")' | scala-cli -
```

Running commands above should end up with `Hello` printed in the last line of the output. Running the command for the first time may take a bit longer then usual and print a bit logs because Scala CLI needs to download all artifacts needed to compile and run the code.

## Scripting

In fact, we just created a Scala Script, so let's create a script in a hello.sc file, that will actually greet properly.

```scala title=hello.sc
def helloMessage(names: Seq[String]) = names match
  case Nil => 
    "Hello!"
  case names =>
    names.mkString("Hello: ", ", ", "!")

println(helloMessage(args.toSeq))
```

Now let run it with 


<RunnableCommand>

```bash
scala-cli hello.sc
```

```
Hello
```
</RunnableCommand>

To provide arguments we need to add them after `--`:

<RunnableCommand>

```bash
scala-cli hello.sc -- Jenny Jake
```

```
Hello Jenny, Jake!
```

</RunnableCommand>

You may wonder what kind of Scala version was used under the hood. The answer is the latest stable one. If we want to specify the Scala version we can use `-S` or `--scala` option. More about setting Scala version in a dedicated [cookbook](./cookbooks/scala-versions.md).

Scala CLI offers much more features dedicated for scripting described in the [dedicated guide](./guides/scripting.md)

## Dependencies

Let's build something more serious. Best start is prototyping inside REPL. We can start a repl by simply running `scala-cli repl` and here we can also set a Scala version with `-S` or `--scala`.

*Scala CLI reuses most of its options across all its comments.**

One of the main strengths of Scala is its ecosystem. Scala CLI is designed in a way to expose Scala Ecosystem to all usages of Scala and running REPL is no exception. 

Let's start prototyping with [os-lib](https://github.com/com-lihaoyi/os-lib) - a Scala interface to common OS filesystem and subprocess. To experiment with `os-lib` in repl we simply need to add a parameter `--dep com.lihaoyi::os-lib:0.7.8`

<RunnableCommand>

```bash ignore
scala-cli repl --dep com.lihaoyi::os-lib:0.7.8
```

```scala ignore
scala> os.pwd
val res0: os.Path = ...

scala> os.walk(os.pwd)
val res1: IndexedSeq[os.Path] = ArraySeq(...)
```

</RunnableCommand>

## A project

Now is time to write some logic, based on the prototyping we have just did: a filter function to display all files with given extension in current directory. 

For the consistency of our results let's create a new directory and `cd` to it:

```bash
mkdir scala-cli-getting-started
cd scala-cli-getting-started
```
<!-- clear -->

Now we can write our logic in `files.scala`:

```scala title=files.scala
// using lib com.lihaoyi::os-lib:0.7.8

def filesByExtension(extension: String, dir: os.Path = os.pwd): Seq[os.Path] = 
  os.walk(os.pwd).filter(f => f.last.endsWith(s".$extension") && os.isFile(f))
```

As you may have noticed we specified a dependency within the `.scala` using `// using lib com.lihaoyi::os-lib:0.7.8`. In Scala CLI configuration can provided through so called using directives - a dedicated syntax that can be embedded in any `.scala` file. We have a dedicated [guide for using directives](./guides/using-directives.md).

Let's check if our code compiles. We can do that by simply running:

```bash
scala-cli compile .
```

This time we did not provide path to single files but rather used a (current) directory. For project-like use-cases we recommend providing directories rather then individual files. More most cases a current directory (`.`) is best choice.

## IDE support

Some people are fine working using command line only, but most Scala Developers use an IDE. Let's open Metals with your favorite editor inside `scala-cli-getting-started` directory. 

// TODO GIF

At this moment support for IntelliJ is often problematic. We are working on making it as rock solid as Metals one.

Actually, we've cheated a bit by running compilation first. In order for Metals or IntelliJ to pick up Scala CLI project we need to generate a BSP connection detail file. Scala CLI generate such details by default every time `compile`, `run` or `test` are run. We expose a `setup-ide` command to manually control creation of connection details file. You can find more details in  [IDE guide](./guides/ide.md).

## Tests

With IDE in place, how can we test if our code works correctly? Best way is to create unit test. The simplest way to add a test using scala-cli is by creating a file named that it ends with `.test.scala` like `files.test.scala`. There are also other ways to mark source a test described in [tests guide](./commands/test.md#test-sources).

We also need to add a test framework. Scala CLI support most popular test frameworks and for this guide we will stick to [munit](https://scalameta.org/munit/). To add a test framework we just need an ordinary dependency that we will add with `using` directive again:

```scala title=files.test.scala
// using lib org.scalameta::munit:1.0.0-M1

class TestSuite extends munit.FunSuite {
  test("hello") {
    val expectedNames = Seq("files.scala", "files.test.scala")
    assertEquals(expectedNames, filesByExtension("scala").map(_.last))
  }
}
```

Now we can run our tests in command line:   

<RunnableCommand>

```bash
scala-cli test .
```

```
Compiling project (test, Scala 3.0.2, JVM)
Compiled project (test, Scala 3.0.2, JVM)
TestSuite:
  + hello 0.058s
```

</RunnableCommand>

or directly within Metals:

// TODO gif



## A project, vol 2

With our code ready and tested it is now time to turn it into a usable tool. Let's make a command-line tool to count files by extension. For that we can write a simple script. With Scala CLI, scripts and scala sources can be mixed.

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

As you probably noticed, we are using `os-lib` in our script without any using directive, how is that possible? Actually, configuration provided by using directives are global and applies to all files. Since `files.scala` and `countByExtension.sc` are compiled together, defining a dependency to a library in more then one file is an anti-pattern. 

<!-- TODO add pice about scala-cli warnings in such case -->

Let's try it:

<RunnableCommand>

```bash
scala-cli . -- scala
```

```
files.scala
.scala/project_940fb43dce/src_generated/main/countByExtension.scala
files.test.scala
```

</RunnableCommand>

Why do we have an additional `.scala` file inside `.scala` dir? Actually, under the hood, Scala CLI needs sometimes to preprocess provided source file (e.g. for scripts) and we compile such file from within `.scala` directory. 

## Packaging

We could stop here and call scala-cli on set of sources every time. Scala CLI uses caches aggressively so rollup runs are reasonable fast (less then 2 seconds on my machine) but sometimes it is not fast enough or shipping sources and compiling them may be not convenient.

Scala CLI offers means to package your project. We can simply run:

```bash
scala-cli package . -o countByExtension
```

It will generate a thin, executable jar with the compiled code inside. We provided `-o` flag to customize the binary name (by default application is written as `app`). Now we can run our project with:

```bash
./countByExtension scala   
```

This time it took less then second so we have a big improvement. Created binary (a runnable jar) is self-contained and can be shipped to your colleagues or deployed.

 We can reduce the start up time even further using [Scala Native](./guides/scala-native.md) or package our application to other formats like [Docker container](./commands/package.md#docker-container), [assembly](./commands/package.md#assemblies) or even [os-specific packages](./commands/package.md#os-specific-packages) (.dep, .pgk etc.) All of this is outside of the scope of this guide.

## Summary

We've only scratch the surface what Scala CLI can do with this guide. We prepare a set of [cookbooks](./cookbooks/intro.md) showcasing solutions to some common problems as well as detailed set of [guides](./guides/intro.md) for our [commands](./commands/basics.md). 

We have a dedicated [room on Scala discord](https://discord.gg/KzQdYkZZza) to ask for help or discuss anything that is Scala CLI related. For more in-depth discussion we are using [Github discussion in our repo](https://github.com/VirtusLab/scala-cli/discussions) and this is a best place to suggest a new feature or an improvements.