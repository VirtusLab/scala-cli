---
title: Scripts
sidebar_position: 19
---

# Scripts

`scala-cli` accepts Scala scripts as files that end in `.sc`.
Unlike `.scala` files, in scripts, any kind of statement is accepted at the top-level:

```scala title=hello.sc
val message = "Hello from Scala script"
println(message)
```

A script is run with the `scala-cli` command:

```bash
scala-cli hello.sc
# Hello from Scala script
```

The way this works is that a script is wrapped in an `object` before it's passed to the Scala compiler, and a `main` class is added to it.
In the previous example, when the `hello.sc` script is passed to the compiler, the altered code looks like this:

```scala
object hello {
  val message = "Hello from Scala script"
  println(message)

  def main(args: Array[String]): Unit = ()
}
```

The name `hello` comes from the file name, `hello.sc`.

When a script is in a sub-directory, a package name is also inferred:

```scala title=my-app/constants/messages.sc
def hello = "Hello from Scala scripts"
```

```scala title=my-app/main.sc
import constants.messages
println(messages.hello)
```

To specify a main class when running a script, use this command:

```bash
scala-cli my-app --main-class main
# Hello from Scala scripts
```

Both of the previous scripts (`hello.sc` and `main.sc`) automatically get a main class, so this is required to disambiguate them.

### Aguments

You may also pass arguments to your script, and they are referenced with the special `args` variable:

```scala title=p.sc
#!/usr/bin/env scala-cli

println(args(1))
```

```bash
chmod +x p.sc
./p.sc hello world
# world
```

### Self executable Scala Script

You can define a file with the “shebang” header to be self-executable. For example, given this script:

```scala title=HelloScript.sc
#!/usr/bin/env scala-cli

println("Hello world")
```

You can make it executable and run it, just like any other shell script:

```bash
chmod +x HelloScript.sc
./HelloScript.sc
# Hello world
```

### Difference with Ammonite scripts

[Ammonite](http://ammonite.io) is a popular REPL for Scala that can also compile and run `.sc` files.

`scala-cli` and Ammonite are similar, but differ significantly when your code is split in multiple scripts:
- In Ammonite, a script needs to use `import $file` directives to use values defined in another script
- With `scala-cli`, all scripts passed can reference each other without such directives

On the other hand:
- You can pass a single "entry point" script as input to Ammonite, and Ammonite finds the scripts it depends on via the `import $file` directives
- `scala-cli` requires all scripts to be passed beforehand, either one-by-one, or by putting them in a directory, and passing the directory to `scala-cli`
