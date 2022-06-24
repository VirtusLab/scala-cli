package scala.cli.commands.publish

import scala.build.errors.BuildException

final class GitRepoError(message: String) extends BuildException(message)
