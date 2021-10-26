---
title: Format
sidebar_position: 15
---

Scala-cli supports formatting your code using [Scalafmt](https://scalameta.org/scalafmt/):

```bash
scala-cli fmt
```

Under the hood scala-cli will download and run Scalafmt over your code.

If you are working on setting up a CI, scala-cli also has you covered.
You can check formatting correctness using a `--check` flag:

```bash
scala-cli fmt --check
```

### Dialects
Scala-cli also supports dialects that are passed to the formatter.
This value will only be used if there is no `.scalafmt.conf` file. If that file exists, then all configuration should be placed there.
For a list of all possible values you may want to consult the [official documentation](https://scalameta.org/scalafmt/docs/configuration.html#scala-dialects)

```bash
scala-cli fmt --dialect scala212
```

### Current limitations
Right now scala-cli doesn't read the `.scalafmt.conf` files,
therefore in some scenarios versions of those two may be mismatched.
It is possible to set the version manually if you encounter any issues:

```bash
scala-cli fmt --scalafmt-tag v3.0.3
```
