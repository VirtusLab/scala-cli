package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class FixTestDefinitions
    extends ScalaCliSuite
    with TestScalaVersionArgs
    with FixBuiltInRulesTestDefinitions
    with FixScalafixRulesTestDefinitions { _: TestScalaVersion =>
  val projectFileName = "project.scala"
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
}
