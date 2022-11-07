package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions}

final case class NameCheck(
  options: PublishSetupOptions,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Core
  def fieldName     = "name"
  def directivePath = "publish.name"

  def check(options: BPublishOptions): Boolean =
    options.name.nonEmpty || options.moduleName.nonEmpty

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] = {
    def fromWorkspaceDirName = {
      val n = workspace.last
      logger.message("name:")
      logger.message(s"  using workspace directory name $n")
      n
    }
    val name = options.publishParams.name.getOrElse(fromWorkspaceDirName)
    Right(OptionCheck.DefaultValue.simple(name, Nil, Nil))
  }
}
