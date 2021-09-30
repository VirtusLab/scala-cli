---
title: Use scala-cli to run Scala Scripts
sidebar_position: 3 
---

### Run 

To run your Scala scripts without any setup enviromnet, just use `scala-cli` to run it:

```scala name:HelloScript.sc
val sv = scala.util.Properties.versionNumberString

def printMessage(): Unit =
  val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
  println(message)

printMessage()
```

```scala-cli
scala-cli run HelloScript.sc
```

<!-- Expected:
Hello from Scala *, Java *
-->


Or you can add shebang header and run as Scala Script like `bash` scripts. Thus, to run Scala Script, `PATH` must contain` scala-cli` path.

```scala name:HelloScriptSheBang.sc
#!/usr/bin/env scala-cli
val sv = scala.util.Properties.versionNumberString

def printMessage(): Unit =
  val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
  println(message)

printMessage()
```


```bash
chmod +x HelloScriptSheBang.sc
./HelloScriptSheBang.sc
# Hello from Scala 2.13.6, Java 16.0.1
```

### Debug 

ScalaCli provides debug mode using the `--watch` parameter to observe all files and re-run project if it detects a change in at least one file.

```bash
scala-cli run --watch HelloScript.sc
# Hello from Scala 2.13.6, Java 16.0.1
# Watching sources, press Ctrl+C to exit.
# Compiling project (Scala 3.0.2, JVM)
# Compiled project (Scala 3.0.2, JVM)
# Re-run using --watch mode - Hello from Scala 2.13.6, Java 16.0.1
# Watching sources, press Ctrl+C to exit.
```