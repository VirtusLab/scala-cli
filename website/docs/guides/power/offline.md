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

The offline mode for Scala CLI was introduced to be used in two situations:
- you want to have more control over the artifacts being downloaded
- your development environment has restricted access to the Internet or certain web domains

In this mode Scala CLI will only use local artifacts cached by coursier. Any attempts to download artifacts will fail unless they're available locally in cache or there is a known fallback.
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

Finally, it's possible to enable offline mode via global config:
```bash ignore
scala-cli --power config offline true
```

## Changes in behaviour

### Scala artifacts
In offline mode Scala CLI will not perform any validation of the Scala version specified in the project, it will not be checked if such a version has been released.

### JVM artifacts
System JVM will be used or it will be fetched from local cache.
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
Note that the dependency format is the same as for `--dep` and `using dep`. More information about it [here](../introduction/dependencies.md).

If you want to use Bloop, you can get it with:
```bash ignore
cs fetch io.github.alexarchambault.bleep:bloop-frontend_2.12:1.5.11-sc-3 
```
Note that Scala CLI uses a custom fork of Bloop, so simple `cs install bloop` won't work.

### Setting up the environment manually

It is possible to copy the Scala language artifacts and dependencies to the local Coursier's cache manually.
This can be done by creating a directory structure like this:
```text
COURSIER_CACHE_PATH
└── https
    └── repo1.maven.org
        └── maven2
            └── org
                └── scala-lang
                    └── scala-compiler
                        └── 2.13.12
                            ├── scala-compiler-2.13.12-sources.jar (OPTIONAL)
                            ├── scala-compiler-2.13.12.jar
                            └── scala-compiler-2.13.12.pom
```
Path on MacOs `~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-compiler/2.13.12`

Same for a library:
```text
COURSIER_CACHE_PATH
└── https
    └── repo1.maven.org
        └── maven2
            └── com
                └── lihaoyi
                    └── os-lib_3
                        └── 0.9.1
                            ├── os-lib_3-0.9.1-sources.jar (OPTIONAL)
                            ├── os-lib_3-0.9.1.jar
                            └── os-lib_3-0.9.1.pom
```
Path on MacOS `~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_3/0.9.1`

The first segments after the `v1` directory are the address of the repository from which the artifact was downloaded.
This part can effectively be `https/repo1.maven.org/maven2` since maven central is the default repository to use.
The rest of the path is the artifact's organization (split by the '.' character) and version.

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
cs fetch io.github.alexarchambault.bleep:bloop-frontend_2.12:1.5.11-sc-3

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