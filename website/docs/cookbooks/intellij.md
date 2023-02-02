---
title: Setup a Scala CLI project in IDEA IntelliJ
sidebar_position: 7
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

It is possible to import a Scala CLI project into IDEA IntelliJ. The import is done
through [BSP](https://build-server-protocol.github.io/) and the relevant files can be seen in the hidden `.bsp`
directory, generated after running the `setup-ide` command (or implicitly the `run`|`compile`|`test` commands as well).

Here's a walk-through for a simple import scenario.

```scala title=src/HelloWorld.scala
@main
def hello() = println("Hello, world")
```

```scala title=test/MyTests.test.scala
//> using dep "org.scalameta::munit::1.0.0-M1"

class MyTests extends munit.FunSuite {
  test("test") {
    val x = 2
    assertEquals(x, 2)
  }
}
```

<ChainedSnippets>

```bash
tree -a
```

```text
.
├── src
│   └── HelloWorld.scala
└── test
    └── MyTests.test.scala
```

</ChainedSnippets>

The following command generates all the relevant configurations for IDE support:

```bash
scala-cli setup-ide .
```

Alternatively, the first time you run the `run`|`compile`|`test` commands, the relevant IDE configuration will be
generated as well.

In fact it is entirely sufficient to just run:

<ChainedSnippets>

```bash
scala-cli .
```

```text
Hello, world
```

</ChainedSnippets>

Next, we need to launch IDEA IntelliJ.
To import the project, you can import it, `File` -> `New` -> `Project from Existing Sources...`

![Project from Existing Sources...](/img/intellij_project_from_existing_sources.png)

And then pick `BSP` as the external model (if `BSP` doesn't show up at this step, it means that the `.bsp` folder is
absent and should be generated with the `scala-cli setup-ide` subcommand).

![BSP external model](/img/intellij_bsp_external_model.png)

Alternatively, you can directly call `File` -> `Open` and pick the directory, allowing `IntelliJ` to figure things out
by itself (which it definitely should, if the `.bsp` folder is in place). Just make sure the `.bsp` folder is present in
the project root directory.

![just open the directory](/img/intellij_open_dir.png)

You should now be able to see the active `BSP` connection icon in the lower right corner of your `IDEA IntelliJ` window.

![BSP icon](/img/intellij_bsp_icon.png)

The run buttons, syntax completions & coloring should now be available when opening source files.
IntelliJ should also be identifying the main sources(blue) and test sources (green) directories.

![imported project layout](/img/intellij_imported_project_layout.png)

IDEA IntelliJ will now call Scala CLI's `bsp` command to handle running, testing and debugging your code in this
project.

![run your code in IntelliJ](/img/intellij_run_code_with_bsp.png)

Also, please do note, that the project structure comes directly from Scala CLI and you shouldn't really have to control
it from IntelliJ. Instead, being a CLI tool, we have a terminal-first policy, and so, if you want to update the project
structure to include an extra directory, just run the proper command to update the `.bsp` directory.

```bash ignore
scala-cli setup-ide . ../extra-directory
```

Now, after waiting for a bit, the extra directory should be picked up by `IntelliJ`.

![BSP project with 2 directories](/img/intellij_project_layout_with_extra_dir.png)

And if for whatever reason you want to reload the project manually, you can do it from `IntelliJ`'s `BSP` panel, just
click `Refresh` there.

![Refresh BSP manually](/img/intellij_refresh_bsp.png)

Note: this example scenario assumes the sources are put in separate subdirectories, 1 per scope. This is because that's
what is encouraged by IDEA IntelliJ, which assumes by default that tests should have its own directory. However, nothing
really forces you to bother with that, you can put everything in the root directory (or anywhere else, really), and it
should (mostly) work fine:

<ChainedSnippets>

```bash
tree -a
```

```text
.
├── HelloWorld.scala
└── MyTests.test.scala
```

</ChainedSnippets>

![Scala CLI flat project structure imported to IntelliJ](/img/intellij_flat_sources_layout.png)

