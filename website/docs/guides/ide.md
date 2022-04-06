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
Reloading the workspace on project structure changes is currently experimental and should work for most scenarios, we are working on improving its stability.
For some cases it may still be necessary to restart the build server manually. 
(Closing & reopening the project should also be sufficient.)

## IntelliJ

Here are a few notes related to IntelliJ support:
- IntelliJ currently does not automatically pick up changes in the project structure, so any change in dependencies, compiler options, etc., need to be manually reloaded.
- Similarly to Metals, reloading the workspace on project structure changes is currently experimental and should work for most scenarios. 
  We are working on improving its stability. For some cases it may still be necessary to restart the build server manually. 
  (Closing & reopening the project should also be sufficient.)

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
