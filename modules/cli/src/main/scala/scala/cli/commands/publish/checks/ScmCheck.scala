package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{GitRepo, OptionCheck, PublishSetupOptions}
import scala.cli.errors.MissingPublishOptionError

final case class ScmCheck(
  options: PublishSetupOptions,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Extra
  def fieldName     = "vcs"
  def directivePath = "publish.versionControl"

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.versionControl.nonEmpty

  def defaultValue(): Either[BuildException, OptionCheck.DefaultValue] = {
    def ghVcsOpt = GitRepo.ghRepoOrgName(workspace, logger) match {
      case Left(err) =>
        logger.debug(
          s"Error when trying to get GitHub repo from git to get default project VCS: $err, ignoring it."
        )
        None
      case Right((org, name)) =>
        logger.message("vcs:")
        logger.message(s"  using GitHub repository $org/$name")
        Some(s"github:$org/$name")
    }
    options.publishParams.vcs.orElse(ghVcsOpt).map(
      OptionCheck.DefaultValue.simple(_, Nil, Nil)
    ).toRight {
      new MissingPublishOptionError("version control", "--vcs", "publish.versionControl")
    }
  }
}
