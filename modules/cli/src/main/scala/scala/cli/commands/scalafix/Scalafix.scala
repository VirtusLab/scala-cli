package scala.cli.commands.scalafix

import caseapp.*
import caseapp.core.help.HelpFormat
import coursier.cache.FileCache
import dependency.*
import scalafix.interfaces.ScalafixError.*
import scalafix.interfaces.{
  Scalafix as ScalafixInterface,
  ScalafixError,
  ScalafixException,
  ScalafixRule
}

import java.io.File
import java.util.Optional

import scala.build.EitherCps.{either, value}
import scala.build.input.{Inputs, Script, SourceScalaFile}
import scala.build.internal.{Constants, ExternalBinaryParams, FetchExternalBinary, Runner}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Artifacts, Build, BuildThreads, Logger, ScalafixArtifacts, Sources}
import scala.cli.CurrentParams
import scala.cli.commands.compile.Compile.buildOptionsOrExit
import scala.cli.commands.fmt.FmtUtil.*
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.{ScalaCommand, SpecificationLevel, compile}
import scala.cli.config.Keys
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.collection.mutable
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object Scalafix extends ScalaCommand[ScalafixOptions] {
  override def group: String = HelpCommandGroup.Main.toString
  override def sharedOptions(options: ScalafixOptions): Option[SharedOptions] = Some(options.shared)
  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.EXPERIMENTAL

  val hiddenHelpGroups: Seq[HelpGroup] =
    Seq(
      HelpGroup.Scala,
      HelpGroup.Java,
      HelpGroup.Dependency,
      HelpGroup.ScalaJs,
      HelpGroup.ScalaNative,
      HelpGroup.CompilationServer,
      HelpGroup.Debug
    )
  override def helpFormat: HelpFormat = super.helpFormat
    .withHiddenGroups(hiddenHelpGroups)
    .withHiddenGroupsWhenShowHidden(hiddenHelpGroups)
    .withPrimaryGroup(HelpGroup.Format)
  override def names: List[List[String]] = List(
    List("scalafix")
  )

  override def runCommand(options: ScalafixOptions, args: RemainingArgs, logger: Logger): Unit = {
    val buildOptions = buildOptionsOrExit(options)
    val buildOptionsWithSemanticDb = buildOptions.copy(scalaOptions =
      buildOptions.scalaOptions.copy(semanticDbOptions =
        buildOptions.scalaOptions.semanticDbOptions.copy(generateSemanticDbs = Some(true))
      )
    )
    val inputs        = options.shared.inputs(args.all).orExit(logger)
    val threads       = BuildThreads.create()
    val compilerMaker = options.shared.compilerMaker(threads)
    val configDb      = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val workspace =
      if (args.all.isEmpty) os.pwd
      else inputs.workspace

    val scalaVersion =
      options.buildOptions.orExit(logger).scalaParams.orExit(logger).map(_.scalaVersion)
        .getOrElse(Constants.defaultScalaVersion)
    val scalaBinVersion =
      options.buildOptions.orExit(logger).scalaParams.orExit(logger).map(_.scalaBinaryVersion)

    val configFilePathOpt = options.scalafixConf.map(os.Path(_, os.pwd))

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
        val classPaths = build.fullClassPath

        val scalacOptions = options.shared.scalac.scalacOption ++
          build.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)

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

          val scalafixOptions =
            options.scalafixConf.toList.flatMap(scalafixConf => List("--config", scalafixConf)) ++
              Seq("--sourceroot", workspace.toString) ++
              Seq("--classpath", classPaths.mkString(java.io.File.pathSeparator)) ++
              Seq("--scala-version", scalaVersion) ++
              (if (options.check) Seq("--test") else Nil) ++
              (if (scalacOptions.nonEmpty) scalacOptions.flatMap(Seq("--scalac-options", _))
               else Nil) ++
              (if (artifacts.toolsJars.nonEmpty)
                 Seq("--tool-classpath", artifacts.toolsJars.mkString(java.io.File.pathSeparator))
               else Nil) ++
              options.rules.flatMap(Seq("-r", _))
              ++ options.scalafixArg

          val proc = Runner.runJvm(
            buildOptions.javaHome().value.javaCommand,
            buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
            artifacts.scalafixJars,
            "scalafix.cli.Cli",
            scalafixOptions,
            logger,
            cwd = Some(workspace),
            allowExecve = true
          )

          sys.exit(proc.waitFor())
        }

  }

}
