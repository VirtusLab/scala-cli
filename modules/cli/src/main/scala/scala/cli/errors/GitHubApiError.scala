package scala.cli.errors

import scala.build.errors.BuildException

final class GitHubApiError(msg: String) extends BuildException(msg)
