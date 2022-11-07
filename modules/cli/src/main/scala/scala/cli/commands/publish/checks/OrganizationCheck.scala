package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{GitRepo, OptionCheck, PublishSetupOptions}
import scala.cli.errors.MissingPublishOptionError

final case class OrganizationCheck(
  options: PublishSetupOptions,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Core
  def fieldName     = "organization"
  def directivePath = "publish.organization"

  def check(options: BPublishOptions): Boolean =
    options.organization.nonEmpty

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] = {

    def viaGitHubRemoteOpt = GitRepo.ghRepoOrgName(workspace, logger) match {
      case Left(err) =>
        logger.debug(
          s"Error when trying to get GitHub repo from git to compute default organization: $err, ignoring it."
        )
        None
      case Right((org, _)) =>
        val publishOrg = s"io.github.$org"
        logger.message("organization:")
        logger.message(s"  computed $publishOrg from GitHub account $org")
        Some(publishOrg)
    }

    val orgOpt = options.publishParams.organization
      .orElse(viaGitHubRemoteOpt)

    orgOpt.map(OptionCheck.DefaultValue.simple(_, Nil, Nil)).toRight {
      new MissingPublishOptionError(
        "organization",
        "--organization",
        "publish.organization"
      )
    }
  }
}
