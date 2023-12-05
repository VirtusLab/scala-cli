---
title: Index
sidebar_position: 1
---

# Guides

This section covers some aspects of Scala CLI that apply across various commands.
We have divided the available guides into categories, so that it's easier to find a relevant topic.

For concrete recipes that show you how to use Scala CLI in particular situations, refer to
the [cookbooks section](../cookbooks/intro.md).

## Introductory guides

These few guides are a good starting point when learning how to use Scala CLI.

- [Configuration](introduction/configuration.md) - learn how to configure various options, and what configuration styles are best
  for each use case
- [Dependencies](introduction/dependencies.md) - learn how to define dependencies within a Scala CLI project.
- [Updating dependencies](introduction/update-dependencies.md) - learn about how to keep your dependencies up-to-date automatically with
  Scala CLI.
- [`using` directives](introduction/using-directives.md) - Scala CLI’s syntax that lets you store configuration information
  directly in source files
- [IDE support](introduction/ide.md) - how to import and use Scala CLI-based projects in your favorite IDE.
- [Scala Toolkit](introduction/toolkit.md) - how to use the [Scala Toolkit](https://github.com/scala/toolkit) dependency batch (and other dependency batches) in a Scala CLI project.
- [Migrating from the old `scala` runner](introduction/old-runner-migration.md) - an in-depth look at all the differences between Scala CLI and the old `scala` script.

## Scripting guides

Guides on how to get started with Scala scripting with Scala CLI.

- [Scripting guide](scripting/scripts.md) - covers how Scala CLI allows for powerful scripting with Scala.
- [Shebang](scripting/shebang.md) - explains how to use the `shebang` sub-command in a script's shebang header.

## Advanced guides

Less introductory guides on specific topics.

- [Scala.js](advanced/scala-js.md) and [Scala Native](advanced/scala-native.md) - learn how Scala CLI supports these non-JVM platforms
- [Piping](advanced/piping.md) - covers how Scala CLI allows to work with piped sources.
- [Snippets](advanced/snippets.md) - learn how to use command line snippets with Scala CLI.
- [Verbosity](advanced/verbosity.md) - learn how to control logs verbosity in Scala CLI.
- [Java properties](advanced/java-properties.md) - learn how to pass Java properties to Scala CLI.
- [Internals](advanced/internals.md) - learn about how Scala CLI works under the hood.

## ⚡️ `--power` mode guides

- [SBT and Mill export](power/sbt-mill.md) - learn how to convert your Scala CLI project into an SBT or Mill project (when
  you need a more powerful build tool).
- [proxies](power/proxy.md) - learn how to configure Scala CLI to work with proxies.
- [Markdown](power/markdown.md) - learn how to work with `.md` sources.
- [Python/ScalaPy](power/python.md) - learn how to use Python libraries in Scala CLI projects.
- [offline mode](power/offline.md) - learn how to use Scala CLI in offline mode.
- [repositories](power/repositories.md) - learn how to configure Scala CLI to work with custom repositories.