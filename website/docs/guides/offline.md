---
title: Offline mode ⚡️
sidebar_position: 51
---

:::caution
Offline mode is an experimental feature.

Please bear in mind that non-ideal user experience should be expected.
If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team
on [GitHub](https://github.com/VirtusLab/scala-cli).
:::

The offline mode for Scala CLI has been introduced to be used in two situations:
- you want to have more control over the artifacts being downloaded
- your development environment has restricted access to the Internet or certain web domains

In this mode Scala CLI will only use local artifacts cached by coursier. Any attempts to download new artifacts will fail if they are not recoverable.
This applies to everything that Scala CLI normally manages behind the scenes:
- Scala language and compiler artifacts
- JVM artifacts
- Bloop artifacts
- dependency artifacts

## How to use the offline mode

To enable offline mode pass the `--offline` flag to Scala CLI, e.g.:

```bash ignore
scala-cli run Main.scala --offline
```

It is also possible to use the `COURSIER_MODE` environment variable or `coursier.mode` java property.
```bash ignore
export COURSIER_MODE=offline
```
or
```bash ignore
scala-cli -Dcoursier.mode=offline run Main.scala 
```

## Changes in behaviour

### Scala artifacts
In offline mode Scala CLI will not perform any validation of the Scala version specified in the project.

### JVM artifacts
System JVM will be used or a fetch from local cache performed.
If a different JVM version than the system one is required, it is best to export it to the `JAVA_HOME` environment variable.
It is important to know, that currently if a version is specified with `--jvm` or `using jvm` Scala CLI will ignore the system JVM and try to fetch via coursier.

To start the Bloop server a JVM with version above 17 is required, if it can't be found compilation will fall back to using `scalac` instead.

### Bloop artifacts
If no artifacts for Bloop are available compilation falls back to using `scalac` instead.

### Dependency artifacts
Any attempt to download a dependency will fail, so it is required to have all the dependencies cached locally before compiling.

Dependencies that reside in local repositories like `~/.ivy2/local` will be resolved as usual.

## Setting up the environment

The easiest way to set up the environment is to use [Coursier](https://get-coursier.io).

Installing scala artifacts:
```bash ignore
cs install scala:3.3.0 scalac:3.3.0
```

Installing a JVM:
```bash ignore
cs java --jvm 17
```

Using the two commands above is already enough for running and compiling code using `scalac`.
For fetching code dependencies run:
```bash ignore
cs fetch com.lihaoyi::os-lib::0.9.1
```
Note that the dependency format is the same as for `--dep` and `using dep`. More information about it [here](./dependencies.md).

If you want to use Bloop, you can get it with:
```bash ignore
cs fetch io.github.alexarchambault.bleep:bloop-frontend_2.12:1.5.11-sc-2 
```
Note that Scala CLI uses a custom fork of Bloop, so simple `cs install bloop` won't work.

## Testing offline mode

To perform a test of environment setup for offline mode, it may be useful to create a clean cache directory for coursier.
To do so, run:
```bash ignore
mkdir test-coursier-cache
export COURSIER_CACHE=`pwd`/test-coursier-cache
```
And proceed with setting up the environment as described above:
```bash ignore
# Should fail with:
# [error]  Error downloading org.scala-lang:scala3-compiler_3:3.3.0
scala-cli run Main.scala --jvm 11 --offline
cs install scala:3.3.0 scalac:3.3.0

# Could fail with:
# Error while getting https://github.com/coursier/jvm-index/raw/master/index.json
# But may also pass on MacOS ('/usr/libexec/java_home -v' is tried)
# or if a JVM is cached in coursier's archive cache (this cache's location can't be overridden), you may want to clear it, see section below
scala-cli run Main.scala --jvm 11 --offline
cs java --jvm 11

# Should pass with a warning:
# [warn]  Offline mode is ON and Bloop could not be fetched from the local cache, using scalac as fallback
scala-cli run Main.scala --jvm 11 --offline
cs fetch io.github.alexarchambault.bleep:bloop-frontend_2.12:1.5.11-sc-2 

# Should pass with a warning:
# [warn]  Offline mode is ON and a JVM for Bloop could not be fetched from the local cache, using scalac as fallback
scala-cli run Main.scala --jvm 11 --offline
cs java 17

Should pass with no warnings
scala-cli run Main.scala --jvm 11 --offline
```

## Clearing coursier's caches
Citing [Coursier's docs](https://get-coursier.io/docs/cache#default-location): <br/>
On a system where only recent versions of coursier were ever run (>= 1.0.0-RC12-1, released on the 2017/10/31), the default cache location is platform-dependent:
- on Linux, `~/.cache/coursier/v1`. This also applies to Linux-based CI environments, and FreeBSD too
- on OS X, `~/Library/Caches/Coursier/v1`
- on Windows, `%LOCALAPPDATA%\Coursier\Cache\v1`, which, for user Alex, typically corresponds to `C:\Users\Alex\AppData\Local\Coursier\Cache\v1`

So clearing the cache is just a matter of removing the `v1` directory corresponding to the platform you're on.
However, Coursier does use a second archive cache, which should be located in the same place as the `v1` directory, e.g. `~/.cache/coursier/arc`,
this cache's location can't be overridden, so it may be necessary to clear it for proper testing.