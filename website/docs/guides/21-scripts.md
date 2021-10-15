---
title: Scripts
---

# Scripts

`scala-cli` accept Scala scripts, ending in `.sc`. Unlike `.scala` files,
any kind of statement is accepted at the top-level:

```scala title=hello.sc
val message = "Hello from Scala script"
println(message)
```

Run it with:

```bash
scala-cli hello.sc
# Hello from Scala script
```

In more detail, such a script is wrapped in an `object` before being passed to
the Scala compiler, and a `main` class is added to it. `hello.sc` is passed as

```scala
object hello {
  val message = "Hello from Scala script"
  println(message)

  def main(args: Array[String]): Unit = ()
}
```
(reformatted for clarity)
The name `hello` comes straight from the file name `hello.sc`.

When a script is in a sub-directory of a directory passed to `scala-cli` , a package is inferred too:

```scala title=my-app/constants/messages.sc
def hello = "Hello from Scala scripts"
```

```scala title=my-app/main.sc
import constants.messages
println(messages.hello)
```

Run them with
```bash
scala-cli my-app --main-class main
# Hello from Scala scripts
```

Note that we pass an explicit main class. Both scripts automatically get a main class, so this
is required to disambiguate them.

### Self executable Scala Script

You can define file with shebang header to self executable. It could be also run as a normal script.

```scala title=HelloScript.sc
#!/usr/bin/env scala-cli

println("Hello world")
```

Make it executable and run it as an any other script:

```bash
chmod +x HelloScript.sc
./HelloScript.sc
# Hello world
```

### Difference with Ammonite scripts

[Ammonite](http://ammonite.io) is a popular REPL for Scala, that is also able to compile and run
`.sc` files.

`scala-cli` and Ammonite differ significantly when your code is split in multiple scripts:
- in Ammonite, a script needs to use `import $file` directives to use values defined in another script
- with `scala-cli` , all scripts passed can reference each other, without such directives

On the other hand,
- you can pass a single "entry point" script as input to Ammonite, and Ammonite finds the scripts
it depends on via the `import $file` directives
- `scala-cli` requires all scripts to be passed beforehand, either one-by-one, or by putting them in a
directory, and passing the directory to `scala-cli`