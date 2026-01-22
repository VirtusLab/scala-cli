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
- `scalafix`, running [Scalafix](https://scalacenter.github.io/scalafix/) under the hood (enabled automatically and controlled with `--enable-scalafix` flag)

- dependency analysis (opt-in with `--check-unused-deps` and `--check-explicit-deps` flags)

You can disable unnecessary rule sets when needed.
For example, to disable built-in rules, you can run:
```bash
scala-cli fix . --power --enable-built-in=false
```

## Built-in rules

Currently, built-in rules include:
- extraction of `using` directives into the `project.scala` configuration file
- dependency analysis (unused and missing explicit dependencies)

### Directive Extraction

Extraction of `using` directives allows to fix warnings tied to having `using` directives present in multiple files and eliminate duplicate directives.
Files containing (experimental) `using target` directives, e.g. `//> using target.scala 3.0.0` will not be changed by `fix`.
The original scope (`main` or `test`) of each extracted directive is respected. `main` scope directives are transformed 
them into their `test.*` equivalent when needed.

Exceptions:
- directives won't be extracted for single-file projects;
- directives in test inputs with no test scope equivalents won't be extracted to preserve their initial scope.

### Dependency analysis

Scala CLI can analyze your project's dependencies to help you maintain a clean and explicit dependency graph.
This feature is inspired by tools like [sbt-explicit-dependencies](https://github.com/cb372/sbt-explicit-dependencies) 
and [mill-explicit-deps](https://github.com/kierendavies/mill-explicit-deps).

#### Detecting unused dependencies

Use the `--check-unused-deps` (or `--detect-unused-deps`) flag to detect dependencies that are declared but not used in your code:

```bash
scala-cli fix . --power --check-unused-deps
```

This will analyze your import statements and report any dependencies that don't appear to be used. For example:

```text
⚠ Found 2 potentially unused dependencies:

  • com.lihaoyi:upickle_3:3.1.0
    No imports found that could be provided by this dependency
    Consider removing: //> using dep com.lihaoyi::upickle:3.1.0

  • org.typelevel:cats-core_3:2.9.0
    No imports found that could be provided by this dependency
    Consider removing: //> using dep org.typelevel::cats-core:2.9.0

Note: This analysis is based on import statements and may produce false positives.
Dependencies might be used via reflection, service loading, or other mechanisms.
```

#### Detecting missing explicit dependencies

Use the `--check-explicit-deps` (or `--detect-explicit-deps`) flag to detect transitive dependencies that you're using directly but haven't declared explicitly:

```bash
scala-cli fix . --power --check-explicit-deps
```

This will analyze your import statements and report any transitive dependencies that you're importing directly:

```text
⚠ Found 1 transitive dependencies that are directly used:

  • org.scala-lang.modules:scala-xml_3:2.1.0
    Directly imported but not explicitly declared (transitive through other dependencies)
    Used in: Main.scala
    Consider adding: //> using dep org.scala-lang.modules:scala-xml_3:2.1.0

Note: These dependencies are currently available transitively but should be declared explicitly.
This ensures your build remains stable if upstream dependencies change.
```

#### Running both checks together

You can run both dependency checks simultaneously:

```bash
scala-cli fix . --power --check-unused-deps --check-explicit-deps
```

**Note:** Dependency analysis is based on static analysis of import statements and may not catch all cases.
Dependencies used via reflection, service loading, annotation processing, or other dynamic mechanisms may be 
incorrectly flagged as unused. Always verify the suggestions before removing dependencies.

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