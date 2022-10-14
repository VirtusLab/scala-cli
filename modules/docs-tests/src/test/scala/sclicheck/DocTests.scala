package sclicheck

class DocTests extends munit.FunSuite {

  val dirs = Seq(
    "cookbook" -> os.pwd / "website" / "docs" / "cookbooks",
    "command"  -> os.pwd / "website" / "docs" / "commands"
  )

  val options = Options(
    scalaCliCommand = Seq(TestUtil.scalaCliPath),
    stopAtFailure = false,
    step = false
  )

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
