package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{GitRepo, OptionCheck, PublishSetupOptions}
import scala.cli.errors.MissingPublishOptionError

final case class ComputeVersionCheck(
  options: PublishSetupOptions,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind      = OptionCheck.Kind.Core
  def fieldName = "computeVersion"
  def directivePath =
    "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".computeVersion"

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.version.nonEmpty ||
    pubOpt.retained(options.publishParams.setupCi).computeVersion.nonEmpty

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] = {
    def fromGitOpt =
      if (GitRepo.gitRepoOpt(workspace).isDefined) {
        logger.message("computeVersion:")
        logger.message("  assuming versions are computed from git tags")
        Some("git:tag")
      }
      else
        None
    val cv = options.publishParams.computeVersion
      .orElse(fromGitOpt)
    cv.map(OptionCheck.DefaultValue.simple(_, Nil, Nil)).toRight {
      new MissingPublishOptionError(
        "compute version",
        "--compute-version",
        "publish.computeVersion"
      )
    }
  }
}
