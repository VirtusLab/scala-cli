---
title: Using directives
sidebar_position: 5
---

The `using` directives mechanism lets you define configuration information within `.scala` source code files,
eliminating the need for build tools to define a dedicated configuration syntax.

`using` directives are basically key-value pairs that let you provide multiple values to a single key. These directives
need to be put in comments with a special syntax. For instance, this command:

```scala
//> using foo bar baz
```

## Deprecated syntax

As a part of `0.0.x` series we experimented with different syntaxes for using directives. Based on feedback and
discussions with the Scala compiler team, we decided to remove `@using` (using annotations), `// using` (using within
plain comment) and `using` code directives. Those syntaxes will keep working in the `0.1.x` series and will be ignored starting from `1.0.x`.

## Semantics

`using` directives can be only declared **before any other Scala code**:

```scala
//> using scala 2.13
//> using platform scala-js
//> using options -Xasync

// package statements, import statements and other code follows ...
```

`using` directives contribute settings to the whole compilation scope where a given `.scala` file is defined.
This means that a library or compiler option defined in one file applies to the whole application or test (depending on
whether the source file is a test, or not).

The only exceptions are `using target` directives, which only apply to the given file.
`using target` is a marker to specify requirements for the file to be used (e.g. Scala version, platform, or scope).

:::caution
The `using target` directives are an experimental feature, and may change in future versions of Scala CLI.
:::

**We believe that syntax similar to `using` directives should become a part of Scala in the future and will already be included within the Scala runner itself**

## `using` directives in the Scala CLI

Below is a list of the most important `using` directives that Scala CLI supports. The full list can be found in
the [Reference section of this documentation](/docs/reference/directives.md).

- `//> using scala <scala-version>` - defines version of Scala used
- `//> using dep org::name:version` - defines dependency to a given
  library [more in dedicated guide](/docs/guides/introduction/dependencies.md)
- `//> using dep org:name:version`  - defines dependency to a given **java** library, note the `:` instead of `::`
- `//> using dep org::name:version,url=url` - defines dependency to a given library with a fallback to its jar url
- `//> using resourceDir dir` - marks directory as source of resources. Resources accessible at runtime and packaged
  together with compiled code.
- `//> using javaOpt opt` - use given java options when running application or tests
- `//> using testFramework framework` - select test framework to use

There are several reasons that we believe `using` directives are a good solution:

- One of the main Scala CLI use cases is prototyping, and the ability to ship one or more source code files with a
  complete configuration is a game-changer for this use case.
- Defining dependencies and other settings is common in Ammonite scripts as well.
- From a teaching perspective, the ability to provide pre-configured pieces of code that fit into one slide is also
  beneficial.
- Having configuration close to the code is beneficial, since often — especially in small programs — the given
  dependencies are only used within one source file.

We acknowledge that configuration distributed across many source files may be hard to maintain in the long term.
Therefore, in the near feature we will introduce a set of lints to ensure that above a given project size or complexity,
all configuration details will be centralized.

How can configuration that’s contained in source files be centralized?
`using` directives can be placed in any `.scala` file, so it’s possible to create a `.scala` file that contains only
configuration information.
Therefore, when your project needs to centralize its configuration, we recommend creating a `project.scala` file, and
placing the configuration there.
We plan to add ways to Scala CLI to migrate these settings into a centralized location with one command or click.

We are aware that `using` directives may be a controversial topic, so we’ve created
a [dedicated space for discussing `using` directives](https://github.com/VirtusLab/scala-cli/discussions/categories/using-directives-and-cmd-configuration-options).

### Explicit handling of paths in using directives

The `${.}` pattern in directive values will be replaced by the parent directory of the file containing the
directive. This makes it possible for example to generate coverage output files relative to the source file location.

```scala
//> using options -coverage-out:${.}
```

However, if you want to include the `${.}` pattern in the directive value without it being replaced, you can precede it
with two dollar signs (`$$`), like this:

```scala
//> using options -coverage-out:$${.}
```

## How to comment out using directives?

Using directives are part of the code so similarly, developers should be able to comment them out.

Commenting out comment-based directives does not cause any problems. Below, some examples how to do it:

```scala compile
// //> using dep "no::lib:123"
```

```scala compile
// // using dep "no::lib:123"
```

## Directives with a test scope equivalent

Some directives have a test scope equivalent. For example, `using dep` has `using test.dep`, which allows to declare
dependencies that are only used in tests outside test-specific sources.

For example, this way you can declare the dependency to `munit` in `project.scala` like this:

```scala title=project.scala
//> using test.dep org.scalameta::munit::0.7.29
```

The dependency will then only be available in test sources.
It's effectively an equivalent to just `using dep` inside of a test source (except you can define it anywhere):

```scala title=src/test/scala/Tests.scala
//> using dep org.scalameta::munit::0.7.29
```

Directives with a test scope equivalent:

```scala compile
//> using test.dep org.scalameta::munit::0.7.29
//> using test.jar path/to/dep.jar
//> using test.sourceJar path/to/some-sources.jar
//> using test.javaOpt -Dfoo=bar
//> using test.javacOpt source 1.8 target 1.8
//> using test.javaProp foo1=bar1
//> using test.option -Xfatal-warnings
//> using test.resourceDir testResources
//> using test.toolkit latest
```
