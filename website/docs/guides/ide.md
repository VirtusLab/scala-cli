---
title: IDE support
sidebar_position: 10
---

:::warning
IDE support is under devlopment mainly because changes needs to be done in both Metals and IntelliJ.
:::


Scala CLI currently integrates with build server using [BSP protocol](https://build-server-protocol.github.io/). At this moment Scala CLI is not automatically detected so we need to use [Build Server Discovery](https://build-server-protocol.github.io/docs/server-discovery.html) from BSP protocol, namely generate connection details file (`.bsp/scala-cli.json`).

:::note
None of the following commands was run: `compile`, `run`, `test`, `setup-ide` or previously generated connection detail file was deleted, IDE will not use Scala CLI to configure workspace.

In such case, just run one of the commands above commands to recreate connection details file
:::


Since Scala CLI has a command-line-first approach and it is reflected in IDE integration. By default, Scala CLI stores options passed to last `compile`, `run` or `test` and use those options to configure IDE. 

For more control we also expose [`setup-ide` command](#) that allows to fine tune options passed to IDE. 

Once `setup-ide` is used, Scala CLI does not update configuration based on latest command. 

To enable automatic updates again, remove `.bsp` directory and run `compile`, `run` or `test` to recreate connection details file.

For now non-local sources are supported. What are non-local sources? Gists, URLs or piped sources.


Scala CLI was tested with two main IDEs for Scala:
 - [Metals](https://scalameta.org/metals/): LSP server for Scala used with [Visual Studio Code](https://code.visualstudio.com/), [Vim](https://www.vim.org/) and many other editors
 - [Intelij Idea](https://www.jetbrains.com/idea/) with [Scala Plugin](https://confluence.jetbrains.com/display/SCA/Scala+Plugin+for+IntelliJ+IDEA?_ga=2.54176744.1963952405.1634470110-410935139.1631638301) installed

In ideal world, we would replace the rest of this Guide with something along the lines of: `Scala CLI works within IDEs above as you would expect` however mainly due to how fresh Scala CLI is and also due to our radical approach to the project structure using Scala CLI -powered project to your favourite IDE may not be as amazing as we would like.

Proper IDE integration is our top priority at this moment.

## Metals

Once Metals pick up proper project structure then basic features like navigation, diagnostics or code completion should work.

Currently release Metals (0.10.7) are not able to pick changes in build structure automatically and this even includes adding new source file. In order for Metals to pick up new files or changes in build structure 'Restart build server' action is needed.

Main classes and tests are at this moment to recognized in Metals.

Generally, mainly due to problems with keeping project structure up to date Metals can assists with writing code using Scala CLI, however it cannot be the source of true and we recommend falling back to command line in such cases.

## Intellij

In terms of Intellij, the most significant problem is that Intellij requires sources to be placed within a directory. This means that top level sources are not recognized as proper sources.

With Intellij, we strongly suggested to placed your sources within a directory (like `src`).

Intellij currently does not pick up automatically changes in project structure so any change in dependencies, compiler options etc. needs manual reload.

Similarly to Metals, currently we do not advise using Intellij as a source of true and we recommend falling back to command line in such cases.