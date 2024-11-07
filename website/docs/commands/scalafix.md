---
title: Scalafix ⚡️
sidebar_position: 80
---

:::caution
The Scalafix command is experimental and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

Scala CLI runs [Scalafix](https://scalacenter.github.io/scalafix/) - Refactoring and Linting tool for Scala

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
scala-cli scalafix . --power
```

If you’re setting up a continuous integration (CI) server, Scala CLI also has you covered.
You can run linter using a `--check` flag:
```bash fail
scala-cli scalafix --check . --power
```

Read more about Scalafix:
- [Configuration](https://scalacenter.github.io/scalafix/docs/users/configuration.html)
- [Rules](https://scalacenter.github.io/scalafix/docs/rules/overview.html)


### Using external rules

Adding an [external scalafix rule](https://scalacenter.github.io/scalafix/docs/rules/external-rules.html) to scala-cli might be done by declaring [`scalafix.dep`](./compile.md#compile-only-dependencies):
```scala title=externalRule.scala
//> using scalafix.dep "com.github.xuwei-k::scalafix-rules:0.5.1"
```
