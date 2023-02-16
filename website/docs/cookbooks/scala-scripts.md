---
title: Using scala-cli to run Scala Scripts
sidebar_position: 3
---

## Scala Scripts

Scala scripts are files that contain Scala code without a main method.
These source code files don't require build-tool configurations.
To run Scala scripts very quickly without waiting the need for build tools, use `scala-cli`.

### Run

For example, given this simple script:

```scala title=HelloScript.sc
val sv = scala.util.Properties.versionNumberString

val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
println(message)
```

You can run it directly with `scala-cli` — there's no need for a build tool or additional configuration:

```bash
scala-cli run HelloScript.sc
```

<!-- Expected-regex:
Hello from Scala .*, Java .*
-->

Alternatively, you can add a "shebang" header to your script, make it executable, and execute it directly with `scala-cli`. For example, given this script with a header that invokes `scala-cli`:

```scala title=HelloScriptSheBang.sc
#!/usr/bin/env -S scala-cli shebang

val sv = scala.util.Properties.versionNumberString

def printMessage(): Unit =
  val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
  println(message)

printMessage()
```

You can make it executable and then run it like this:

```bash
chmod +x HelloScriptSheBang.sc
./HelloScriptSheBang.sc
# Hello from Scala 2.13.6, Java 16.0.1
```

<!-- Expected-regex:
Hello from Scala .*, Java .*
-->

You can also pass command line arguments to Scala scripts:

```scala title=ScriptArguments.sc
#!/usr/bin/env -S scala-cli shebang
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

As shown, command line arguments are accessed through the special `args` variable.


## Features

All of the features shown for non-scripts work for Scala scripts as well, such as waiting for changes (watch mode), dependency menagement, packaging, compiling, etc.

### Package

For example, run the `package` sub-command to package your script as a lightweight executable JAR file:

```bash
scala-cli --power package HelloScript.sc
./HelloScript
```

<!-- Expected-regex:
Hello from Scala .*, Java .*
-->

### Watch mode

As another example, pass `--watch` to `scala-cli` to watch all source files for changes, and then re-run them when there is a change:

```bash ignore
scala-cli --watch HelloScript.sc
```
