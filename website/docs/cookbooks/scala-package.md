---
title: Packaging Scala applications as executable files
sidebar_position: 14
---

Scala CLI lets you package your application into a lightweight JAR file that can be easily run.
The JAR file only contains the byte code that’s generated from your source code files, and automatically downloads its dependencies on its first run.

As an example, the following snippet contains a short application to detect the OS:
```scala title=DetectOsApp.scala
object DetectOsApp extends App  {
    def getOperatingSystem(): String = {
        val os: String = System.getProperty("os.name")
        os
    }
    println(s"os: ${getOperatingSystem()}")
}
```

### Default format (lightweight launcher)

By default, the `package` sub-command generates a lightweight JAR that contains only your bytecode. This is how you create a lightweight JAR named `DetectOsApp.jar`:

```bash
scala-cli --power package DetectOsApp.scala
```

<!-- Expected-regex:
Wrote .*DetectOsApp, run it with
  .*\/DetectOsApp
-->

Lightweight JARs require the `java` command to be available, and access to the internet, if dependencies need to be downloaded. This is how you run it on macOS:

```bash
# Run DetectOsApp on macOS
./DetectOsApp
# os: Mac OS X
```

The lightweight JAR that was just built on macOS can also run on Linux:

```bash
# Run DetectOsApp on Linux
./DetectOsApp
# os: Linux
```

Scala CLI supports building Lightweight JARs in the macOS, Linux, and Windows environments.
JARs built on macOS and Linux are portable between these two operating systems.
Lightweight JARs built on Windows can only be run on Windows.


### Assemblies
Passing `--assembly` to the `package` sub-command generates so-called "assemblies," or "fat JARs":

```bash
scala-cli --power package --assembly DetectOsApp.scala
```

Assemblies also require the `java` command to be available in the `PATH`. But in this case, all of the dependencies that are needed are packaged into the assembly, so nothing gets downloaded upon the first run, and no internet access is required.

```bash
# Run DetectOsApp on macOS
./DetectOsApp
# os: Mac OS X
```
