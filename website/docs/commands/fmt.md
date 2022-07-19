---
title: Format
sidebar_position: 15
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

Scala CLI supports formatting your code using [Scalafmt](https://scalameta.org/scalafmt/):

```bash
scala-cli fmt
```

Under the hood, `scala-cli` downloads and runs Scalafmt on your code.

If you’re setting up a continuous integration (CI) server, `scala-cli` also has you covered.
You can check formatting correctness using a `--check` flag:

```bash
scala-cli fmt --check
```

### Scalafmt version and dialect

Scala CLI `fmt` command supports passing the `scalafmt` **version** and **dialect** directly from the command line, using respectively the `--scalafmt-dialect` and `--scalafmt-version` options:
```
scala-cli fmt --scalafmt-dialect scala3 --scalafmt-version 3.5.8
```
You can skip passing either of those, which will make Scala CLI to infer a default value:
- If a `.scalafmt.conf` file is present in the workspace and it has the field defined, the value will be read from there, unless explicitly specified with Scala CLI options.
- Otherwise, the default `scalafmt` **version** will be the latest one used by your Scala CLI version (so it is subject to change when updating Scala CLI). The default **dialect** will be inferred based on Scala version (defined explicitly by `-S` option, or default version if option would not be passed).

#### Example 1

``` text title=.scalafmt.conf
version = "3.5.8"
runner.dialect = scala212
```

```bash
scala-cli fmt --scalafmt-dialect scala213
```

For above setup `fmt` will use:
- `version="3.5.8"` from the file
- `dialect=scala213`, because passed `--scalafmt-dialect` option overrides dialect found in the file

#### Example 2

``` text title=.scalafmt.conf
version = "2.7.5"
```

```bash
scala-cli fmt --scalafmt-version 3.5.8
```

For above setup `fmt` will use:
- `version="3.5.8"`, because passed `--scalafmt-version` option overrides version from the file
- `dialect=scala3`, because dialect is neither passed as an option nor is it present in the configuration file, so it is inferred based on the Scala version; the Scala version wasn't explicitly specified in the command either, so it falls back to the default Scala version - the latest one, thus the resulting dialect is `scala3`. 

### Scalafmt options

It is possible to pass native `scalafmt` options with the `-F` (short for `--scalafmt-arg`), for example:

<ChainedSnippets>

```bash
scala-cli fmt -F --version
```

```text
scalafmt 3.5.8
```

</ChainedSnippets>

For the available options please refer to `scalafmt` help, which can be viewed with the `--scalafmt-help` option (which
is just an alias for `-F --help`):

<ChainedSnippets>

```bash
scala-cli fmt --scalafmt-help
```

```text
scalafmt 3.5.8
Usage: scalafmt [options] [<file>...]

  -h, --help               prints this usage text
  -v, --version            print version 
(...)
```

</ChainedSnippets>

### Excluding sources

Because of the way Scala CLI invokes `scalafmt` under the hood, sources are always being passed to it explicitly. This
in turn means that regardless of how the sources were passed, `scalafmt` exclusion paths (the `project.excludePaths`)
would be ignored. In order to prevent that from happening, the `--respect-project-filters` option is set to `true` by
default.

```text title=.scalafmt.conf
version = "3.5.8"
runner.dialect = scala3
project {
  includePaths = [
    "glob:**.scala",
    "regex:.*\\.sc"
  ]
  excludePaths = [
    "glob:**/should/not/format/**.scala"
  ]
}
```

<ChainedSnippets>

```bash
scala-cli fmt . --check
```

```text
All files are formatted with scalafmt :)
```

</ChainedSnippets>

You can explicitly set it to false if you want to disregard any filters configured in the `project.excludePaths` setting
in your `.scalafmt.conf` for any reason.

<ChainedSnippets>

```bash
scala-cli fmt . --check --respect-project-filters=false
```

```text
--- a/.../should/not/format/ShouldNotFormat.scala
+++ b/.../should/not/format/ShouldNotFormat.scala
@@ -1,3 +1,3 @@
 class ShouldNotFormat {
-                       println()
+  println()
 }
```

</ChainedSnippets>

### How `.scalafmt.conf` file is generated

The Scala CLI `fmt` command runs `scalafmt` under the hood, which *normally* requires `.scalafmt.conf` configuration file with explicitly specified **version** and **dialect** fields. The way it is handled by Scala CLI is as follows:

At the beginning `fmt` looks for existing `.scalafmt.conf` file inside **current workspace** directory and if it doesn't find it - inside **git root** directory. There are 3 possible cases:

1. Configuration file with the specified version and dialect is found.
2. Configuration file is found, but it doesn't have specified version or dialect.
3. Configuration file is not found.

- In the **first** case `fmt` uses the found `.scalafmt.conf` file to run `scalafmt`.
- In the **second** case `fmt` creates a `.scalafmt.conf` file inside the `.scala-build` directory. Content of the previously found file is copied into the newly created file, missing parameters are [inferred](/docs/commands/fmt#scalafmt-version-and-dialect) and written into the same file. Created file is used to run `scalafmt`. 
- In the **third** case `fmt` creates a `.scalafmt.conf` file inside the `.scala-build` directory, writes [inferred](/docs/commands/fmt#scalafmt-version-and-dialect) version and dialect into it and uses it to run `scalafmt`.

If the `--save-scalafmt-conf` option is passed, then `fmt` command behaves as follows:
- In the **first** case `fmt` uses the found `.scalafmt.conf` file to run `scalafmt`.
- In the **second** case `fmt` [infers](/docs/commands/fmt#scalafmt-version-and-dialect) missing parameters, writes them directly into the previously found file and then uses this file to run `scalafmt`.
- In the **third** case `fmt` creates a `.scalafmt.conf` file in the current workspace directory, writes [inferred](/docs/commands/fmt#scalafmt-version-and-dialect) version and dialect into it and uses it to run `scalafmt`.
