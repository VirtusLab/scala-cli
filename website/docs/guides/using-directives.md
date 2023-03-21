---
title: Using directives
sidebar_position: 5
---

:::warning
`using` directives is an experimental language extension that may change in future versions of Scala CLI.
:::

The `using` directives mechanism lets you define configuration information within `.scala` source code files, eliminating the need for build tools to define a dedicated configuration syntax.

`using` directives are basically key-value pairs that let you provide multiple values to a single key. These directives need to be put in comments with a special syntax. For instance, this command:

```scala
//> using foo "bar", "baz"
```

Scala CLI reports warnings for each using directive that does not contribute to the build, which includes all removed alternatives to special comment using directives.

With following snippet:

```scala
using scala "3.1"
// using scala "2.13.8"
//> using scala "2.12.11"
```

Scala `2.12.11` will be used and following warnings would be reported:

```
[warn] ./.pg/a.scala:1:1: This using directive is ignored. Only using directives starting with //> are supported.
[warn] using scala "3.1"
[warn] ^^^
[warn] ./.pg/a.scala:2:1: Using directive using plain comments are deprecated. Please use a special comment syntax: '//> ...'.
[warn] // using scala "2.13.8"
[warn] ^^^^
```

## Deprecated syntax

As a part of `0.0.x` series we experimented with different syntaxes for using directives. Based on feedback and discussions with the Scala compiler team, we decided to remove `@using` (using annotations), `// using` (using within plain comment) and `using` code directives. Those syntaxes will keep working in the `0.1.x` series and will result in an error starting from `1.0.x`.

Scala CLI produces warnings if any of the syntaxes above is used:

```
[warn] ./.pg/a.scala:1:1: Using directive using plain comments are deprecated, please add `>` to each comment.
[warn] // using scala "3"
[warn] ^^^
```

```
[warn] ./.pg/a.scala:1:1: Deprecated using directive syntax, please use keyword `using`.
[warn] @using scala "3"
[warn] ^^^^^^
```

## Semantics

`using` directives can be only declared **before any other Scala code**:

```scala
//> using scala "2.13"
//> using platform "scala-js"
//> using options "-Xasync"

// package statements, import statements and other code follows ...
```

`using` directives contribute settings to the whole compilation scope where a given `.scala` file is defined.
This means that a library or compiler option defined in one file applies to the whole application or test (depending on whether the source file is a test, or not).

The only exceptions are `using target` directives, which only apply to the given file.
`using target` is a marker to assign a given file to a given target (e.g., test or main sources).

**We believe that syntax similar to `using` directives should become a part of Scala in the future.**

## `using` directives in the Scala CLI

Below is a list of the most important `using` directives that Scala CLI supports. The full list can be found in the [Reference section of this documentation](/docs/reference/directives.md).

- `//> using scala "<scala-version>"` - defines version of Scala used
- `//> using dep "org::name:version"` - defines dependency to a given library [more in dedicated guide](/docs/guides/dependencies.md)
- `//> using dep "org:name:version"`  - defines dependency to a given **java** library, note the `:` instead of `::`
- `//> using dep "org::name:version,url=url"` - defines dependency to a given library with a fallback to its jar url
- `//> using resourceDir "dir"` - marks directory as source of resources. Resources accessible at runtime and packaged together with compiled code.
- `//> using javaOpt "opt"` - use given java options when running application or tests
- `//> using target.scope "test"` used to marked or unmarked given source as test
- `//> using testFramework "framework"` - select test framework to use

There are several reasons that we believe `using` directives are a good solution:

- One of the main Scala CLI use cases is prototyping, and the ability to ship one or more source code files with a complete configuration is a game-changer for this use case.
- Defining dependencies and other settings is common in Ammonite scripts as well.
- From a teaching perspective, the ability to provide pre-configured pieces of code that fit into one slide is also beneficial.
- Having configuration close to the code is beneficial, since often — especially in small programs — the given dependencies are only used within one source file.

We acknowledge that configuration distributed across many source files may be hard to maintain in the long term. Therefore, in the near feature we will introduce a set of lints to ensure that above a given project size or complexity, all configuration details will be centralized.

How can configuration that’s contained in source files be centralized?
`using` directives can be placed in any `.scala` file, so it’s possible to create a `.scala` file that contains only configuration information.
Therefore, when your project needs to centralize its configuration, we recommend creating a `project.scala` file, and placing the configuration there.
We plan to add ways to Scala CLI to migrate these settings into a centralized location with one command or click.

We are aware that `using` directives may be a controversial topic, so we’ve created a [dedicated space for discussing `using` directives](https://github.com/VirtusLab/scala-cli/discussions/categories/using-directives-and-cmd-configuration-options).


## How to comment out using directives?

Using directives are part of the code so similarly, developers should be able to comment them out. 

Commenting out comment-based directives does not cause any problems. Below, some examples how to do it:

```scala compile
// //> using dep "no::lib:123"
```

```scala compile
// // using dep "no::lib:123"
```

