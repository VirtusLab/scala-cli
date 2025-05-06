package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions}
import scala.cli.errors.MissingPublishOptionError

final case class RepositoryCheck(
  options: PublishSetupOptions,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Repository
  def fieldName     = "repository"
  def directivePath = "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".repository"
  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.retained(options.publishParams.setupCi).repository.nonEmpty
  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] = {
    val maybeRepo = options.publishRepo.publishRepository
      .toRight(RepositoryCheck.missingValueError)
      .orElse {
        if (options.publishParams.setupCi) {
          val repoFromLocal = pubOpt.retained(isCi = false)
            .repository
            .toRight(RepositoryCheck.missingValueError)
          repoFromLocal.foreach { repoName =>
            logger.message("repository:")
            logger.message(s"  using repository from local configuration: $repoName")
          }
          repoFromLocal
        }
        else Left(RepositoryCheck.missingValueError)
      }

    maybeRepo.map(repo => OptionCheck.DefaultValue.simple(repo, Nil, Nil))
  }
}

object RepositoryCheck {
  def missingValueError = new MissingPublishOptionError(
    "repository",
    "--publish-repository",
    "publish.repository",
    extraMessage = "use 'central' or 'central-s01' to publish to Maven Central via Sonatype."
  )
}
