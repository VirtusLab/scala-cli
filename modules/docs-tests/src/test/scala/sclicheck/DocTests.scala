package sclicheck

class DocTests extends munit.FunSuite {
  val docsRootPath: os.Path = os.pwd / "website" / "docs"
  val dirs: Seq[(String, os.Path)] = Seq(
    "cookbook" -> docsRootPath / "cookbooks",
    "command"  -> docsRootPath / "commands",
    "guide"    -> docsRootPath / "guides"
  )

  val options: Options = Options(scalaCliCommand = Seq(TestUtil.scalaCliPath))

  private def containsCheck(f: os.Path): Boolean =
    os.read.lines(f)
      .exists(line => line.startsWith("```md") || line.startsWith("```bash"))

  for {
    (tpe, dir) <- dirs
    inputs = os.walk(dir)
      .filter(_.last.endsWith(".md"))
      .filter(os.isFile(_))
      .filter(containsCheck)
      .map(_.relativeTo(dir))
      .sortBy(_.toString)
    md <- inputs
  }
    test(s"$tpe ${md.toString.stripSuffix(".md")}") {
      checkFile(dir / md, options)
    }

}
