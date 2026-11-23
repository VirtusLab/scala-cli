package scala.cli.errors

import scala.build.errors.BuildException

final class InvalidSonatypePublishCredentials(
  usernameIsAscii: Boolean,
  passwordIsAscii: Boolean,
  isCentralPortal: Boolean
) extends BuildException(
      if (usernameIsAscii && passwordIsAscii)
        if isCentralPortal then
          """Username or password to the publish repository are incorrect.
            |The Sonatype Central Portal expects a user token rather than your account login,
            |see https://central.sonatype.org/publish/generate-portal-token/""".stripMargin
        else "Username or password to the publish repository are incorrect"
      else
        s"Your Sonatype ${InvalidSonatypePublishCredentials.isUsernameOrPassword(
            usernameIsAscii,
            passwordIsAscii
          )} unsupported characters"
    )

object InvalidSonatypePublishCredentials {
  def isUsernameOrPassword(usernameIsAscii: Boolean, passwordIsAscii: Boolean): String =
    if (!usernameIsAscii && !passwordIsAscii)
      "password and username contain"
    else if (!usernameIsAscii)
      "username contains"
    else
      "password contains"
}
