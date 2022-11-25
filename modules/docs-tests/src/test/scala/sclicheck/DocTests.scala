package sclicheck

class DocTests extends munit.FunSuite {
  val docsRootPath: os.Path = os.pwd / "website" / "docs"
  val dirs: Seq[(String, os.Path)] = Seq(
    "cookbook" -> docsRootPath / "cookbooks",
    "command"  -> docsRootPath / "commands",
    "guide"    -> docsRootPath / "guides"
  )

  val options: Options = Options(scalaCliCommand = Seq(TestUtil.scalaCliPath))

  private def lineContainsAnyChecks(l: String): Boolean =
    l.startsWith("```md") || l.startsWith("```bash") ||
    l.startsWith("```scala compile") || l.startsWith("```scala fail") ||
    l.startsWith("````markdown compile") || l.startsWith("````markdown fail") ||
    l.startsWith("```java compile") || l.startsWith("````java fail")
  private def fileContainsAnyChecks(f: os.Path): Boolean =
    os.read.lines(f).exists(lineContainsAnyChecks)

  for {
    (tpe, dir) <- dirs
    inputs = os.walk(dir)
      .filter(_.last.endsWith(".md"))
      .filter(os.isFile(_))
      .filter(fileContainsAnyChecks)
      .map(_.relativeTo(dir))
      .sortBy(_.toString)
    md <- inputs
  }
    test(s"$tpe ${md.toString.stripSuffix(".md")}") {
      checkFile(dir / md, options)
    }

}
