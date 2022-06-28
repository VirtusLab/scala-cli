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

### Dialects

Scala CLI also supports dialects that are passed to the formatter.
This value is only used if there is no `.scalafmt.conf` file.
However, if it exists, then all configuration should be placed there.
For a list of all possible values, consult
the [official Scala Dialects documentation](https://scalameta.org/scalafmt/docs/configuration.html#scala-dialects):

```bash
scala-cli fmt --dialect scala212
```

### Scalafmt version

At this time, Scala CLI reads a `scalafmt` version from `.scalafmt.conf` files. If the version is missing, Scala CLI
throws an error, stating that users should declare an explicit Scalafmt version. Since Scalafmt `3.5.0`, this parameter
is mandatory.

To configure the Scalafmt version, add the following to `.scalafmt.conf`. For example, to set the version to `3.5.0`,
add the following line:

```
version = "3.5.0"
```

### Scalafmt options

It is possible to pass native `scalafmt` options with the `-F` (short for `--scalafmt-arg`), for example:

<ChainedSnippets>

```bash
scala-cli fmt -F --version
```

```text
scalafmt 3.5.2
```

</ChainedSnippets>

For the available options please refer to `scalafmt` help, which can be viewed with the `--scalafmt-help` option (which
is just an alias for `-F --help`):

<ChainedSnippets>

```bash
scala-cli fmt --scalafmt-help
```

```text
scalafmt 3.5.2
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
version = 3.5.2
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
