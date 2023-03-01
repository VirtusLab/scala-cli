---
title: Export ⚡️
sidebar_position: 27
---

In case your project outgrows the cabapilities of Scala CLI (e.g support for modules) it may be beneficial
to switch to a build tool such as SBT or Mill.
The `export` sub-command allows to do that by converting a Scala CLI project into an SBT or Mill configuration.
Additionally the sub-command supports the JSON format for custom analysis of projects.

Results of running the sub-command are, by default, put in `./dest/`,
that behaviour can be modified by specifying a path with the `--output` option.

:::caution
The `export` sub-command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:
scala-cli config power true
:::

The project configuration is read both from information specified in source files
as well as options passed to the `export` sub-command.

Let's take a simple one-file project as an example:
```scala title=Hello.scala
//> using scala "3.1.3"
//> using option "-Xasync"
//> using dep "com.lihaoyi::os-lib:0.9.0"

object Hello {
  def main(args: Array[String]): Unit =
    println(os.pwd)
}
```

# Exporting to SBT:

```bash
scala-cli --power export Hello.scala --sbt
```
Note that `--sbt` is not required here since it's the default.

The result is an sbt-compliant project created in the `dest/` directory:

```
dest
├── project
│   └── build.properties
├── src
│   └── main
│       └── scala
│           └── Hello.scala
└── build.sbt
```

All the project's configuration resides now in `dest/build.sbt`:


```scala title=dest/build.sbt
scalaVersion := "3.1.3"

scalacOptions ++= Seq("-Xasync")

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.9.0" 

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.9.0" % Test

```


To configure the version of SBT used in the new project provide the `--sbtVersion` option to the `export` sub-command.

# Exporting to Mill:

```bash
scala-cli --power export Hello.scala --mill --output=dest_mill
```
Mill is not the default `export` format, so passing the `--mill` option is required.

By specifying the path with `--output` option the results are now created in `dest_mill/` directory:

```
dest_mill
├── project
│   └── src
│       └── Hello.scala
├── .mill-version
├── build.sc
├── mill
└── mill.bat
```

And all the project's configuration resides now in `dest_mill/build.sc`:

```scala title=dest_mill/build.sc
import mill._
import mill.scalalib._
object project extends ScalaModule {
  def scalaVersion = "3.1.3"
  def scalacOptions = super.scalacOptions() ++ Seq("-Xasync")
  def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"com.lihaoyi::os-lib:0.9.0"
  )

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Seq(
      ivy"com.lihaoyi::os-lib:0.9.0"
    )
  }
}

```

The script files `mill` and `mill.bat` are mill wrappers fetched from [lefou/millw repository](https://github.com/lefou/millw/tree/166bcdf5741de8569e0630e18c3b2ef7e252cd96).
To change the build tool version used override the contents of `dest_mill/.mill-version`.


# Exporting to JSON:

To export project information in a human-comprehensible format, use the `--json` flag.

```bash
scala-cli --power export Hello.scala --json --output=dest_json
```

The result is the `dest_json/export.json` file:

```json title=dest_json/export.json
{
 "scalaVersion": "3.1.3",
 "platform": "JVM",
 "scopes": {
  "main": {
   "sources": [
    "Foo.scala"
   ],
   "scalacOptions": [
    "-Xasync"
   ],
   "dependencies": [
    {
     "groupId": "com.lihaoyi",
     "artifactId": {
      "name": "os-lib",
      "fullName": "os-lib_3"
     },
     "version": "0.9.0"
    }
   ],
   "resolvers": [
    "https://repo1.maven.org/maven2",
    "ivy:file:///Users/mgajek/Library/Caches/ScalaCli/local-repo/v0.1.20-111-648755-DIRTY2ba64fdc//[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]",
    "ivy:file:/Users/mgajek/.ivy2/local/[organisation]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]"
   ]
  }
 }
}
```
