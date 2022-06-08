---
title: Run
sidebar_position: 6
---

The `run` command runs your Scala code:

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

```bash
scala-cli run Hello.scala
# Hello
```

This is the default command, so you don’t have to specify it explicitly:
```bash
scala-cli Hello.scala
# Hello
```

## Passing arguments

You can pass arguments to the application or script you're launching after `--`:

```scala title=app.sc
println(args.mkString("App called with arguments: ", ", ", ""))
```

```bash
scala-cli app.sc -- first-arg second-arg
# App called with arguments: first-arg, second-arg
```

<!-- Expected:
App called with arguments: first-arg, second-arg
-->

## Main class

If your application has multiple main classes, the `--main-class` option lets you explicitly specify the main class you want to run:

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

JVMs are [managed by coursier](https://get-coursier.io/docs/cli-java#managed-jvms), and are read from the [coursier JVM index](https://github.com/coursier/jvm-index).
(New JVM versions are automatically checked daily, and updates for those are - manually - merged
swiftly.)

## Watch mode

`--watch` makes `scala-cli` watch your code for changes, and re-runs it upon any change:

```bash ignore
scala-cli run Hello.scala  --watch
# Hello
# Watching sources, press Ctrl+C to exit.
# Compiling project (Scala 3.1.1, JVM)
# Compiled project (Scala 3.1.1, JVM)
# Hello World
# Watching sources, press Ctrl+C to exit.
```
### Watch mode - restart

`--restart` mode runs your application in the background and automatically restart it upon any change:

```bash ignore
scala-cli run Hello.scala --restart
# Hello
# Watching sources, press Ctrl+C to exit.
# Compiling project (Scala 3.1.1, JVM)
# Compiled project (Scala 3.1.1, JVM)
# Hello World
# Watching sources, press Ctrl+C to exit.
```

## Scala.js

Scala.js applications can also be compiled and run with the `--js` option.
Note that this requires `node` to be [installed](/install#scala-js) on your system:

```bash
scala-cli Hello.scala --js
```

See our dedicated [Scala.js guide](../guides/scala-js.md) for more information.

## Scala Native

Scala Native applications can be compiled and run with the `--native` option.
Note that the [Scala Native requirements](https://scala-native.readthedocs.io/en/latest/user/setup.html#installing-clang-and-runtime-dependencies) need to be [installed](/install#scala-native) for this to work:

```bash
scala-cli Hello.scala --native -S 2.13.6
```

We have a dedicated [Scala Native guide](../guides/scala-native.md) as well.

## Scala Scripts

Scala CLI can also compile and run Scala scripts:

```scala title=HelloScript.sc
#!/usr/bin/env -S scala-cli shebang

println("Hello world from scala script")
```

```bash
scala-cli run HelloScript.sc
# Hello world from scala script
```

Our [scripts guide](../guides/scripts.md) provides many more details.

## Scala CLI from docker

Scala applications can also be compiled and run using a [docker](https://docs.docker.com/get-started/) image with `scala-cli`, without needing to install Scala CLI manually:

```bash
docker run virtuslab/scala-cli:latest about
```

```scala title=HelloWorld.scala
object HelloWorld extends App {
  println("Hello world")
}
```

```bash ignore
docker run  -v $(pwd)/HelloWorld.scala:/HelloWorld.scala virtuslab/scala-cli /HelloWorld.scala
# Hello world
```
