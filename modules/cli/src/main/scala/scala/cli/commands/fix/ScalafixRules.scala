package scala.cli.commands.fix

import coursier.cache.FileCache
import scalafix.interfaces.ScalafixError.*

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.compiler.ScalaCompilerMaker
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData}
import scala.build.internal.{Constants, Runner}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Build, Logger, ScalafixArtifacts}
import scala.cli.commands.fix.ScalafixOptions
import scala.cli.commands.util.CommandHelpers
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object ScalafixRules extends CommandHelpers {
  def runRules(
    buildOptions: BuildOptions,
    scalafixOptions: ScalafixOptions,
    inputs: Inputs,
    compilerMaker: ScalaCompilerMaker,
    workspace: os.Path,
    check: Boolean,
    actionableDiagnostics: Option[Boolean],
    logger: Logger
  )(using ScalaCliInvokeData): Either[BuildException, Int] = {
    val buildOptionsWithSemanticDb = buildOptions.copy(scalaOptions =
      buildOptions.scalaOptions.copy(semanticDbOptions =
        buildOptions.scalaOptions.semanticDbOptions.copy(generateSemanticDbs = Some(true))
      )
    )

    val scalaVersion =
      buildOptions.scalaParams.orExit(logger).map(_.scalaVersion)
        .getOrElse(Constants.defaultScalaVersion)

    val res = Build.build(
      inputs,
      buildOptionsWithSemanticDb,
      compilerMaker,
      None,
      logger,
      crossBuilds = false,
      buildTests = false,
      partial = None,
      actionableDiagnostics = actionableDiagnostics
    )
    val builds = res.orExit(logger)

    builds.get(Scope.Main).flatMap(_.successfulOpt) match
      case None => sys.exit(1)
      case Some(build) =>
        val classPaths    = build.fullClassPath
        val scalacOptions = build.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)

        either {
          val artifacts =
            value(
              ScalafixArtifacts.artifacts(
                scalaVersion,
                build.options.classPathOptions.scalafixDependencies.values.flatten,
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
              (if (check) Seq("--test") else Nil) ++
              (if (scalacOptions.nonEmpty) scalacOptions.flatMap(Seq("--scalac-options", _))
               else Nil) ++
              (if (artifacts.toolsJars.nonEmpty)
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
