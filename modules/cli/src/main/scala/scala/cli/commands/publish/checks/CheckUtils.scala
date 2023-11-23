package scala.cli.commands.publish.checks

import java.net.URI

import scala.build.Logger
import scala.build.options.PublishOptions as BPublishOptions
import scala.cli.commands.publish.{PublishSetupOptions, RepoParams}

object CheckUtils {

  /** Keep in mind that combinedOptions do not contain all options from cliOptions, e.g.
    * publishRepo.publishRepository is not propagated
    */
  def getRepoOpt(
    cliOptions: PublishSetupOptions,
    combinedOptions: BPublishOptions
  ): Option[String] =
    cliOptions.publishRepo.publishRepository
      .orElse {
        combinedOptions.contextual(cliOptions.publishParams.setupCi).repository
      }
      .orElse {
        if (cliOptions.publishParams.setupCi)
          combinedOptions.contextual(isCi = false).repository
        else
          None
      }

  def getHostOpt(
    options: PublishSetupOptions,
    pubOpt: BPublishOptions,
    workspace: os.Path,
    logger: Logger
  ): Option[String] =
    getRepoOpt(options, pubOpt).flatMap { repo =>
      RepoParams(
        repo,
        pubOpt.versionControl.map(_.url),
        workspace,
        None,
        false,
        null,
        logger
      ) match {
        case Left(ex) =>
          logger.debug("Caught exception when trying to compute host to check user credentials")
          logger.debug(ex)
          None
        case Right(params) =>
          Some(new URI(params.repo.snapshotRepo.root).getHost)
      }
    }
}
