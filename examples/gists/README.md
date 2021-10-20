<!--
  File was generated from based on docs/cookbooks/gists.md, do not edit manually!
-->


# Sharing and testing code with Github Gists

## Running code from Gists

`scala-cli` allows running Scala code straight from Github Gists, without the need for manually downloading them first.
This is done by just passing the link to a Gist as an argument to `scala-cli`.

For example, having a given gist `https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec`:
```scala title=Messages.scala
object Messages {
  def hello = "Hello"
}
```
```scala title=run.sc
println(Messages.hello)
```
Running in terminal
```bash
scala-cli https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec
```
<!-- Expected:
Hello
-->
Will print `Hello` to the standard output.

:::note
Note that the gist doesn't necesarily have to be one file.
`scala-cli` downloads the Gists archive and unzips it, so the Gist can contain multiple files depending on each other.

`scala-cli` also caches the project sources using `coursier`'s cache.
:::

## Sharing code snippets

Together with github-cli it becomes really easy to share Scala code.

If you wants to share a code file, just run:
```sh
gh gist create file.scala
```

And to run it quickly like shown above.