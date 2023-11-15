package scala.cli.errors

import scala.build.errors.BuildException

final class InvalidSonatypePublishCredentials(usernameIsAscii: Boolean, passwordIsAscii: Boolean)
    extends BuildException(
      if (usernameIsAscii && passwordIsAscii)
        "Username or password to the publish repository are incorrect"
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
