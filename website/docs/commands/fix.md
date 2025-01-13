---
title: Fix ⚡️
sidebar_position: 28
---

:::caution
The Fix command is experimental and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

The `fix` command is used to check, lint, rewrite or otherwise rearrange code in a Scala CLI project.

Currently, the following sets of rules are supported:
- built-in rules (enabled automatically and controlled with the `--enable-built-in` flag)
- `scalafix`, running [Scalafix](https://scalacenter.github.io/scalafix/) under the hood (enabled automatically and controlled with `--enable-scalafix` flag).

You can disable unnecessary rule sets when needed.
For example, to disable built-in rules, you can run:
```bash
scala-cli fix . --power --enable-built-in=false
```

## Built-in rules

Currently, the only built-in rule is extraction of `using` directives into the `project.scala` configuration file.
This allows to fix warnings tied to having `using` directives present in multiple files and eliminate duplicate directives.
Files containing (experimental) `using target` directives, e.g. `//> using target.scala 3.0.0` will not be changed by `fix`.
The original scope (`main` or `test`) of each extracted directive is respected. `main` scope directives are transformed 
them into their `test.*` equivalent when needed.

## `scalafix` integration

Scala CLI is capable of running [Scalafix](https://scalacenter.github.io/scalafix/) (a refactoring and linting tool for Scala) on your project.
Before using this command you need to provide the configuration at `.scalafix.conf`.
For example:
``` text title=.scalafix.conf
// .scalafix.conf
rules = [
  DisableSyntax
]
```

Then you can run it:
```bash
scala-cli fix . --power
```

If you’re setting up a continuous integration (CI) server, Scala CLI also has you covered.
You can run linter using the `--check` flag:
```bash fail
scala-cli fix --check . --power
```

Read more about Scalafix:
- [Configuration](https://scalacenter.github.io/scalafix/docs/users/configuration.html)
- [Rules](https://scalacenter.github.io/scalafix/docs/rules/overview.html)

### Using external rules

Adding an [external scalafix rule](https://scalacenter.github.io/scalafix/docs/rules/external-rules.html) to Scala CLI can be done with the [`scalafix.dep`](./compile.md#compile-only-dependencies) directive:
```scala compile power
//> using scalafix.dep com.github.xuwei-k::scalafix-rules:0.5.1
```