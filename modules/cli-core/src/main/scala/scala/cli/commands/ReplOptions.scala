package scala.cli.commands

import caseapp._

import scala.build.BuildOptions

@HelpMessage("Fire-up a Scala REPL")
final case class ReplOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),

  @Group("Repl")
  @HelpMessage("Set Ammonite version")
    ammonite: Option[String] = None
) {
  def ammoniteVersion: String =
    ammonite.getOrElse {
      "2.3.8-58-aa8b2ab1" // TODO Get via scala.build.internal.Constants
    }

  def buildOptions: BuildOptions =
    shared.buildOptions(jmhOptions = None, jmhVersion = None)
}

object ReplOptions {
  implicit val parser = Parser[ReplOptions]
  implicit val help = Help[ReplOptions]
}
