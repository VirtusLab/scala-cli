package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.build.options.BuildOptions

// format: off
@HelpMessage("Fire-up a Scala REPL")
final case class ReplOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),

  @Group("Repl")
  @HelpMessage("Use Ammonite rather than the default Scala REPL")
  @Name("A")
  @Name("amm")
    ammonite: Option[Boolean] = None,

  @Group("Repl")
  @HelpMessage("Set Ammonite version")
  @Name("ammoniteVer")
    ammoniteVersion: Option[String] = None,

  @Group("Repl")
  @Name("a")
  @Hidden
    ammoniteArg: List[String] = Nil,

  @Group("Repl")
  @Hidden
  @HelpMessage("Don't actually run the REPL, only fetch it")
    replDryRun: Boolean = false
) {
  // format: on
  private def ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty)
  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts = baseOptions.javaOptions.javaOpts ++ sharedJava.allJavaOpts
      ),
      replOptions = baseOptions.replOptions.copy(
        useAmmoniteOpt = ammonite,
        ammoniteVersionOpt = ammoniteVersionOpt,
        ammoniteArgs = ammoniteArg
      ),
      internalDependencies = baseOptions.internalDependencies.copy(
        addRunnerDependencyOpt = baseOptions.internalDependencies.addRunnerDependencyOpt
          .orElse(Some(false))
      )
    )
  }
}

object ReplOptions {
  implicit val parser = Parser[ReplOptions]
  implicit val help   = Help[ReplOptions]
}
