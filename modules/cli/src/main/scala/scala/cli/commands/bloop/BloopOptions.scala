package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared._
import scala.cli.commands.tags

// format: off
@HelpMessage(BloopOptions.helpMessage, "", BloopOptions.detailedHelpMessage)
final case class BloopOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),

  @ExtraName("workingDir")
  @ExtraName("dir")
  @Tag(tags.restricted)
    workingDirectory: Option[String] = None
) extends HasGlobalOptions {
  // format: on

  def workDirOpt: Option[os.Path] =
    workingDirectory
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))
}

object BloopOptions {
  implicit lazy val parser: Parser[BloopOptions] = Parser.derive
  implicit lazy val help: Help[BloopOptions]   = Help.derive
  val helpMessage: String = "Interact with Bloop (the build server) or check its status."
  val detailedHelpMessage: String =
    s"""$helpMessage
       |
       |This sub-command allows to check the current status of Bloop.
       |If Bloop isn't currently running, it will be started.
       |
       |${HelpMessages.bloopInfo}""".stripMargin
}
