---
title: Run
---

The `run` command offers to run your Scala code:
```bash
cat Hello.scala
# object Hello {
#   def main(args: Array[String]): Unit =
#     println("Hello")
# }
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
```bash
scala-cli my-app/ --main-class app.Main
```

If you application only defines a single main class, you can just omit `--main-class`.

## Custom JVM

`--jvm` allows to run your application with a custom JVM:
```bash
scala-cli my-app/ --jvm adopt:14
```

JVMs are [managed by coursier](https://get-coursier.io/docs/cli-java#managed-jvms).

## Scala.JS

Scala.JS applications can also be compiled and run, with the `--js` option. Note that this requires `node`
to be installed on your system.
```bash
scala-cli my-scala-js-app/ --js
```

## Scala Native

Scala Native applications can also be compiled and run, with the `--native` option.
Note that the [Scala Native requirements](https://scala-native.readthedocs.io/en/latest/user/setup.html#installing-clang-and-runtime-dependencies) need to be installed for this to work fine,
and that Scala Native only supports Linux and macOS for now.
```bash
scala-cli my-scala-native-app/ --native
```

## Scala Scripts

Scala CLI can also compile and run Scala scripts.
```bash
cat HelloScript.sc
# #!/usr/bin/env scala-cli

# println("Hello world from scala script")

scala-cli run HelloScript.sc
# Hello world from scala script
```

You may also pass args to your script, it is referenced via `args` special variable

```bash
cat p.sc
# !/usr/bin/env scala-cli
# println(args(1))
./p.sc hello world
# world
```

## ScalaCli from docker

Scala applications can also be compiled and run using docker image with `scala-cli`.

```bash
docker run virtuslab/scala-cli:latest about
```

###
```bash
cat HelloWorld.scala
# object HelloWorld extends App {
#     println("Hello world")
# }
docker run  -v $(pwd)/HelloWorld.scala:/HelloWorld.scala virtuslab/scala-cli /HelloWorld.scala
# Hello world
```
