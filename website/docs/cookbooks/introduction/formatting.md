---
title: Formatting
sidebar_position: 5
---

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

Scala CLI also lets you format your code using the [Scalafmt](https://scalameta.org/scalafmt/) formatter.


## Basic usage

This section describes how to use Scala CLI to format your code using default rules. For advanced usage and all available flags, please see next section.

### Check

To check the format of all your sources in the current directory:
```bash
scala-cli fmt --check .
```

This command will check if the code is formatted correctly according to the default rules.
If it is not, it will return a diff of the changes that need to be made to match formatting rules.

### Format

If you want to format all your code, you can use the following command:
```bash
scala-cli fmt .
```
This command will format all your sources in the current directory according to the default rules.

### Example

Let's take a simple `HelloWorld` application:
```scala title=HelloWorld.scala
// use scala 3.3.0
@main def hello()={println("Hello, World!")}
def someLongMethodMoreThan80Characters():Unit={println("This is a long method")
println("Another print here?")}
```

You can notice that this implementations is written with long lines, no indentation, no spaces and no new lines.
It is hard to distinguish where one statement ends and another begins, which makes it hard to read.

<ChainedSnippets>

Let's check the format of this code:
```bash fail
scala-cli fmt --check .
```
Here is the output:
```diff
--- a/private/tmp/formatting-example/FormattingExample.scala
+++ b/private/tmp/formatting-example/FormattingExample.scala
@@ -1,4 +1,7 @@
 // use scala 3.3.0
-@main def hello()={println("Hello, World!")}
-def someLongMethodMoreThan80Characters():Unit={println("This is a long method")
-println("Another print here?")}
+@main def hello() = { println("Hello, World!") }
+def someLongMethodMoreThan80Characters(): Unit = {
+  println("This is a long method")
+  println("Another print here?")
+}
+
error: --test failed
```
</ChainedSnippets>

As we can see, the code is not correctly formatted according to the default rules and [Scalafmt](https://scalameta.org/scalafmt/) formatter provides a diff of the changes that need to be made to match them.

<ChainedSnippets>
To apply the suggested changes to our code, we can run:

```bash
scala-cli fmt .
```
And there will be no output, which means that the formatting was successful. Let's see the code after formatting:

```scala title=HelloWorld.scala reset
// use scala 3.3.0
@main def hello() = { println("Hello, World!") }
def someLongMethodMoreThan80Characters(): Unit = {
  println("This is a long method")
  println("Another print here?")
}
```
</ChainedSnippets>

Now the code is formatted according to the default rules.

<ChainedSnippets>
Let's check it again:

```bash
scala-cli fmt --check .
```

And now we seee `All files are formatted with scalafmt :)` message, which means that the code is formatted correctly.

</ChainedSnippets>

---

## Advanced usage

This section describes how to use Scala CLI to format your code using custom parameters and the `.scalafmt.conf` file.


### Default 
By default, Scala CLI creates a `.scalafmt.conf` file in the `.scala-build/.scalafmt.conf` directory with default rules. The default rules in this case are:
```hocon
version = 3.7.12
runner.dialect = scala3
```

:::note
The default version of `scalafmt` is linked to the Scala CLI version you are using. In case you want to use a different version, you can override it with the `--fmt-version` option. The defaults might also change after doing a Scala CLI version update.
:::

### Custom config

The custom configuration will be used if the `.scalafmt.conf` file is present in the root directory of the project. 
You can create a `.scalafmt.conf` file in the root directory of your project with the desired `Scalafmt` configuration.


Scala CLI also allows you to pass the relative path to the desired config with `--scalafmt-conf`, `--scalafmt-config` or `--fmt-config`. For example:

```bash fail
scala-cli fmt --check --scalafmt-conf=super.scalafmt.conf FormattingExample.scala
```

This command will check the format of the `FormattingExample.scala` file using the custom configuration from the `super.scalafmt.conf` file.

### Custom dialect

You can specify the dialect of Scala using `--dialect` or `--scalafmt-dialect` parameter. For example:

```bash
scala-cli fmt --dialect=scala3 .
```

This command will run check using Scala 3 dialect. All dialects can be found in [Scalafmt documentation](https://scalameta.org/scalafmt/docs/configuration.html#scala-dialects) in section *Scala Dialects*.

### Custom version

You can specify the version of Scalafmt using `--fmt-version` or `--scalafmt-version` parameter. For example:

```bash
scala-cli fmt --fmt-version=3.7.12 .
```

The version of Scalafmt can be found in [Scalafmt releases](https://github.com/scalameta/scalafmt/releases) on Github.

### Saving the configuration

The configuration can be saved in the `.scalafmt.conf` file in the root directory of the project automatically by running the following command:

```bash
scala-cli fmt --save-scalafmt-conf .
```

### Direct `scalafmt` arguments

You can pass any `scalafmt` arguments directly to the `scalafmt` command using `-F` or `--scalafmt-arg` parameter. For example:

```bash
scala-cli fmt -F --list
```
The full list of `scalafmt` arguments can be found in the [Scalafmt CLI documentation](https://scalameta.org/scalafmt/docs/installation.html#--help) in section *`--help`*.


---

## See also

Please see the our [Github Actions Cookbook](/docs/cookbooks/introduction/gh-action#check-your-scala-code-format) to learn how to setup checking your Scala code formating using Github Actions. And for more information about Scalafmt, please see the [Scalafmt documentation](https://scalameta.org/scalafmt/).