package scala.build.postprocessing

import coursier.cache.FileCache
import dependency.*

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, SlothAgentError}
import scala.build.internal.Constants
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.util.WarningMessages
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.options.BuildOptions
import scala.build.{Artifacts, Logger, Positioned}

object SlothAgent:

  def javaAgentArgs(
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[String]] =
    if options.notForBloopOptions.slothAgent then
      either:
        warnIfRedundantWithBatchPatching(options, logger)
        val agentJar = value(fetchAgentJar(options, logger))
        Seq(s"-javaagent:$agentJar")
    else Right(Nil)

  private[build] def warnIfRedundantWithBatchPatching(
    options: BuildOptions,
    logger: Logger
  ): Unit =
    if options.notForBloopOptions.sloth && options.notForBloopOptions.slothAgent then
      logger.message(s"$warnPrefix ${WarningMessages.slothModesMutuallyRedundant}")
    if options.notForBloopOptions.slothStrict &&
      !options.notForBloopOptions.sloth &&
      !options.notForBloopOptions.slothAgent
    then
      logger.message(s"$warnPrefix ${WarningMessages.slothStrictRequiresPatching}")

  private def fetchAgentJar(
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, os.Path] =
    either:
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
      value(selectAgentJar(artifacts))

  private[build] def selectAgentJar(
    artifacts: Seq[(String, os.Path)]
  ): Either[BuildException, os.Path] =
    val expectedJarName =
      s"${Constants.slothAgentModuleName}-${Constants.slothAgentVersion}.jar"
    val plainJarName = s"${Constants.slothAgentModuleName}.jar"
    artifacts
      .collectFirst {
        // Maven-style: sloth-agent-<version>.jar; Ivy publishLocal often keeps sloth-agent.jar.
        case (_, path)
            if path.last == expectedJarName || path.last == plainJarName => path
      }
      .toRight(SlothAgentError(s"Could not resolve sloth agent ${Constants.slothAgentVersion}"))
