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
import scala.build.{Build, Logger, Os, ScalafixArtifacts}
import scala.cli.commands.fix.ScalafixOptions
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.util.BuildCommandHelpers.copyOutput
import scala.cli.commands.util.CommandHelpers
import scala.cli.commands.util.ScalacOptionsUtil.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

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
      buildTests = true,
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
