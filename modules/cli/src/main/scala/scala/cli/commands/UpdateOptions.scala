package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Update scala-cli - it works only for installation script")
final case class UpdateOptions(
  @Group("Update")
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @Group("Update")
  @HelpMessage("Binary directory")
    binDir: Option[String] = None,
  @Name("f")
  @HelpMessage("Update scala-cli if is outdated")
    force: Boolean = false,
  @Hidden
    isInternalRun: Boolean = false
) {
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
  lazy val installDirPath =
    binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / binaryName)
}

object UpdateOptions {
  implicit lazy val parser: Parser[UpdateOptions] = Parser.derive
  implicit lazy val help: Help[UpdateOptions]     = Help.derive
}
