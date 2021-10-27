---
title: Use scala-cli to run Scala Scripts
sidebar_position: 3
---

## Scala Scripts

Scala Scripts are files containing Scala code without main method required. These codes of Scala do not require build-tools configurations. To run Scala Scripts very quickly without waiting for warn-up build-tools, you can use `scala-cli`.

### Run

You can use `scala-cli` to run Scala scripts (no further setup is required):

```scala title=HelloScript.sc
val sv = scala.util.Properties.versionNumberString

val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
println(message)
```

```bash
scala-cli run HelloScript.sc
```

<!-- Expected-regex:
Hello from Scala .*, Java .*
-->


Alternatively, add a "shebang" header to your script, make your script executable, and execute it directly. `scala-cli` needs to be installed for this to work.

```scala title=HelloScriptSheBang.sc
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

<!-- Expected-regex:
Hello from Scala .*, Java .*
-->

It is also possible to pass command-line arguments to the script

```scala title=ScriptArguments.sc
#!/usr/bin/env scala-cli
println(args(1))
```

```bash
chmod +x ScriptArguments.sc
./ScriptArguments.sc foo bar
# bar
```

<!-- Expected-regex:
bar
-->


## Features

All the features from non-scripts work for Scala scripts too, such as waiting for changes (watch mode), dependencies menagement, packaging, compiling and many others.

### Package

Run `package` to the Scala CLI to package your script to a lightweight executable JAR file.

```bash
scala-cli package HelloScript.sc
./HelloScript
```

<!-- Expected-regex:
Hello from Scala .*, Java .*
-->

### Watch mode

Pass `--watch` to the Scala CLI to watch all sources for changes, and re-run them upon changes.

```bash ignore
scala-cli --watch HelloScript.sc
```
