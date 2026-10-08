package scala.cli.commands.publish

import sttp.client3.*

import scala.build.Logger
import scala.cli.commands.publish.checks.*
import scala.cli.config.ConfigDb

object OptionChecks {

  def checks(
    options: PublishSetupOptions,
    configDb: => ConfigDb,
    workspace: os.Path,
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
      PgpSecretKeyCheck(options, () => configDb, logger, backend),
      LicenseCheck(options, logger),
      UrlCheck(options, workspace, logger),
      ScmCheck(options, workspace, logger),
      DeveloperCheck(options, () => configDb, logger)
    )

}
