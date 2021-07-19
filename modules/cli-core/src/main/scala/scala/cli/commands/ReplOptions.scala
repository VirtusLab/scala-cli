package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.build.options.BuildOptions

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
    ammonite.getOrElse(Constants.ammoniteVersion)

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts = baseOptions.javaOptions.javaOpts ++ sharedJava.allJavaOpts
      )
    )
  }
}

object ReplOptions {
  implicit val parser = Parser[ReplOptions]
  implicit val help = Help[ReplOptions]
}
