---
title: Use scala-cli to run Scala Scripts
sidebar_position: 3 
---

## Scala Scripts

Scala Scripts are files containing Scala code without main method required. These codes of scala do not require build-tools configurations. To run Scala Scripts very quickly without waiting for warn-up build-tools, you can use `scala-cli`.

### Run 

You can use `scala-cli` to run Scala scripts (no further setup is required):

```scala name:HelloScript.sc
val sv = scala.util.Properties.versionNumberString

val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
println(message)
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

## Features

All the features from non-scripts are working with Scala Scripts, such as waiting for changes (watch mode), dependencies menagement, packaging, compiling and many others. 

### Package

Pass `--package` to the Scala CLI to package your script to Leighweight JAR file.

```bash
scala-cli --package HelloScript.sc
```

### Watch mode

Pass `--watch` to the Scala CLI to watch all sources for changes, and re-run them upon changes.

```bash
scala-cli --watch HelloScript.sc
```