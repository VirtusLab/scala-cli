---
title: Using directives
sidebar_position: 11
---

:::warning
Using directives is an experimental language extension that may change in future versions of Scala CLI
:::

Using directives mechanism allows to define configuration within the .scala sources itself eliminating need for build tools to define dedicated configuration syntax. Scala compiler treats using directives like special kinds of comments.

Using directives as basically key-value paris allowing to providing multiple values to single key:

```scala
using foo bar baz
```

will be interpreted as assigning `bar` an `baz` to key `foo`.

Using directives can be defined using special keyword `using` (however it may not be compatible with exisiting tools outside Scala CLI), using them within comments `// using scala 2` or as a special top-level annotation `@using jars libs/model.jar`.

:::info
For now we recommend using comment-flavor (e.g. `// using scala 3.0.2`) and we will stick it in this guide.

Until using directives becomes a part of Scala specification, this is the only way that guarantee that your code will work well with IDE, formatters or similar tool.
:::

Using directives can be declared only **before any other Scala code**.

Using directives contributes settings to the whole compilation scope where given .scala file is defined so a library or compiler option defined in one file applies to the whole application or test dependeding if source is test or not.

The only exception are `using target` directives that applies only to the given file. `using target` is a marker to assigned given file to given target (e.g. test or main sources).

**We believe that syntax similar to using directives should become a part of Scala in the future**

## Using directives in Scala CLI

Below is the list of most important using directives that Scala CLI supports and full list can be found in the [Reference section of this documentation](./reference/directives.md).

### Most important using directives supported by Scala CLI

- `// using scala <scala-version>` - defines version of Scala used
- `// using lib org::name:version` - defines dependency to given library [more in dedicated guide](./guides/dependencies.md)
- `// using resource <file-or-dir>` - marks file/directory as resources. Resources accessible at runtime and packaged together with compiled code.
- `// using java-opt <opt>` - use given java options when running application or tests
- `// using target [test|main]` used to marked or unmarked given source as test
- `// using test-framework` - select test framework to use

## Why not dedicated configuration file?

One of the main use cases of the Scala CLI is prototyping and ability to ship the code with complte configuration is a game changer. Defining dependencies or other settings is quite common in Ammonite scripts as well. From the learning perspective, ability to provide pre-configured pieces of code that fits into one slide is benefical.

Having configuration close to the code is benefical, since often (especially in small programs) given depencencies are used only within one source file. We acknowledge that configuration distributed across many source files may be hard to maintain in the long term, so in near feature we will introduce set of lints to ensure that above given project size or complexity all configuration will be centralized.

How configuration in source file can be centralized? Using directives can be placed in any .scala files so such file may contains only configuration. When your projects needs to centralize its configuration we recommend to create `conf.scala` file and move configuration there. We plan to add ways to Scala CLI to migrate setting into centralized location with one command or click.

We are aware that using directives may be a controversial topic and that is why we have create a [dedicated space for discussion](https://github.com/VirtusLab/scala-cli/discussions/categories/using-directives-and-cmd-configuration-options) about using directives.
