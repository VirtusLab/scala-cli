---
title: Use scala-cli to run Scala Scripts
sidebar_position: 3 
---

### Run 

You can use `scala-cli` to run Scala scripts (no further setup is required):

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


Alternatively, add a shebang header to your script, make your script executable, and execute it directly. `scala-cli` needs to be installed for this to work.

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

### Watch mode

Pass `--watch` to the Scala CLI to watch all sources for changes, and re-run them upon changes.

```bash
scala-cli --watch HelloScript.sc
```