package scala.cli.launcher

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import scala.cli.commands.shared.{HelpGroup, SharedOptions}
import scala.cli.commands.{Constants, tags}

case class ScalaRunnerLauncherOptions(
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage(
    s"The default version of Scala used when processing user inputs (current default: ${Constants.defaultScalaVersion}). Can be overridden with --scala-version. "
  )
  @ValueDescription("version")
  @Hidden
  @Tag(tags.implementation)
  @Name("cliDefaultScalaVersion")
  cliUserScalaVersion: Option[String] = None,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("")
  @Hidden
  @Tag(tags.implementation)
  @Name("r")
  @Name("repo")
  @Name("repository")
  @Name("predefinedRepository")
  cliPredefinedRepository: List[String] = Nil,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage(
    "This allows to override the program name identified by Scala CLI as itself (the default is 'scala-cli')"
  )
  @Hidden
  @Tag(tags.implementation)
  progName: Option[String] = None
) {
  def toCliArgs: List[String] =
    cliUserScalaVersion.toList.flatMap(v => List("--cli-default-scala-version", v)) ++
      cliPredefinedRepository.flatMap(v => List("--repository", v)) ++
      progName.toList.flatMap(v => List("--prog-name", v))
}

object ScalaRunnerLauncherOptions {
  implicit val parser: Parser[ScalaRunnerLauncherOptions] = Parser.derive
  implicit val help: Help[ScalaRunnerLauncherOptions]     = Help.derive
}
