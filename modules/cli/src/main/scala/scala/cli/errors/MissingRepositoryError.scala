package scala.cli.errors

import scala.build.errors.BuildException

final class MissingRepositoryError
    extends BuildException(
      s"Missing repository, specify one with --publish-repository or with a 'using publish.repository' directive"
    )
