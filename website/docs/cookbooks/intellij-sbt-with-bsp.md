---
title: Setup a Scala CLI project in IntelliJ alongside your existing SBT project
sidebar_position: 7
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

If you've read through [the basic IDEA IntelliJ cookbook](intellij.md), then you already know how to import a Scala CLI
project using `BSP`. However, did you know that it's possible to import one alongside an `SBT` project? (Or any other
build tool's project, for that matter.)

Here's a walk-through for a simple example.

Let's say you have an existing `SBT` project that you're working with for a while now. You have imported it in IntelliJ
and the integration works nicely.
The project's structure looks roughly like this:

<ChainedSnippets>

```bash
tree -a
```

```text
.
├── .bsp
│   └── sbt.json
├── .idea
│   ├── .gitignore
│   ├── codeStyles
│   │   ├── Project.xml
│   │   └── codeStyleConfig.xml
│   ├── libraries
│   │   ├── sbt__junit_junit_4_13_2_jar.xml
│   │   ├── sbt__org_hamcrest_hamcrest_core_1_3_jar.xml
│   │   ├── sbt__org_scala_lang_scala3_library_3_3_1_3_jar.xml
│   │   ├── sbt__org_scala_lang_scala_library_2_13_8_jar.xml
│   │   ├── sbt__org_scala_sbt_test_interface_1_0_jar.xml
│   │   ├── sbt__org_scalameta_junit_interface_1_0_0_M6_jar.xml
│   │   └── sbt__org_scalameta_munit_3_1_0_0_M6_jar.xml
│   ├── misc.xml
│   ├── modules
│   │   ├── intellij-sbt-with-scala-cli-bsp-build.iml
│   │   └── intellij-sbt-with-scala-cli-bsp.iml
│   ├── modules.xml
│   ├── sbt.xml
│   ├── scala_compiler.xml
│   ├── vcs.xml
│   └── workspace.xml
├── build.sbt
├── project
│   └── build.properties
├── scripts
│   ├── AnotherScript.sc
│   └── SomeScript.sc
├── src
│   ├── main
│   │   └── scala
│   │       └── main.scala
│   └── test
│       └── scala
│           └── MyTests.test.scala
└── target
    └── scala-3.1.3
        ├── classes
        │   ├── main$package$.class
        │   ├── main$package.class
        │   ├── main$package.tasty
        │   ├── main.class
        │   └── main.tasty
        └── test-classes
            ├── MyTests.class
            └── MyTests.tasty

16 directories, 32 files
```

</ChainedSnippets>

Now, let's say that at some point you decide you need to occasionally run some scripts relevant to this project. You run
those scripts with Scala CLI and decide it'd be convenient to keep them in the same repository. 

<ChainedSnippets>
```bash
tree scripts
```

```text
scripts
├── AnotherScript.sc
└── SomeScript.sc

0 directories, 2 files
```
</ChainedSnippets>

However, you already import this repo as an `SBT` project, so what can you do?
Well, you can import the Scala CLI scripts as a `BSP` module **alongside** your `SBT` project.

Make sure you setup the `BSP` configuration for the `scripts` directory first:

```bash ignore
scala-cli setup-ide scripts
```

As a result, a `scripts/.bsp` directory should be created.
Now, right-click on your project root directory in `IntelliJ` and go into `Module Settings`

![Go into Module Settings](/img/intellij_sbt_module_settings.png)

Then, under `Project Structure` -> `Modules` press the `+` button and then `Import Module`.

![Import a module](/img/intellij_module_settings_import_module.png)

Navigate to the `scripts` directory from there and add it as a `BSP` module (`BSP` should be an available choice,
if the `setup-ide` command was run correctly).

![Import from BSP as external model](/img/intellij_import_bsp_module.png)

Now the `scripts` `BSP` module should be imported and you should be able to run the scripts from your IDE.
The end result should look like this:

![Import from BSP as external model](/img/intellij_sbt_alongside_bsp.png)
