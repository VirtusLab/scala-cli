package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions}

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
    val repo = options.publishRepo.publishRepository.getOrElse {
      logger.message("repository:")
      logger.message(s"  using ${RepositoryCheck.defaultRepositoryDescription}")
      RepositoryCheck.defaultRepository
    }
    Right(OptionCheck.DefaultValue.simple(repo, Nil, Nil))
  }
}

object RepositoryCheck {
  def defaultRepository            = "central-s01"
  def defaultRepositoryDescription = "Maven Central via its s01 server"
}
