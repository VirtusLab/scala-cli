---
title: Scala CLI as scala
---

# Scala CLI as implementation for `scala` command

Scala CLI is designed to be a replacement for script that is currently installed as `scala`. Since Scala CLI is
feature-packed we do not want to expose all the features and options to the whole Scala public at the very start. Why is
that?

- We want to make sure that the options / commands are stable
- We do not want to overwhelm users with multiple options and commands
- We want to make sure that the commands we add to `scala` are stable so once we commited to supporting given option it
  may be hard to remove it later

That is why we built in a mechanism to limit the commands, options, directives in Scala CLI by default. However, it's
still possible to enable all features by explicitly passing the `--power` flag on the command line, or by setting it
globally running:

```bash ignore
scala-cli config power true
```

To check which options, commands and directives are supported when running Scala CLI with limited functionalities, refer
to [options](./cli-options.md), [commands](./commands.md) and [using directives](./directives.md), respectively.

## Testing Scala CLI as `scala`

There are two recommended ways to test and use Scala CLI:

- with brew:

```bash ignore
brew install virtuslab/scala-experimental/scala
```

- with coursier:

```bash ignore
cs setup
cs install scala-experimental ← this command will replace the default scala runner
```

Alternatively, you can rename your `scala-cli` executable or alias it as `scala`.

## Migrating from the old `scala` runner to Scala CLI

If you have been using the old `scala` runner and want to migrate to Scala CLI, refer
to [the migration guide](../../guides/old-runner-migration.md).
