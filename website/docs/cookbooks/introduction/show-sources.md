---
title: Accessing Source File Paths
sidebar_position: 10
---

## Source Names and Paths

System properties provide source file paths, separated by `java.io.File.pathSeparator`.
- `scala.source.names` filenames without a path.
- `scala.sources`  full paths to source files

### System property `scala.source.names`

This simple script reports the script file name:

```scala title=reportNames.sc
val name = sys.props("scala.source.names")
println(s"scriptName: $name")
```

```bash
scala-cli reportNames.sc
# scriptName: reportNames.sc
```

<!-- Expected-regex:
scriptName: reportNames.sc
-->

Likewise, for this `.scala` program:

```scala title=SourceNames.scala
object Main {
  def main(args: Array[String]): Unit =
    val names = sys.props("scala.source.names")
    println(names)
}
```

```bash
scala-cli SourceNames.scala
# SourceNames.scala
```

<!-- Expected-regex:
SourceNames[.]scala
-->

This java program prints a comma-separated list of source names:

```java title=NamesLister.java
public class NamesLister {
  public static void main(String[] args) {
    String sourceNames = System.getProperty("scala.source.names");
    String[] names = sourceNames.split(java.io.File.pathSeparator);
    String list = String.join(",", names);
    System.out.printf("%s\n", list);
  }
}

```

```bash
scala-cli NamesLister.java
# NamesLister.java
```

<!-- Expected-regex:
NamesLister.java
-->


If multiple sources are provided, all are displayed:

```bash
scala-cli NamesLister.java reportNames.sc SourceNames.scala --main-class NamesLister
# NamesLister.java,reportNames.sc,SourceNames.scala
```

<!-- Expected-regex:
NamesLister[.]java,reportNames[.]sc,SourceNames[.]scala
-->

### System property `scala.sources`

```java title=PathsLister.java
public class PathsLister {
  public static void main(String[] args) {
    String paths = System.getProperty("scala.sources");
    String list = String.join(",", paths.split(java.io.File.pathSeparator));
    System.out.printf("%s\n", list);
  }
}
```
The property "scala.sources" displays full source file paths:

```bash
scala-cli PathsLister.java reportNames.sc SourceNames.scala --main-class PathsLister
# /tmp/workdir/PathsLister.java,/tmp/workdir/reportNames.sc,/tmp/workdir/SourceNames.scala
```

<!-- Expected-regex:
.*/PathsLister[.]java,.*/reportNames[.]sc,.*/SourceNames[.]scala
-->

### Other examples

When multiple `.md` files with plain `scala` snippets are being run, names and paths can refer to '.md' files:

````markdown title=MainA.md
# Main class A
```scala
println(s"A: ${sys.props("scala.source.names")}")
```
````


````markdown title=MainB.md
# Main class 2
```scala
println(s"2: ${sys.props("scala.source.names")}")
```
````

When multiple such sources are passed as inputs, the main class has to be passed explicitly with the `--main-class`
option.

```bash
scala-cli --power MainA.md MainB.md --main-class MainA_md
# A: MainA.md:MainB.md
```

```text
1: MainA.md:MainB.md
```
<!-- Expected-regex:
A: MainA[.]md:MainB[.]md
-->
