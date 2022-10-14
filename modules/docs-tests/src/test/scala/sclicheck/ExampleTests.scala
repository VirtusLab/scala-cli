package sclicheck

class ExampleTests extends munit.FunSuite {

  val examplesDir = Option(System.getenv("SCALA_CLI_EXAMPLES")).map(os.Path(_, os.pwd)).getOrElse {
    sys.error("SCALA_CLI_EXAMPLES not set")
  }

  val exampleDirs = os.list(examplesDir)
    .filter(!_.last.startsWith("."))
    .filter(os.isDir(_))

  for (dir <- exampleDirs)
    test(dir.last) {
      val args =
        if os.exists(dir / ".opts") then os.read(dir / ".opts").split("\\s+").toSeq
        else Nil
      os.proc(TestUtil.scalaCliPath, args, "--jvm", "temurin:17", ".")
        .call(cwd = dir, stdin = os.Inherit, stdout = os.Inherit)
    }

}
