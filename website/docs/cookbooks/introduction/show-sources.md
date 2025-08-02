---
title: Displaying Source File Paths or Names
sidebar_position: 6
---

## Source Paths and Names

A portable way to access source file paths is based on two System properties.
Multiple source files, if present, are separated by `java.io.File.pathSeparator`.

### System property "scala.sources"

For example, given this simple script:

```scala title=reportSources.sc
val sources = sys.props("scala.sources")
println(s"scriptPath: $sources")
```

When run, the script reports its own path:

```bash
scala-cli reportSources.sc
```

<!-- Expected-regex:
scriptPath: .*/reportSources.sc
-->

Likewise, for this `.scala` program:

```scala title=ReportPath.scala
object Main {
  def main(args: Array[String]): Unit =
    val path = sys.props("scala.sources")
    println(s"sourcePath: $path")
}
```

When run, the script reports its own path:

```bash
scala-cli ReportPath.scala
```

<!-- Expected-regex:
.*/ReportPath.scala
-->

And for this java program:

```java title=PathsLister.java
public class PathsLister {
  public static void main(String[] args) {
    String[] sources = System.getProperty("scala.sources").split(java.io.File.pathSeparator);
    String list = String.join(",", sources);
    System.out.printf("%s\n", list);
  }
}

```

```bash
scala-cli PathsLister.java
```

<!-- Expected-regex:
.*/PathsLister.java
-->


The same java program, if provided with multiple sources, will display them all:

```bash
scala-cli PathsLister.java reportSources.sc ReportPath.scala --main-class PathsLister
```

<!-- Expected-regex:
.*/PathsLister[.]java,.*/reportSources[.]sc,.*/ReportPath[.]scala
-->

### System property "scala.source.names"

```java title=NamesLister.java
public class NamesLister {
  public static void main(String[] args) {
    String names = System.getProperty("scala.source.names");
    String list = String.join(",", names.split(java.io.File.pathSeparator));
    System.out.printf("%s\n", list);
  }
}

```
The property "scala.source.names" shows source file base names (without path):

```bash
scala-cli NamesLister.java reportSources.sc ReportPath.scala --main-class NamesLister
```

<!-- Expected-regex:
NamesLister[.]java,reportSources[.]sc,ReportPath[.]scala
-->

