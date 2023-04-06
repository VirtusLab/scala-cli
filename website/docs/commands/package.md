---
title: Package ⚡️
sidebar_position: 27
---

:::caution
The Package command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

The `package` command can package your Scala code in various formats, such as:
- [lightweight launcher JARs](#default-package-format)
- [standard library JARs](#library-jars)
- so called ["assemblies" or "fat JARs"](#assemblies)
- [docker container](#docker-container)
- [JavaScript files](#scalajs) for Scala.js code
- [GraalVM native image executables](#native-image)
- [native executables](#scala-native) for Scala Native code
- [OS-specific formats](#os-specific-packages), such as deb or rpm (Linux), pkg (macOS), or MSI (Windows)

## Default package format

The default package format writes a *lightweight launcher JAR*, like the "bootstrap" JAR files [generated by coursier](https://get-coursier.io/docs/cli-bootstrap#bootstraps).
These JARs tend to have a small size (mostly containing only the byte code from your own sources),
can be generated fast,
and download their dependencies upon first launch via [coursier](https://get-coursier.io).

Such JARs can be copied to other machines, and will run fine there.
Their only requirement is that the `java` command needs to be available in the `PATH`:

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash
scala-cli --power package Hello.scala -o hello
./hello
```

```text
Hello
```

</ChainedSnippets>

<!-- Expected
Hello
-->

## Library JARs

*Library JARs* are suitable if you plan to put the resulting JAR in a class path, rather than running it as is.
These follow the same format as the JARs of libraries published to Maven Central:

```scala title=MyLibrary.scala
package mylib

class MyLibrary {
  def message = "Hello"
}
```


<ChainedSnippets>

```bash
scala-cli --power package MyLibrary.scala -o my-library.jar --library
javap -cp my-library.jar mylib.MyLibrary
```

```text
Compiled from "MyLibrary.scala"
public class mylib.MyLibrary {
  public java.lang.String message();
  public mylib.MyLibrary();
}
```

</ChainedSnippets>

<!-- Expected:
MyLibrary.scala
public class mylib.MyLibrary
public java.lang.String message();
public mylib.MyLibrary();
-->

## Assemblies

*Assemblies* blend your dependencies and your sources' byte code together in a single JAR file.
As a result, assemblies can be run as is, just like [bootstraps](#default-package-format), but don't need to download
anything upon first launch.
Because of that, assemblies also tend to be bigger, and somewhat slower to generate:

<!-- clear -->

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash
scala-cli --power package Hello.scala -o hello --assembly
./hello
```

```text
Hello
```

</ChainedSnippets>

<!-- Expected:
Hello
-->

## Docker container

Scala CLI can create an executable application and package it into a docker image.

For example, here’s an application that will be executed in a docker container:

```scala title=HelloDocker.scala
object HelloDocker extends App {
  println("Hello from Docker")
}
```

Passing `--docker` to the `package` sub-command generates a docker image.
The docker image name parameter `--docker-image-repository` is mandatory.

The following command generates a `hello-docker` image with the `latest` tag:

```bash
scala-cli --power package --docker HelloDocker.scala --docker-image-repository hello-docker
```

<!-- Expected:
Started building docker image with your application
docker run hello-docker:latest
-->

<ChainedSnippets>

```bash
docker run hello-docker
```

```text
Hello from Docker
```

</ChainedSnippets>

<!-- Expected:
Hello from Docker
-->

You can also create Docker images for Scala.js and Scala Native applications.
The following command shows how to create a Docker image (`--docker`) for a Scala.js (`--js`) application:

```bash
scala-cli --power package --js --docker HelloDocker.scala --docker-image-repository hello-docker
```
<!-- Expected:
Started building docker image with your application
docker run hello-docker:latest
-->

Packaging Scala Native applications to a Docker image is only supported on Linux.

The following command shows how to do that:

```bash ignore
scala-cli --power package --native --docker HelloDocker.scala --docker-image-repository hello-docker
```

### Building Docker container from base image

`--docker-from` lets you specify your base docker image.

The following command generate a `hello-docker` image using base image `openjdk:11`

```bash ignore
scala-cli --power package --docker HelloDocker.scala --docker-from openjdk:11 --docker-image-repository hello-docker
```

## Scala.js

Packaging Scala.js applications results in a `.js` file, which can be run with `node`:

<!-- TODO: add something js specific -->

```scala title=HelloJs.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash
scala-cli --power package --js HelloJs.scala -o hello.js
node hello.js
```

```text
Hello
```

</ChainedSnippets>

<!-- Expected:
Hello
-->


Note that Scala CLI doesn't offer the ability to link the resulting JavaScript with linkers, such as Webpack (yet).

## Native image

[GraalVM native image](https://www.graalvm.org/22.0/reference-manual/native-image/)
makes it possible to build native executables out of JVM applications. It can
be used from Scala CLI to build native executables for Scala applications.

<!-- clear -->

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash
scala-cli --power package Hello.scala -o hello --native-image
./hello
```

```text
Hello
```

</ChainedSnippets>

<!-- Expected:
Hello
-->

Note that Scala CLI automatically downloads and unpacks a GraalVM distribution
using the [JVM management capabilities of coursier](https://get-coursier.io/docs/cli-java).

Several options can be passed to adjust the GraalVM version used by Scala CLI:
- `--graalvm-jvm-id` accepts a JVM identifier, such as `graalvm-java17:22.0.0` or `graalvm-java17:21` (short versions accepted).
- `--graalvm-java-version` makes it possible to specify only a target Java version, such as `11` or `17` (note that only specific Java versions may be supported by the default GraalVM version that Scala CLI picks)
- `--graalvm-version` makes it possible to specify only a GraalVM version, such as `22.0.0` or `21` (short versions accepted)
- `--graalvm-args` makes it possible to pass args to GraalVM version

## Scala Native

Packaging a Scala Native application results in a native executable:

<!-- clear -->

<!-- TODO: add something native specific -->

```scala title=HelloNative.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash
scala-cli --power package --native HelloNative.scala -S 2.13.6 -o hello
file hello
```

```text
hello: Mach-O 64-bit executable x86_64
```

```bash
./hello
```

```text
Hello
```

</ChainedSnippets>

<!-- Expected:
Hello
-->

## OS-specific packages

Scala CLI also lets you package Scala code as OS-specific packages.
This feature is somewhat experimental, and supports the following formats, provided they're compatible with the operating system you're running Scala CLI on:

- [DEB](#debian) (Linux)
- [RPM](#redhat) (Linux)
- [PKG](#macos-pkg) (macOS)
- [MSI](#windows) (Windows)

```scala Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

<ChainedSnippets>

```bash ignore
scala-cli --power package --deb Hello.scala -o hello.deb
file hello
```

```text
hello: Mach-O 64-bit executable x86_64
```

```bash ignore
./hello
```

```text
Hello
```

</ChainedSnippets>

### Debian

DEB is the package format for the Debian Linux distribution.
To build a Debian package, you will need to have [`dpkg-deb`](http://manpages.ubuntu.com/manpages/trusty/pl/man1/dpkg-deb.1.html) installed.

Example:

```bash ignore
scala-cli --power package --deb --output 'path.deb' Hello.scala
```

#### Mandatory arguments
* version
* maintainer
* description
* output-path

#### Optional arguments
* force
* launcher-app
* debian-conflicts
* debian-dependencies
* architecture

### RedHat

RPM is the software package format for RedHat distributions.
To build a RedHat Package, you will need to have [`rpmbuild`](https://linux.die.net/man/8/rpmbuild) installed.

Example:

```bash ignore
scala-cli --power package --rpm --output 'path.rpm' Hello.scala
```

#### Mandatory arguments
* version
* description
* license
* output-path

#### Optional arguments
* force
* launcher-app
* release
* rpm-architecture

### macOS (PKG)

PKG is a software package format for macOS.
To build a PKG you will need to have [`pkgbuild`](https://www.unix.com/man-page/osx/1/pkgbuild/) installed.

Example:

```bash ignore
`scala-cli --power package --pkg --output 'path.pkg` Hello.scala
```

#### Mandatory arguments
* version
* identifier
* output-path

#### Optional arguments
* force
* launcher-app

### Windows

MSI is a software package format for Windows.
To build an MSI installer, you will need to have [`WIX Toolset`](https://wixtoolset.org/) installed.

Example:

```cmd
scala-cli --power package --msi --output path.msi Hello.scala
```

#### Mandatory arguments
* version
* maintainer
* licence-path
* product-name
* output-path

#### Optional arguments
* force
* launcher-app
* exit-dialog
* logo-path

## Using directives

Instead of passing the `package` options directly from bash, it is possible to pass some of them with [using directives](/docs/guides/using-directives).

### packaging.packageType

This using directive makes it possible to define the type of the package generated by the `package` command. For example:

```scala compile power
//> using packaging.packageType "assembly"
```

Available types: `assembly`, `raw-assembly`, `bootstrap`, `library`, `source`, `doc`, `spark`, `js`, `native`, `docker`, `graalvm`, `deb`, `dmg`, `pkg`, `rpm`, `msi`.

### packaging.output

This using directive makes it possible to define the destination path of the package generated by the `package` command. For example:

```scala compile power
//> using packaging.output "foo"
```

The using directive above makes it possible to create a package named `foo` inside the current directory.

### packaging.graalvmArgs

This using directive makes it possible to pass args to GraalVM:

```scala compile power
//> using packaging.graalvmArgs "--no-fallback", "--enable-url-protocols=http,https"
```

### Docker

#### packaging.dockerFrom

The using directive allows you to define the base Docker image that is used to run your application.

```scala compile power
//> using packaging.dockerFrom "openjdk:11"
```

#### packaging.dockerFrom

The using directive allows you to define the generated Docker image tag.

```scala compile power
//> using packaging.dockerImageTag "1.0.0"
```

#### packaging.dockerImageRegistry

The using directive allows you to define the image registry.

```scala compile power
//> using packaging.dockerImageRegistry "virtuslab"
```

#### packaging.dockerImageRegistry

The using directive allows you to define the image repository.

```scala compile power
//> using packaging.dockerImageRepository "scala-cli"
```
