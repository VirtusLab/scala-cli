---
title: Run
sidebar_position: 6
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

The `run` command runs your Scala code:

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash
scala-cli run Hello.scala
```

```text
Hello
```

</ChainedSnippets>

This is the default command, so you don’t have to specify it explicitly:

<ChainedSnippets>

```bash
scala-cli Hello.scala
```

```text
Hello
```

</ChainedSnippets>

## Passing arguments

You can pass arguments to the application or script you're launching after `--`:

```scala title=app.sc
println(args.mkString("App called with arguments: ", ", ", ""))
```

<ChainedSnippets>

```bash
scala-cli app.sc -- first-arg second-arg
```

```text
App called with arguments: first-arg, second-arg
```

</ChainedSnippets>

<!-- Expected:
App called with arguments: first-arg, second-arg
-->

## Main class

If your application has multiple main classes, the `--main-class` option lets you explicitly specify the main class you
want to run:

```scala title=hi.sc
println("Hi")
```

```bash
scala-cli Hello.scala hi.sc --main-class hi_sc
```

## Custom JVM

`--jvm` lets you run your application with a custom JVM:

```bash
scala-cli Hello.scala --jvm adopt:14
```

You can also specify custom JVM with the using directive `//> using jvm`:

```scala compile
//> using jvm "adopt:14"
```

JVMs are [managed by coursier](https://get-coursier.io/docs/cli-java#managed-jvms), and are read from
the [coursier JVM index](https://github.com/coursier/jvm-index).
(New JVM versions are automatically checked daily, and updates for those are - manually - merged
swiftly.)

### JVM options

`--java-opt` lets you add `java` options which will be passed when running an application:

```bash
scala-cli Hello.scala --java-opt -Xmx1g --java-opt -Dfoo=bar
```

You can also add java options with the using directive `//> using javaOpt`:

```scala compile
//> using javaOpt "-Xmx1g", "-Dfoo=bar"
```

Additionally, java properties can be passed to `scala-cli` without `--java-prop`:

```bash
scala-cli Hello.scala -Dfoo=bar
```

### JAR

`scala-cli` lets you run JAR files just like any other input.

```bash ignore
scala-cli Hello.jar
```

```text
Hello World
```

When you provide a JAR file as input to `scala-cli`, it will be added to the `classPath`.

## Define source files in using directives

You can also add source files with the using directive `//> using file`:

```scala title=Main.scala
//> using file "Utils.scala" 

object Main extends App {
  println(Utils.message)
}
```

```scala title=Utils.scala
object Utils {
  val message = "Hello World"
}
```

`scala-cli` takes it into account and compiles `Utils.scala`.

<ChainedSnippets>

```bash
scala-cli Main.scala
```

```text
Hello World
```

</ChainedSnippets>

<!-- Expected:
Hello World
-->

It is also possible to pass multiple paths to source files in a single using directive:

```scala title=Multiple.scala
//> using files "Utils.scala", "Main.scala"
```

```bash
scala-cli run Multiple.scala
```

Note that the `//> using file` using directive only supports `.java`, `.scala`, `.sc` files or a directory.

## Watch mode

`--watch` makes Scala CLI watch your code for changes, and re-runs it upon any change
or when the `ENTER` key is passed from the command line:

<ChainedSnippets>

```bash ignore
scala-cli run Hello.scala  --watch
```

```text
Hello
Program exited with return code 0.
Watching sources, press Ctrl+C to exit, or press Enter to re-run.
Compiling project (Scala 3.2.2, JVM)
Compiled project (Scala 3.2.2, JVM)
Hello World
Program exited with return code 0.
Watching sources, press Ctrl+C to exit, or press Enter to re-run.
```

</ChainedSnippets>

### Watch mode (restart)

The `--restart` option works very similarly to `--watch`, but instead of waking the sleeping thread,
it kills the process and restarts the app whenever sources change or the `ENTER` key is passed from the command line.

<ChainedSnippets>

```bash ignore
scala-cli run Hello.scala --restart
```

```text
Watching sources while your program is running.
Hello
Program exited with return code 0.
Watching sources while your program is running.
Compiling project (Scala 3.2.2, JVM)
Compiled project (Scala 3.2.2, JVM)
Hello World
Program exited with return code 0.
Watching sources while your program is running.
```

</ChainedSnippets>

## Scala.js

Scala.js applications can also be compiled and run with the `--js` option.
Note that this requires `node` to be [installed](/install#scala-js) on your system:

```bash
scala-cli Hello.scala --js
```

It is also possible to achieve it using `--platform` option:

```bash
scala-cli Hello.scala --platform js
```

See our dedicated [Scala.js guide](/docs/guides/scala-js.md) for more information.

## Scala Native

Scala Native applications can be compiled and run with the `--native` option.
Note that
the [Scala Native requirements](https://scala-native.readthedocs.io/en/latest/user/setup.html#installing-clang-and-runtime-dependencies)
need to be [installed](/install#scala-native) for this to work:

```bash
scala-cli Hello.scala --native -S 2.13.6
```

It is also possible to achieve it using `--platform` option:

```bash
scala-cli Hello.scala --platform native
```

We have a dedicated [Scala Native guide](/docs/guides/scala-native.md) as well.

## Platform

The `--platform` option can be used to choose the platform, which should be used to compile and run application.
Available platforms are:

* JVM (`jvm`)
* Scala.js (`scala.js` | `scala-js` | `scalajs` | `js`)
* Scala Native (`scala-native` | `scalanative` | `native`)

Passing the `--platform` along with `--js` or `--native` is not recommended. If two different types of platform are
passed, Scala CLI throws an error.

## Scala Scripts

Scala CLI can also compile and run Scala scripts:

```scala title=HelloScript.sc
#!/ usr / bin / env -S scala -cli shebang

println("Hello world from scala script")
```

<ChainedSnippets>

```bash
scala-cli run HelloScript.sc
```

```text
Hello world from scala script
```

</ChainedSnippets>

Our [scripts guide](/docs/guides/scripts.md) provides many more details.

## Scala CLI from docker

Scala applications can also be compiled and run using a [docker](https://docs.docker.com/get-started/) image
with `scala-cli`, without needing to install Scala CLI manually:

```bash
docker run virtuslab/scala-cli:latest version
```

```scala title=HelloWorld.scala
object HelloWorld extends App {
  println("Hello world")
}
```

<ChainedSnippets>

```bash ignore
docker run  -v $(pwd)/HelloWorld.scala:/HelloWorld.scala virtuslab/scala-cli /HelloWorld.scala
```

```text
Hello world
```

</ChainedSnippets>

## Debugging

It is possible to debug code by passing `--debug` flag.

Additional debug options:

* `--debug-mode` (attach by default)
* `--debug-port` (5005 by default)

Available debug modes:

* Attach (`attach` | `att` | `a`)
* Listen (`listen` | `lis` | `l`)

Example debugging with scala-cli:

```bash ignore
scala-cli Foo.scala --debug --debug-mode l --debug-port 5006
```
