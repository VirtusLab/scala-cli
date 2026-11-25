package scala.cli.errors

import scala.build.errors.BuildException
import scala.cli.commands.publish.RepoParams.SonatypeServer

/** Sonatype denied access to the staging API (HTTP 403) */
final class SonatypeAccessDeniedError(server: SonatypeServer)
    extends BuildException(
      server match {
        case SonatypeServer.CentralPortal =>
          """Access denied by the Sonatype Central Portal. Check that:
            |- your credentials are a Central Portal user token,
            |- you're allowed to publish under this organization (a namespace verified for your account).""".stripMargin
        case legacy =>
          s"""Access denied by ${legacy.host}.
             |Legacy Sonatype servers reached end of life on 2025-06-30.
             |Publish to the Sonatype Central Portal instead, with the 'central' repository.""".stripMargin
      }
    )
