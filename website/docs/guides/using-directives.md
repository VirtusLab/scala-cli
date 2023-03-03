---
title: Using directives
sidebar_position: 5
---

:::warning
`using` directives is an experimental language extension that may change in future versions of Scala CLI.
:::

The `using` directives mechanism lets you define configuration information within `.scala` source code files, eliminating the need for build tools to define a dedicated configuration syntax.

`using` directives are basically key-value pairs that let you provide multiple values to a single key. For instance, this command:

```scala
using foo "bar", "baz"
```

will be interpreted as assigning `bar` and `baz` to the key `foo`.

As shown, `using` directives can be defined using the special keyword `using`. However, this may break existing tools outside of Scala CLI. Therefore, `using` directives can be put in comments with a special syntax:

```scala
//> using scala "2"

/*> using options "-Xfatal-warnings" */
```

:::info
For now we recommend using the special comment (`//> using scala "3.0.2"`), and we will use that syntax in this guide.

Until `using` directives becomes a part of the Scala specification, this is the only way that guarantees that your code will work well with IDEs, code formatters, and other tools.
:::

Within one file, only one flavor of using directives can be used. The keyword-based syntax (`using scala "3"`) has precedence over special comments (`//> using scala "3"`). The deprecated, plain comments (`// using scala "3"`) have lowest priority.

For now `using` and `@using` can be mixed within a given syntax however we strongly suggest not to use deprecated `@using`.

Scala CLI reports warnings for each using directive that does not contribute to the build.

With following snippet:

```scala
using scala "3.1"
// using scala "2.13.8"
//> using scala "2.12.11"
```

Scala `3.1` will be used and following warnings would be reported:

```
[warn] ./.pg/a.scala:2:1: This using directive is ignored. File contains directives outside comments and those has higher precedence.
[warn] // using scala "2.13.8"
[warn] ^^^
[warn] ./.pg/a.scala:3:1: This using directive is ignored. File contains directives outside comments and those has higher precedence.
[warn] //> using scala "2.12.11"
[warn] ^^^^
```
## Deprecated syntax

As a part of `0.0.x` series we experimented with different syntaxes for using directives. Based on feedback and discussions with the Scala compiler team, we decided to deprecate `@using` (using annotations) and `// using` (using within plain comment). Those syntaxes will keep working in the `0.1.x` series and will result in an error starting from `0.2.x`.

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

`using` directives also support indentation and braces syntax similar to the syntax of Scala:
```scala
//> using:
//>   scala "2.13"
//>   options "-Xasync"
//>   target {
//>     scope "test"
//>     platform "jvm"
//>   }
```

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
- From a teaching perspective, the ability to provide pre-configured pieces of code that fit into one slide is also benefical.
- Having configuration close to the code is benefical, since often — especially in small programs — the given depencencies are only used within one source file.

We acknowledge that configuration distributed across many source files may be hard to maintain in the long term. Therefore, in the near feature we will introduce a set of lints to ensure that above a given project size or complexity, all configuration details will be centralized.

How can configuration that’s contained in source files be centralized?
`using` directives can be placed in any `.scala` file, so it’s possible to create a `.scala` file that contains only configuration information.
Therefore, when your project needs to centralize its configuration, we recommend creating a `conf.scala` file, and placing the configuration there.
We plan to add ways to Scala CLI to migrate these settings into a centralized location with one command or click.

We are aware that `using` directives may be a controversial topic, so we’ve created a [dedicated space for discussing `using` directives](https://github.com/VirtusLab/scala-cli/discussions/categories/using-directives-and-cmd-configuration-options).


## How to comment out using directives?

Using directives are part of the code so similarly, developers should be able to comment them out. Until 0.2.x when plain comment syntax will be removed commenting out using directives requires special care.

Paradoxically, commenting out comment-based directives does not cause any problems. Below, some examples how to do it:

```scala compile
// //> using dep "no::lib:123"
```

```scala compile
// // using dep "no::lib:123"
```

Until plain using directives in plain comments are supported, commenting keyword base syntax require some attention. Let' assume that we have a following code:

```scala fail
using scala "3.1.1"
using dep "no::lib:123"
```

and we want to comment out broken using directive: `lib "no::lib:123"` when we simply comment it out we will actually turn it into a using directive that is using a plain comment syntax!

```scala compile
using scala "3.1.1"
// using dep "no::lib:123"
```

In cases where there are other uncommented directives, scala-cli will ignore that directives, producing a warning. In cases that this is the only directive in the file, the commented directive will be still used to configure build.

In such cases we suggest to use triple `/` for single line comments, or use `//` withing multiline comments:

```scala compile
/// using dep "in::single-line-comments:123"
/*
// using dep "in::multiline-line-comments:123"
*/
```

Generally, our recommendation is to not use keyword based directives until scala-cli will stop supporting plain comments-based directives.
