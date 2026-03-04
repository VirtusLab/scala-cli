package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class FixTestDefinitions
    extends ScalaCliSuite
    with TestScalaVersionArgs
    with FixBuiltInRulesTestDefinitions
    with FixScalafixRulesTestDefinitions { this: TestScalaVersion =>
  val projectFileName           = "project.scala"
  val extraOptions: Seq[String] =
    scalaVersionArgs ++ TestUtil.extraOptions ++ Seq("--suppress-experimental-feature-warning")
  def enableRulesOptions(
    enableScalafix: Boolean = true,
    enableBuiltIn: Boolean = true
  ): Seq[String] =
    Seq(
      s"--enable-scalafix=${enableScalafix.toString}",
      s"--enable-built-in-rules=${enableBuiltIn.toString}"
    )

  test("built-in + scalafix rules") {
    val mainFileName         = "Main.scala"
    val unusedValName        = "unused"
    val directive1           = "//> using dep com.lihaoyi::os-lib:0.11.3"
    val directive2           = "//> using dep com.lihaoyi::pprint:0.9.0"
    val mergedDirective1And2 =
      "using dependency com.lihaoyi::os-lib:0.11.3 com.lihaoyi::pprint:0.9.0"
    val directive3 =
      if (actualScalaVersion.startsWith("2")) "//> using options -Xlint:unused"
      else "//> using options -Wunused:all"
    TestInputs(
      os.rel / "Foo.scala" ->
        s"""$directive1
           |object Foo {
           |  def hello: String = "hello"
           |}
           |""".stripMargin,
      os.rel / "Bar.scala" ->
        s"""$directive2
           |object Bar {
           |  def world: String = "world"
           |}
           |""".stripMargin,
      os.rel / mainFileName ->
        s"""$directive3
           |object Main {
           |  def main(args: Array[String]): Unit = {
           |    val unused = "unused"
           |    pprint.pprintln(Foo.hello + Bar.world)
           |    pprint.pprintln(os.pwd)
           |  }
           |}
           |""".stripMargin,
      os.rel / scalafixConfFileName ->
        """rules = [
          |  RemoveUnused
          |]
          |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "fix", ".", extraOptions, "--power").call(cwd = root)
      val projectFileContents = os.read(root / projectFileName)
      expect(projectFileContents.contains(mergedDirective1And2))
      expect(projectFileContents.contains(directive3))
      val mainFileContents = os.read(root / mainFileName)
      expect(!mainFileContents.contains(unusedValName))
      os.proc(TestUtil.cli, "compile", ".", extraOptions).call(cwd = root)
    }
  }

  def filterDebugOutputs(output: String): String =
    output
      .linesIterator
      .filterNot(_.trim().contains("repo dir"))
      .filterNot(_.trim().contains("local repo"))
      .filterNot(_.trim().contains("archive url"))
      .mkString(System.lineSeparator())
}
