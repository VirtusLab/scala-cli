package scala.cli.commands.fix

import coursier.cache.FileCache

import scala.build.EitherCps.{either, value}
import scala.build.compiler.ScalaCompilerMaker
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData}
import scala.build.internal.{Constants, Runner}
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.options.BuildOptions
import scala.build.{Build, Logger, ScalafixArtifacts}
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.util.BuildCommandHelpers.copyOutput
import scala.cli.commands.util.CommandHelpers

object ScalafixRules extends CommandHelpers {
  def runRules(
    buildOptions: BuildOptions,
    scalafixOptions: ScalafixOptions,
    sharedOptions: SharedOptions,
    inputs: Inputs,
    compilerMaker: ScalaCompilerMaker,
    workspace: os.Path,
    check: Boolean,
    actionableDiagnostics: Option[Boolean],
    logger: Logger
  )(using ScalaCliInvokeData): Either[BuildException, Int] = {
    sharedOptions.semanticDbOptions.semanticDb match {
      case Some(false) =>
        logger.message(
          s"""$warnPrefix SemanticDB files' generation was explicitly set to false.
             |$warnPrefix Some scalafix rules require .semanticdb files and may not work properly."""
            .stripMargin
        )
      case Some(true) =>
        logger.debug("SemanticDB files' generation enabled.")
      case None =>
        logger.debug("Defaulting SemanticDB files' generation to true, to satisfy scalafix needs.")
    }
    val buildOptionsWithSemanticDb =
      if buildOptions.scalaOptions.semanticDbOptions.generateSemanticDbs.isEmpty then
        buildOptions.copy(scalaOptions =
          buildOptions.scalaOptions.copy(semanticDbOptions =
            buildOptions.scalaOptions.semanticDbOptions.copy(generateSemanticDbs =
              Some(true)
            )
          )
        )
      else buildOptions

    val shouldBuildTestScope = sharedOptions.scope.test.getOrElse(true)
    if !shouldBuildTestScope then
      logger.message(
        s"""$warnPrefix Building test scope was explicitly disabled.
           |$warnPrefix Some scalafix rules may not work correctly with test scope inputs."""
          .stripMargin
      )
    val res = Build.build(
      inputs,
      buildOptionsWithSemanticDb,
      compilerMaker,
      None,
      logger,
      crossBuilds = false,
      buildTests = shouldBuildTestScope,
      partial = None,
      actionableDiagnostics = actionableDiagnostics
    )
    val builds = res.orExit(logger)

    builds.builds match
      case b if b.forall(_.success) =>
        val successfulBuilds = b.collect { case s: Build.Successful => s }
        successfulBuilds.foreach(_.copyOutput(sharedOptions))
        val classPaths = successfulBuilds.flatMap(_.fullClassPath).distinct
        val scalacOptions =
          successfulBuilds.headOption.toSeq
            .flatMap(_.options.scalaOptions.scalacOptions.toSeq.map(_.value.value))

        val scalaVersion = {
          for {
            b           <- successfulBuilds.headOption
            scalaParams <- b.scalaParams
          } yield scalaParams.scalaVersion
        }.getOrElse(Constants.defaultScalaVersion)

        either {
          val artifacts =
            value(
              ScalafixArtifacts.artifacts(
                scalaVersion,
                successfulBuilds.headOption.toSeq
                  .flatMap(_.options.classPathOptions.scalafixDependencies.values.flatten),
                value(buildOptions.finalRepositories),
                logger,
                buildOptions.internal.cache.getOrElse(FileCache())
              )
            )

          val scalafixCliOptions =
            scalafixOptions.scalafixConf.toList.flatMap(scalafixConf =>
              List("--config", scalafixConf)
            ) ++
              Seq("--sourceroot", workspace.toString) ++
              Seq("--classpath", classPaths.mkString(java.io.File.pathSeparator)) ++
              Seq("--scala-version", scalaVersion) ++
              (if check then Seq("--test") else Nil) ++
              (if scalacOptions.nonEmpty then scalacOptions.flatMap(Seq("--scalac-options", _))
               else Nil) ++
              (if artifacts.toolsJars.nonEmpty then
                 Seq("--tool-classpath", artifacts.toolsJars.mkString(java.io.File.pathSeparator))
               else Nil) ++
              scalafixOptions.scalafixRules.flatMap(Seq("-r", _))
              ++ scalafixOptions.scalafixArg

          val proc = Runner.runJvm(
            buildOptions.javaHome().value.javaCommand,
            buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
            artifacts.scalafixJars,
            "scalafix.cli.Cli",
            scalafixCliOptions,
            logger,
            cwd = Some(workspace),
            allowExecve = true
          )

          proc.waitFor()
        }

  }
}
