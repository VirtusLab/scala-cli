package scala.cli.errors

import scala.build.errors.BuildException

final class WrongSonatypeServerError(isCentralPortal: Boolean)
    extends BuildException(
      if isCentralPortal then
        """Access denied by the Sonatype Central Portal.
          |Make sure the namespace you publish under is verified for your account at https://central.sonatype.com,
          |and that you're using a Central Portal user token as credentials""".stripMargin
      else
        "Wrong Sonatype server, legacy OSSRH servers are EOL since 2025-06-30, try 'central'"
    )
