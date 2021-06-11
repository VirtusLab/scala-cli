package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.Build

@HelpMessage("Compile and test Scala code")
final case class TestOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions()
) {
  def buildOptions(scalaVersions: ScalaVersions): Build.Options =
    shared.buildOptions(scalaVersions, enableJmh = None, jmhVersion = None)
}

object TestOptions {
  implicit val parser = Parser[TestOptions]
  implicit val help = Help[TestOptions]
}
