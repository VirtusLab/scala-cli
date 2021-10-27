---
title: IDE support
sidebar_position: 10
---

:::warning
IDE support is under development mainly because changes needs to be done in both Metals and IntelliJ.
:::


Scala CLI currently Scala integrates with IDEs using [BSP protocol](https://build-server-protocol.github.io/). At this moment Scala CLI is not automatically detected so we need to use [Build Server Discovery](https://build-server-protocol.github.io/docs/server-discovery.html) from the BSP protocol. This is the main reason we create the BSP connection details file (`.bsp/scala-cli.json`) for your editor to pick up.

:::note
If none of the following commands were ran: `compile`, `run`, `test`,
`setup-ide` or a `.bsp/scala-cli.json` file has yet to be created (or was deleted), your editor
won't pick up scala-cli as a build server. In that case, simply run one of the
those commands and the file will be created.
:::

Since Scala CLI has a command-line-first approach that is reflected in the IDE
integrations. By default, Scala CLI stores options passed to the last `compile`,
`run` or `test` commands and use those options to configure your IDE. 

For more control we also expose a [`setup-ide` command](../commands/setup-ide.md) that allows to fine tune options passed to IDE.

Once `setup-ide` is used, Scala CLI does not update configuration based on the latest command.

To enable automatic updates again, remove the `.bsp` directory and run `compile`, `run` or `test` to recreate the connection details file (`.bsp/scala-cli.json`).

For now non-local sources are supported. What are non-local sources? Gists, URLs or piped sources.


Scala CLI was tested with two main IDEs for Scala:
 - [Metals](https://scalameta.org/metals/): LSP server for Scala used with [Visual Studio Code](https://code.visualstudio.com/), [Vim](https://www.vim.org/) and many other editors
 - [Intelij Idea](https://www.jetbrains.com/idea/) with [Scala Plugin](https://confluence.jetbrains.com/display/SCA/Scala+Plugin+for+IntelliJ+IDEA?_ga=2.54176744.1963952405.1634470110-410935139.1631638301) installed

In ideal world, we would replace the rest of this Guide with something along the
lines of: `Scala CLI works with all IDEs above as you would expect` however
mainly due to how fresh Scala CLI is and also due to our radical approach to the
project structure using a Scala CLI-powered projects with your favourite IDE may
not be as amazing as we would like to be.

Proper IDE integration is our top priority at them moment.

## Metals

Once Metals picks up proper project structure then basic features like navigation, diagnostics or code completion should work.

The Current release Metals (0.10.7) is not able to pick changes in the build
structure automatically and this includes adding new source files. In order
for Metals to pick up new files or changes in build structure you'll need to
trigger the 'Restart build server' command.

Main classes and tests are at this moment not recognized in Metals.

Generally, mainly due to problems with keeping project structure up to date
Metals can assists with writing code while using Scala CLI, but it cannot be the
source of truth and we recommend falling back to command line in such cases.

## IntelliJ

In terms of IntelliJ, the most significant problem is that IntelliJ requires sources to be placed within a directory. This means that top level sources are not recognized as proper sources.

<<<<<<< HEAD
With IntelliJ, we strongly suggested to placed your sources within a directory (like `src`).

IntelliJ currently does not automatically pick up changes in project structure so any change in dependencies, compiler options etc. needs manual reload.

Similarly to Metals, currently we do not advise using IntelliJ as a source of truth. We recommend falling back to command line in such cases.
=======
With Intellij, it's strongly suggested to place your sources within a directory (like `src`).

Intellij currently does not automatically pick up changes in your project
structure so any change in dependencies, compiler options etc. needs a manual
reload.

Similarly to Metals, we don't currently advise using Intellij as a source of
truth and we recommend falling back to command line in such cases.
>>>>>>> be56190 (docs: fix typos and clarify a few things on the ide page)
