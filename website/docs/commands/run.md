---
title: Run
sidebar_position: 6
---

The `run` command offers to run your Scala code:

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

This is the default command, so you're not required to specify it explicitly:
```bash
scala-cli Hello.scala
# Hello
```

## Passing arguments

You can pass arguments to the application or script you're launching after `--`:
```bash
scala-cli MyApp.scala -- first-arg second-arg
# MyApp called with arguments: first-arg second-arg

```

## Main class

`--main-class` allows to specify an explicit main class, in case your application
defines several main classes for example:

```scala title=hi.sc
println("Hi")
```

```bash
scala-cli Hello.scala hi.sc --main-class hi
```

If you application only defines a single main class, you can just omit `--main-class`.

## Custom JVM

`--jvm` allows to run your application with a custom JVM:
```bash
scala-cli my-app/ --jvm adopt:14
```

JVMs are [managed by coursier](https://get-coursier.io/docs/cli-java#managed-jvms) and are based on the [index](https://github.com/shyiko/jabba/blob/master/index.json) from the command-line tool [jabba](https://github.com/shyiko/jabba).

## Scala.JS

Scala.JS applications can also be compiled and run, with the `--js` option. Note that this requires `node`
to be [installed](/install#scala-js) on your system.

```bash
scala-cli Hello.scala --js
```

We have a dedicated [Scala.js guide](../20-guides/21-scala-js.md).

## Scala Native

Scala Native applications can also be compiled and run, with the `--native` option.
Note that the [Scala Native requirements](https://scala-native.readthedocs.io/en/latest/user/setup.html#installing-clang-and-runtime-dependencies) need to be [installed](install#scala-native) for this to work fine,
and that Scala Native only supports Linux and macOS for now.

```bash
scala-cli Hello.scala --native
```

We have a dedicated [Scala Native guide](../20-guides/22-scala-native.md) as well.

## Scala Scripts

Scala CLI can also compile and run Scala scripts.

```scala title=HelloScript.sc
#!/usr/bin/env scala-cli

println("Hello world from scala script")
```

```bash
scala-cli run HelloScript.sc
# Hello world from scala script
```

[Scripts guide](../20-guides/scripts) provides much more details.

## Scala CLI from docker

Scala applications can also be compiled and run using [docker](https://docs.docker.com/get-started/) image with `scala-cli` without a need to install Scala CLI manually.

```bash
docker run virtuslab/scala-cli:latest about
```

```scala title=HelloWorld.scala
object HelloWorld extends App {
  println("Hello world")
}
```

```bash
docker run  -v $(pwd)/HelloWorld.scala:/HelloWorld.scala virtuslab/scala-cli /HelloWorld.scala
# Hello world
```
