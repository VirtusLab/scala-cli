---
title: IDE support
sidebar_position: 10
---

:::warning
IDE support is currently under development, primarily because changes needs to be made in both Metals and IntelliJ.
:::


Scala CLI currently integrates with a build server using the [BSP protocol](https://build-server-protocol.github.io/).
At this moment Scala CLI is not automatically detected, so we need to use [Build Server Discovery](https://build-server-protocol.github.io/docs/server-discovery.html) from BSP protocol to generate a connection details file (`.bsp/scala-cli.json`).

:::note
If none of these commands were run:

- `compile`
- `run`
- `test`
- `setup-ide` 

or a previously-generated connection detail file was deleted, your IDE will *not* use Scala CLI to configure your workspace.

In this case, just run one of the commands above to recreate the connection details file.
:::

Since Scala CLI has a command-line-first approach, this is reflected in its IDE integration.
By default, Scala CLI stores options passed to the last `compile`, `run`, or `test` command, and uses those options to configure the IDE.

For more control we also expose the [`setup-ide` command](../commands/setup-ide.md), which lets you fine-tune the options that are passed to the IDE.

But note that once `setup-ide` is used, Scala CLI does not update the configuration based on latest command.
To enable automatic updates again, remove the `.bsp` directory and run `compile`, `run`, or `test` to recreate the connection details file.

<!-- TODO: Does this belong here? Is it related to IDEs? -->
For now non-local sources are supported. What are non-local sources? Gists, URLs or piped sources.


Scala CLI has been tested with two main Scala IDEs:
 - [Metals](https://scalameta.org/metals/), which is an LSP server for Scala, and is used with [Visual Studio Code](https://code.visualstudio.com/), [Vim](https://www.vim.org/) and many other editors
 - [IntelliJ IDEA](https://www.jetbrains.com/idea/), with the [Scala Plugin](https://confluence.jetbrains.com/display/SCA/Scala+Plugin+for+IntelliJ+IDEA?_ga=2.54176744.1963952405.1634470110-410935139.1631638301) installed

In an ideal world we would replace the rest of this guide with something along the lines of, “Scala CLI works within IDEs above as you would expect.” However, mainly due to how fresh Scala CLI is, and also due to our radical approach to the project structure, using a Scala CLI project with your favourite IDE may not be as amazing as we would like. (That being said, proper IDE integration is our top priority at this moment!)

## Metals

Once Metals picks up the project structure that’s created by Scala CLI, basic features like navigation, diagnostics, and code completion should work.




## IntelliJ

Here are a few notes related to IntelliJ support:

- IntelliJ currently does not automatically pick up changes in the project structure, so any change in dependencies, compiler options, etc., need to be manually reloaded.
- We currently don’t advise using IntelliJ as a source of truth, and we recommend falling back to command line in such cases.
