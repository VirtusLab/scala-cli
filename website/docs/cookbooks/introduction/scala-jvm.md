---
title: Changing Java versions
sidebar_position: 3
---

You can use Scala CLI to test your code compatibility with various versions of `java`, with a key point being that manual installation of a JDK/SDK is not required(!).
Scala CLI automatically downloads the Java version you specify.

As an example, the following snippet uses the new method `Files.writeString` from Java 11:

```scala title=Main.scala
import java.nio.file.Files
import java.nio.file.Paths

object Main extends App {
  val dest = Files.createTempDirectory("scala-cli-demo").resolve("hello.txt")
  val filePath = Files.writeString(dest, "Hello from ScalaCli")
  val fileContent: String = Files.readString(filePath)
  println(fileContent)
}
```

To use Java 11 to run this application, pass the following `--jvm` option to the Scala CLI command:

```bash ignore
scala-cli --jvm temurin:11 Main.scala
```

<!-- ignored Expected:
Hello from ScalaCli
-->

To attempt to compile the application with Java 8, change the value of the `--jvm` parameter:
```bash ignore fail
scala-cli --jvm 8 Main.scala
# In this case, it raises an error because the `Files.writeString` and `Files.readString` methods are not available in java 8
#
# [error] ./Main.scala:6:18
# [error] value writeString is not a member of object java.nio.file.Files
# [error]   val filePath = Files.writeString(dest, "Hello from ScalaCli")
# [error]                  ^^^^^^^^^^^^^^^^^
# [error] ./Main.scala:7:29
# [error] value readString is not a member of object java.nio.file.Files
# [error]   val fileContent: String = Files.readString(filePath)
# [error]                             ^^^^^^^^^^^^^^^^
```

<!-- ignored Expected:
java.lang.NoSuchMethodError
java.nio.file.Files.writeString
-->
