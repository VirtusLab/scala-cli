package scala.cli.errors

import scala.build.errors.BuildException

final class InvalidPublishCredentials
    extends BuildException(
      "Username or password to the publish repository is invalid"
    )
