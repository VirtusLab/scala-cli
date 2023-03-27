---
title: Introduction
sidebar_position: 1
---

# Guides

This section covers some aspects of Scala CLI that apply across various commands.
We have divided the available guides into categories, so that it's easier to find a relevant topic.

For concrete recipes that show you how to use Scala CLI in particular situations, refer to
the [cookbooks section](../cookbooks/intro.md).

## Introductory guides

These few guides are a good starting point when learning how to use Scala CLI.

- [Configuration](./configuration.md) - learn how to configure various options, and what configuration styles are best
  for each use case
- [Dependencies](./dependencies.md) - learn how to define dependencies within a Scala CLI project.
- [Updating dependencies](./update-dependencies.md) - learn about how to keep your dependencies up-to-date automatically with
  Scala CLI.
- [`using` directives](./using-directives.md) - Scala CLI’s syntax that lets you store configuration information
  directly in source files
- [IDE support](./ide.md) - how to import and use Scala CLI-based projects in your favorite IDE.
- [Migrating from the old `scala` runner](./old-runner-migration.md) - an in-depth look at all the differences between Scala CLI and the old `scala` script.

## Scripting guides

Guides on how to get started with Scala scripting with Scala CLI.

- [Scripting guide](./scripts.md) - covers how Scala CLI allows for powerful scripting with Scala.
- [Shebang](./shebang.md) - explains how to use the `shebang` sub-command in a script's shebang header.

## Advanced guides

Less introductory guides on specific topics.

- [Scala.js](./scala-js.md) and [Scala Native](./scala-native.md) - learn how Scala CLI supports these non-JVM platforms
- [Piping](./piping.md) - covers how Scala CLI allows to work with piped sources.
- [Snippets](./snippets.md) - learn how to use command line snippets with Scala CLI.
- [Verbosity](./verbosity.md) - learn how to control logs verbosity in Scala CLI.
- [Internals](./internals.md) - learn about how Scala CLI works under the hood.

## ⚡️ `--power` mode guides

- [SBT and Mill export](./sbt-mill.md) - learn how to convert your Scala CLI project into an SBT or Mill project (when
  you need a more powerful build tool).
- [proxies](./proxies.md) - learn how to configure Scala CLI to work with proxies.
- [Markdown](./markdown.md) - learn how to work with `.md` sources.