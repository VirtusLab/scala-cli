package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.BuildOptions

@HelpMessage("Compile and test Scala code")
final case class TestOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions()
) {
  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)
}

object TestOptions {
  implicit val parser = Parser[TestOptions]
  implicit val help = Help[TestOptions]
}
