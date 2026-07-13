package scala.build.postprocessing

import coursier.cache.FileCache
import dependency.*
import os.Path

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, SlothAgentError}
import scala.build.internal.Constants
import scala.build.internal.CsLoggerUtil.*
import scala.build.options.BuildOptions
import scala.build.{Artifacts, Logger, Positioned}

object SlothAgent {

  def javaAgentArgs(
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[String]] =
    if options.notForBloopOptions.slothAgent then
      either {
        val agentJar = value(fetchAgentJar(options, logger))
        Seq(s"-javaagent:$agentJar")
      }
    else Right(Nil)

  private def fetchAgentJar(
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Path] =
    either {
      val cache        = options.internal.cache.getOrElse(FileCache())
      val repositories = value(options.finalRepositories)
      val dependency   =
        dep"${Constants.slothAgentOrganization}:${Constants.slothAgentModuleName}:${Constants.slothAgentVersion}"
      val artifacts = value(
        Artifacts.artifacts(
          Seq(Positioned.none(dependency)),
          repositories,
          None,
          logger,
          cache.withMessage(s"Downloading sloth agent ${Constants.slothAgentVersion}")
        )
      )
      value {
        artifacts.headOption.map(_._2).toRight(
          SlothAgentError(s"Could not resolve sloth agent ${Constants.slothAgentVersion}")
        )
      }
    }
}
