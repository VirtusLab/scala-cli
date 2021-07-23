---
title: Input format
sidebar_position: 3
---

The `scala-cli` CLI commands accept input in a number of ways, most notably:
- as `.scala` files
- as one or several directories, containing Scala sources
- as URLs, pointing to Scala sources
- by piping or process substitution

Note that it accepts two kinds of Scala sources:
- standard `.scala` files
- scripts, ending in `.sc`, accepting any kind of top-level statement

Java sources are also accepted.

Lastly, note that all these input formats can used alongside each other.

## Scala files

This is the simplest input format. Just write a `.scala` file, and pass it to
`scala-cli` to run it:

- `Hello.scala`
```scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello from Scala")
}
```

Run it with
```text
$ scala-cli Hello.scala
Hello from Scala
```

You can also split your code in multiple files, and pass all of them to `scala-cli` :

- `Messages.scala`
```scala
object Messages {
  def hello = "Hello from Scala"
}
```

- `Hello.scala`
```scala
object Hello {
  def main(args: Array[String]): Unit =
    println(Messages.hello)
}
```

Run them with
```text
$ scala-cli Hello.scala Messages.scala
Hello from Scala
```

Passing many files this way can be cumbersome. Directories can help.

## Directories

`scala-cli` accepts whole directories as input. This is convenient when you have many
`.scala` files, and passing them all one-by-one on the command line isn't practical:

- `my-app/Messages.scala`
```scala
object Messages {
  def hello = "Hello from Scala"
}
```

- `my-app/Hello.scala`
```scala
object Hello {
  def main(args: Array[String]): Unit =
    println(Messages.hello)
}
```

Run them with
```text
$ scala-cli my-app
Hello from Scala
```

## URLs

`scala-cli` accepts input via URLs pointing at `.scala` files.
It'll download and cache their content, and run them.

```text
$ scala-cli https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala
Hello from Scala GitHub Gist
```

## GitHub Gist

TODO

## Piping

You can just pipe Scala code to `scala-cli` for execution:
```text
$ echo 'println("Hello")' | scala-cli -
Hello
```

## Process substitution

`scala-cli` accepts input via shell process substitution:
```text
$ scala-cli <(echo 'println("Hello")')
Hello
```

## Scripts

`scala-cli` accept Scala scripts, ending in `.sc`. Unlike `.scala` files,
any kind of statement is accepted at the top-level:

- `hello.sc`
```scala
val message = "Hello from Scala script"
println(message)
```

Run it with
```text
$ scala-cli hello.sc
Hello from Scala script
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
(reformated for clarity)
The name `hello` comes straight from the file name `hello.sc`.

When a script is in a sub-directory of a directory passed to `scala-cli` , a package is inferred too:

- `my-app/constants/messages.sc`
```scala
def hello = "Hello from Scala scripts"
```

- `my-app/main.sc`
```
import constants.messages
println(messages.hello)
```

Run them with
```text
$ scala-cli my-app --main-class main
Hello from Scala scripts
```

Note that we pass an explicit main class. Both scripts automatically get a main class, so this
is required to disambiguate them.

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