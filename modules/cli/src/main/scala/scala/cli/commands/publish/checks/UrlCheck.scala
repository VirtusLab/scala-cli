package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{GitRepo, OptionCheck, PublishSetupOptions}
import scala.cli.errors.MissingPublishOptionError

final case class UrlCheck(
  options: PublishSetupOptions,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Extra
  def fieldName     = "url"
  def directivePath = "publish.url"

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.url.nonEmpty

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] = {
    def ghUrlOpt = GitRepo.ghRepoOrgName(workspace, logger) match {
      case Left(err) =>
        logger.debug(
          s"Error when trying to get GitHub repo from git to get default project URL: $err, ignoring it."
        )
        None
      case Right((org, name)) =>
        val url = s"https://github.com/$org/$name"
        logger.message("url:")
        logger.message(s"  using GitHub repository URL $url")
        Some(url)
    }
    options.publishParams.url
      .orElse(ghUrlOpt)
      .map(OptionCheck.DefaultValue.simple(_, Nil, Nil))
      .toRight {
        new MissingPublishOptionError("url", "--url", "publish.url")
      }
  }
}
