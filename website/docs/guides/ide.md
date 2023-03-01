---
title: IDE support
sidebar_position: 6
---

Scala CLI currently integrates a build server using the [BSP protocol](https://build-server-protocol.github.io/).
At this moment Scala CLI is not automatically detected by IDEs, so we need to
use [Build Server Discovery](https://build-server-protocol.github.io/docs/server-discovery.html) from BSP protocol to
generate a connection details file (`.bsp/scala-cli.json`).

:::note
If none of these commands were run:

- `compile`
- `run`
- `test`
- `setup-ide`

or a previously-generated connection detail file was deleted, your IDE will *not* use Scala CLI to configure your
workspace. (Although there are ongoing efforts to improve that situation.)

In this case, just run one of the commands above to recreate the connection details file.
:::

Since Scala CLI has a command-line-first approach, this is reflected in its IDE integration.
By default, Scala CLI stores options passed to the last `compile`, `run`, or `test` command, and uses those options to
configure the IDE.

For more control we also expose the [`setup-ide` command](/docs/commands/setup-ide.md), which lets you fine-tune the
options that are passed to the IDE.

But note that once `setup-ide` is used, Scala CLI does not update the configuration based on latest command.
To enable automatic updates again, remove the `.bsp` directory and run `compile`, `run`, or `test` to recreate the
connection details file.

## Specific IDEs supporting Scala CLI

Scala CLI has been tested with two main Scala IDEs:

- [Metals](https://scalameta.org/metals/), which is an LSP server for Scala, and is used
  with [Visual Studio Code](https://code.visualstudio.com/), [Vim](https://www.vim.org/) and many other editors
- [IntelliJ IDEA](https://www.jetbrains.com/idea/), with
  the [Scala Plugin](https://confluence.jetbrains.com/display/SCA/Scala+Plugin+for+IntelliJ+IDEA?_ga=2.54176744.1963952405.1634470110-410935139.1631638301)
  installed

In an ideal world we would replace the rest of this guide with something along the lines of, “Scala CLI works within
IDEs above as you would expect.” However, mainly due to how fresh Scala CLI is, and also due to our radical approach to
the project structure, using a Scala CLI project with your favourite IDE may not be as amazing as we would like. (That
being said, proper IDE integration is our top priority at this moment!)

### VS Code with Metals

Check the cookbook on [how to set up a Scala CLI project in VSCode with Metals](/docs/cookbooks/vscode.md).

### IntelliJ

Cookbooks on how to work with IntelliJ:

- [set up a simple Scala CLI project in IDEA IntelliJ](/docs/cookbooks/intellij.md)
- [set up a Scala CLI project in IntelliJ alongside an existing SBT project](/docs/cookbooks/intellij-sbt-with-bsp.md)
- [set up multiple Scala CLI projects in IDEA IntelliJ as separate modules](/docs/cookbooks/intellij-multi-bsp.md)

## Directories vs single files when working with an IDE

When working with Scala CLI in an IDE, it is generally suggested to use directories rather than single files.

```shell
scala-cli setup-ide some-directory
```

Of course, nothing is stopping you from working with whatever you like as normal,
but please do keep in mind that the IDE will import the exact build that you have set up,
without second-guessing the user's intentions. In many IDEs, IDEA IntelliJ & Visual Studio Code included,
everything within a given project root directory is at least implicitly treated as
a part of the project (and probably shown as part of your project structure).

This means that when you pass just a single source file to Scala CLI like this:

```shell
scala-cli setup-ide some-directory/A.scala
```

If you open its surrounding directory as a project, any other files present in that directory will be visible
in your IDE project's structure, but they will not be included in your builds.

So if you want to include another file in your build, let's say `some-directory/B.scala`
alongside the previously configured `some-directory/A.scala`, it is probably not enough
to create the file within the same directory in your IDE.

What you need to do instead is add it to your build with Scala CLI from the command line:

```shell
scala-cli setup-ide some-directory/A.scala some-directory/B.scala
```

There, now both `A.scala` and `B.scala` should be included in your builds when the IDE picks up the new structure.

Still, if you want to add/remove files like this a lot while working in an IDE,
it may be a lot simpler to work on the whole directory instead:

```shell
cd some-directory
scala-cli setup-ide .
```

That way all the contents of `some-directory` will be treated as a part of the project as you go,
without the need to jump into the command line whenever you create a new file.

## Remote and virtual inputs

Do note that IDEs do not yet support working with Scala CLI's remote and virtual inputs. That includes:

- [piped sources](./piping.md),
- URLs and [GitHub gists](/docs/cookbooks/gists.md),
- [code snippets](./snippets.md).

Beyond that, IDE support for some non-standard (like `.c` and `.h` resources used
with [Scala Native](./scala-native.md)) and experimental inputs (like i.e. [`.md` sources](./markdown.md)) may not yet
be on par with on-disk Scala and Java source files.
