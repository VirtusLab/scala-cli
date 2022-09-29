---
title: Scala CLI as scala
---


# Scala CLI as implementation for `scala` command

Scala CLI is designed to be a replacement for script that is currently installed as `scala`. Since Scala CLI is feature-packed we do not want to expose all of the features and options to the whole Scala public at the very start. Why is that?
 - We want to make sure that the options / commands are stable
 - We do not want to overwhelm users with multiple options and commands
 - We want to make sure that the commands we add to `scala` are stable so once we commited to supporting given option it may be hard to remove it later

That is why we built in a mechanism to limit the commands, options, directives based if Scala CLI is run as `scala` or `scala-cli`. Mainly for SIP submission we have prepared a pages with supported [options](./cli-options.md), [commands](./commands.md) and [using directives](./directives.md) when running Scala CLI as `scala` or `scala-cli-sip`.

## Testing Scala CLI as `scala`

The recommended way to test and use Scala CLI, as it will be `scala` command is to create a link to the existing Scala CLI binary named `scala-cli-sip` and add it to PATH.

In future it will be also possible to use coursier as well, for now there is a [bug](https://github.com/VirtusLab/scala-cli/issues/1307) and Scala CLI does not recognize it is installed as `scala-cli-sip` or `scala` when installed using coursier.