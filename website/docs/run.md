---
title: Run
sidebar_position: 6
---

The `run` command offers to run your Scala code:
```text
$ cat Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
$ scala run Hello.scala
Hello
```

This is the default command, so you're not required to specify it explicitly:
```text
$ scala Hello.scala
Hello
```

## Passing arguments

You can pass arguments to the application you're launching after `--`:
```text
$ scala MyApp.scala -- first-arg second-arg
MyApp called with arguments: first-arg second-arg
```

## Main class

`--main-class` allows to specify an explicit main class, in case your application
defines several main classes for example:
```text
$ scala my-app/ --main-class app.Main
```

If you application only defines a single main class, you can just omit `--main-class`.

## Custom JVM

`--jvm` allows to run your application with a custom JVM:
```text
$ scala my-app/ --jvm adopt:14
```

JVMs are [managed by coursier](https://get-coursier.io/docs/cli-java#managed-jvms).

## Scala.JS

Scala.JS applications can also be compiled and run, with the `--js` option. Note that this requires `node`
to be installed on your system.
```text
$ scala my-scala-js-app/ --js
```

## Scala Native

Scala Native applications can also be compiled and run, with the `--native` option.
Note that the [Scala Native requirements](https://scala-native.readthedocs.io/en/latest/user/setup.html#installing-clang-and-runtime-dependencies) need to be installed for this to work fine,
and that Scala Native only supports Linux and macOS for now.
```text
$ scala my-scala-native-app/ --native
```
