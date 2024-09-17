package scala.build

import coursier.cache.FileCache
import coursier.core.{Repository, Version}
import coursier.util.Task
import dependency.*

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.CsLoggerUtil.*

final case class FixArtifacts(
  artifacts: Seq[(String, os.Path)]
)

object FixArtifacts {
  def artifacts(
    extraRepositories: Seq[Repository],
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, FixArtifacts] = either {
    val scalafixDeps = Seq(dep"ch.epfl.scala:scalafix-cli_2.13.14:0.12.1")
    val fixArtifacts = Artifacts.artifacts(
      scalafixDeps.map(Positioned.none),
      extraRepositories,
      None,
      logger,
      cache.withMessage(s"Downloading 0.12.1")
    )
    FixArtifacts(
      artifacts = value(fixArtifacts)
    )
  }
}
