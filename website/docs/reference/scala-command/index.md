---
title: Scala CLI as scala
---

# Scala CLI as an implementation of the `scala` command

As of Scala 3.5.0, Scala CLI has become the official runner for the language,
replacing the old runner implementation under the `scala` script.

Since Scala CLI is quite feature-packed, we do not want to expose all the features and options to all the Scala users
from the very beginning. Why is that?

- We want to make sure that the options / commands are stable.
- We do not want to overwhelm users with multiple options and commands.

That is why we built in a mechanism to limit the commands, options, directives in Scala CLI by default. However, it's
still possible to enable all features by explicitly passing the `--power` flag on the command line, or by setting it
globally running:

```bash ignore
scala config power true
```

Alternatively, it is also possible to rely on the `SCALA_CLI_POWER` environment variable to achieve the same:

```bash ignore
export SCALA_CLI_POWER=true
```

To check which options, commands and directives are supported when running Scala CLI with limited functionalities, refer
to [options](./cli-options.md), [commands](./commands.md) and [using directives](./directives.md), respectively.

## Installing Scala CLI as `scala`

Refer to the [official instructions for installing Scala](https://www.scala-lang.org/download/).

:::note
A given Scala version has a paired Scala CLI version which is used by the `scala` command installed alongside it, as per
the [official instructions](https://www.scala-lang.org/download/).
This means that even when installing the latest Scala version, its `scala` command may refer to an older Scala CLI
version.

To get the latest stable Scala CLI launcher, refer to the [separate
`scala-cli` installation instructions](../../../install).

Alternatively, you can use the `--cli-version` launcher option to specify the Scala CLI version to use.
This will run the JVM launcher of the specified Scala CLI version under the hood, so do keep in mind that it may be a
bit slower than a native launcher.
Also, the specified version (and potentially any of its dependencies, if they are not already installed) would be
downloaded if it's not available in the local cache, so it may require additional setup for isolated environments.

```bash ignore
scala --cli-version 1.5.0 version
# Scala CLI version: 1.5.0
# Scala version (default): 3.5.0
```

If the bleeding edge is what you are after, you can use the nightly version this way.
Just keep in mind that there are no guarantees about the stability of nightly versions.

```bash ignore
scala --cli-version nightly version
# Scala CLI version: 1.5.0-17-g00e4c88c1-SNAPSHOT
# Scala version (default): 3.5.0
```

:::

## Migrating from the old `scala` runner to Scala CLI

If you have been using the old `scala` runner and want to migrate to Scala CLI, refer
to [the migration guide](../../guides/introduction/old-runner-migration.md).

## Using the old (deprecated) `scala` runner with Scala 3.5+

You can still use the (deprecated as of Scala 3.5.0) legacy runner with Scala 3.5+ installed. It is available under
the `scala_legacy` command.

:::caution
Even though this enables usage of the old runner under a new alias, it is recommended to
migrate any existing scripts and automations to Scala CLI under either `scala` or `scala-cli`, as the `scala_legacy`
command may be dropped at some point in the future.
:::

```bash ignore
scala_legacy
# [warning] MainGenericRunner class is deprecated since Scala 3.5.0, and Scala CLI features will not work.
# [warning] Please be sure to update to the Scala CLI launcher to use the new features.
# [warning] Check the Scala 3.5.0 release notes to troubleshoot your installation.
# Welcome to Scala 3.5.0 (17, Java OpenJDK 64-Bit Server VM).
# Type in expressions for evaluation. Or try :help.
#                                                                                                                  
# scala> 
```
