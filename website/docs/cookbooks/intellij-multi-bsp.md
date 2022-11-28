---
title: Setup multiple Scala CLI projects in IDEA IntelliJ as separate modules
sidebar_position: 7
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

If you've read through [the basic IDEA IntelliJ cookbook](intellij.md), then you already know how to import a Scala CLI
project using `BSP`. However, in some cases importing a single project just does not fit the bill.

Here's a walk-through for a slightly more advanced scenario.

Let's say we keep the sources for 2 separate Scala apps in one repository. Each has its own subdirectory, to keep things
clean. Additionally, you have another one for scripts alongside them.

It looks somewhat similar to this:

<ChainedSnippets>

```bash
tree -a
```

```text
.
├── app1
│   ├── src
│   │   └── HelloWorld1.scala
│   └── test
│       └── MyTests1.test.scala
├── app2
│   ├── src
│   │   └── HelloWorld2.scala
│   └── test
│       └── MyTests2.test.scala
└── scripts
    ├── AnotherScript.sc
    └── SomeScript.sc

7 directories, 6 files
```

```scala title=app1/src/HelloWorld1.scala
@main def hello: Unit = println("hello1")
```

```scala title=app1/test/MyTests1.scala
//> using lib "org.scalameta::munit:1.0.0-M7"
class MyTests1 extends munit.FunSuite {
  test("my test 1") {
    assert(2 + 2 == 4)
  }
}
```

```scala title=app2/src/HelloWorld2.scala
@main def hello: Unit = println("hello2")
```

```scala title=app2/test/MyTests2.scala
//> using lib "com.lihaoyi::utest::0.8.1"
import utest.*
object MessagesTests extends TestSuite {
  val tests = Tests {
    test("my test 2") {
      assert(2 + 2 == 4)
    }
  }
}
```

```scala title=scripts/SomeScript.sc
println("some script")
```

```scala title=scripts/AnotherScript.sc
println("another script")
```

</ChainedSnippets>

When running these apps, you'd like to run them separately. `app1` and `app2` may have conflicting dependencies, or it
may just not feel hygienic to share their classpath long term.

However, you keep those in one repository because of business relevance (or whatever other reasons why they are tied
together), and so, you'd like to see them all at once in your IDE, with all the syntax coloring, completions and
debugging
your code straight from the IDE, the whole shebang.

It's tempting to just run:

```bash
scala-cli setup-ide .
```

Unfortunately, in this case that won't really do the trick. Even if you run and package the apps & scripts from the
terminal separately, when importing everything together to your IDE like this, the single `BSP` project will make them
share their classpath. This in turn means that things will break.

The only way to solve this is for each to have its own `BSP` configuration, really.
And so:

```bash
scala-cli setup-ide app1
scala-cli setup-ide app2
scala-cli setup-ide scripts
```

As a result, a separate `.bsp` directory was created in `app1`, `app2` and `scripts`, respectively.

<ChainedSnippets>

```bash
tree -a
```

```text
.
├── app1
│   ├── .bsp
│   │   └── scala-cli.json
│   ├── .scala-build
│   │   ├── ide-inputs.json
│   │   └── ide-options-v2.json
│   ├── src
│   │   └── HelloWorld1.scala
│   └── test
│       └── MyTests1.test.scala
├── app2
│   ├── .bsp
│   │   └── scala-cli.json
│   ├── .scala-build
│   │   ├── ide-inputs.json
│   │   └── ide-options-v2.json
│   ├── src
│   │   └── HelloWorld2.scala
│   └── test
│       └── MyTests2.test.scala
└── scripts
    ├── .bsp
    │   └── scala-cli.json
    ├── .scala-build
    │   ├── ide-inputs.json
    │   └── ide-options-v2.json
    ├── AnotherScript.sc
    └── SomeScript.sc

13 directories, 15 files



```

</ChainedSnippets>


After opening the root directory in `IntelliJ` (`File` -> `Open...`), the 3 `BSP` setups should be successfully
detected.

![IntelliJ noticed the 3 BSP configs](/img/intellij_bsp_build_scripts_found.png)

However, since there are 3 different setups, `IntelliJ` doesn't know what to import. And so, we have to set it up
ourselves.

Right-click on your project root directory in `Intellij` and go into `Module Settings`.

![Go into Module Settings](/img/intellij_module_settings.png)

Then, under `Project Structure` -> `Modules` press the `+` button and then `Import Module`.

![Import a module](/img/intellij_module_settings_import_module.png)

Navigate to each of the subdirectories from there and add them as a `BSP` module (`BSP` should be an available choice,
if the `setup-ide` was run correctly).

![Import from BSP as external model](/img/intellij_import_bsp_module.png)

You have to import each of the subdirectories separately (`app1`, `app2` and `scripts`, in the example).

The end result should look like this:

![End result multi-BSP setup](/img/intellij_multi_bsp_setup.png)

Now each of the subdirectories uses its own `BSP` connection, which in turn means a separate classpath. And all of that
in a single `IntelliJ` project!

Upon closer inspection, you may notice that `IntelliJ` stores this as separate sub-project configurations. Each
subdirectory gets its own `.idea` folder with the relevant settings.

<ChainedSnippets>

```bash
tree -a
```

```text
.
├── .idea
│   ├── .gitignore
│   ├── bsp.xml
│   ├── codeStyles
│   │   ├── Project.xml
│   │   └── codeStyleConfig.xml
│   ├── intellij-multi-bsp.iml
│   ├── misc.xml
│   ├── modules.xml
│   ├── sbt.xml
│   ├── vcs.xml
│   └── workspace.xml
├── app1
│   ├── .bsp
│   │   └── scala-cli.json
│   ├── .idea
│   │   └── modules
│   │       └── app1-root.iml
│   ├── .scala-build
│   │   ├── ide-inputs.json
│   │   └── ide-options-v2.json
│   ├── src
│   │   └── HelloWorld1.scala
│   └── test
│       └── MyTests1.test.scala
├── app2
│   ├── .bsp
│   │   └── scala-cli.json
│   ├── .idea
│   │   └── modules
│   │       └── app2-root.iml
│   ├── .scala-build
│   │   ├── ide-inputs.json
│   │   └── ide-options-v2.json
│   ├── src
│   │   └── HelloWorld2.scala
│   └── test
│       └── MyTests2.test.scala
└── scripts
    ├── .bsp
    │   └── scala-cli.json
    ├── .idea
    │   └── modules
    │       └── scripts-root.iml
    ├── .scala-build
    │   ├── ide-inputs.json
    │   └── ide-options-v2.json
    ├── AnotherScript.sc
    └── SomeScript.sc

21 directories, 28 files
```

</ChainedSnippets>
