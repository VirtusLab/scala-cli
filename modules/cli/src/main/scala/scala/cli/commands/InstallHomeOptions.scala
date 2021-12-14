package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Install `scala-cli` in a sub-directory of the home directory")
final case class InstallHomeOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions(),
  @Group("InstallHome")
    scalaCliBinaryPath: String,
  @Group("InstallHome")
  @Name("f")
  @HelpMessage("Overwrite `scala-cli`, if it exists")
    force: Boolean = false,
  @Hidden
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @HelpMessage("Print the update to `env` variable")
    env: Boolean = false,
  @Hidden
  @HelpMessage("Binary directory")
    binDir: Option[String] = None
) {
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
}

object InstallHomeOptions {
  implicit lazy val parser: Parser[InstallHomeOptions] = Parser.derive
  implicit lazy val help: Help[InstallHomeOptions]     = Help.derive
}
