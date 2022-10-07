package scala.cli.commands.publish

import coursier.cache.FileCache
import coursier.util.Task
import sttp.client3._

import scala.build.Logger
import scala.cli.commands.publish.checks._
import scala.cli.config.ConfigDb

object OptionChecks {

  def checks(
    options: PublishSetupOptions,
    configDb: => ConfigDb,
    workspace: os.Path,
    coursierCache: FileCache[Task],
    logger: Logger,
    backend: SttpBackend[Identity, Any]
  ): Seq[OptionCheck] =
    Seq(
      OrganizationCheck(options, workspace, logger),
      NameCheck(options, workspace, logger),
      ComputeVersionCheck(options, workspace, logger),
      RepositoryCheck(options, logger),
      UserCheck(options, () => configDb, workspace, logger),
      PasswordCheck(options, () => configDb, workspace, logger),
      PgpSecretKeyCheck(options, coursierCache, () => configDb, logger, backend),
      LicenseCheck(options, logger),
      UrlCheck(options, workspace, logger),
      ScmCheck(options, workspace, logger),
      DeveloperCheck(options, () => configDb, logger)
    )

}
