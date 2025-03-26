package scala.cli.integration

class CompileTestsDefault extends CompileTestDefinitions with CompileTests3StableDefinitions
    with TestDefault {
  test(
    s"compile --cross $actualScalaVersion with ${Constants.scala213} and ${Constants.scala212}"
  ) {
    val crossVersions = Seq(actualScalaVersion, Constants.scala213, Constants.scala212)
    simpleInputs
      .add(os.rel / "project.scala" -> s"//> using scala ${crossVersions.mkString(" ")}")
      .fromRoot { root =>
        os.proc(TestUtil.cli, "compile", ".", "--cross", "--power", extraOptions).call(cwd = root)
      }
  }
}
