---
title: Setup a Scala CLI project in VSCode
sidebar_position: 7
---

Scala CLI can generate the files that are necessary for providing IDE support in Visual Studio Code.

For example, here is a simple project in scala-cli which contains only one main and one test class.

```scala title=HelloWorld.scala
@main
def hello() = println("Hello, world")
```

```scala title=MyTests.test.scala
//> using lib "org.scalameta::munit::1.0.0-M1"

class MyTests extends munit.FunSuite {
  test("test") {
    val x = 2
    assertEquals(x, 2)
  }
}
```

The following command generates configuration files for VSCode support:

```bash
scala-cli setup-ide .
```

There is also another way. The first time you run the `run`|`compile`|`test` commands, the configuration files for the
VSCode will be also generated.

```bash
scala-cli run .
# "Hello, world"
```

and then, we launch Visual Studio Code

```bash ignore
code .
```

After starting metals, you will see the `run/debug` buttons in `HelloWorld.scala` and `test/debug`
in `MyTests.test.scala` (assuming the following directory layout).

![layout](/img/source_layout.png)

Pressing the `run` button will run the `Main.scala`, the output will be visible in `DebugConsole`.

import VSCodeRun from '@site/static/img/vscode-run.png';

<img src={VSCodeRun} />

And the similar effect after pressing the `test` button.

import VSCodeTest from '@site/static/img/vscode-test.png';

<img src={VSCodeTest} />
