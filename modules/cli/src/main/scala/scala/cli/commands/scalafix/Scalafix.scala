package scala.cli.commands.scalafix

import caseapp.*
import caseapp.core.help.HelpFormat
import dependency.*
import scalafix.interfaces.ScalafixError.*
import scalafix.interfaces.{
  ScalafixError,
  ScalafixException,
  ScalafixRule,
  Scalafix as ScalafixInterface
}

import java.util.Optional
import scala.build.input.{Inputs, Script, SourceScalaFile}
import scala.build.internal.{Constants, ExternalBinaryParams, FetchExternalBinary, Runner}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Build, BuildThreads, ScalafixArtifacts, Logger, Sources}
import scala.cli.CurrentParams
import coursier.cache.FileCache
import scala.cli.commands.compile.Compile.buildOptionsOrExit
import scala.cli.commands.fmt.FmtUtil.*
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.{compile, ScalaCommand, SpecificationLevel}
import scala.cli.config.Keys
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.collection.mutable
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.build.EitherCps.{either, value}
import java.io.File
import scala.build.Artifacts

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

    val (sourcePaths, workspace, _) =
      if (args.all.isEmpty)
        (Seq(os.pwd), os.pwd, None)
      else {
        val s = inputs.sourceFiles().collect {
          case sc: Script          => sc.path
          case sc: SourceScalaFile => sc.path
        }
        (s, inputs.workspace, Some(inputs))
      }

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
        val compileOnlyDeps = {
          val params = ScalaParameters(scalaVersion)
          build.options.classPathOptions.extraCompileOnlyDependencies.values.flatten.map(
            _.value.applyParams(params)
          )
        }

        val scalacOptions = options.shared.scalac.scalacOption ++
          build.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)

        either {
          val artifacts =
            value(
              ScalafixArtifacts.artifacts(
                scalaVersion,
                compileOnlyDeps,
                value(buildOptions.finalRepositories),
                logger,
                buildOptions.internal.cache.getOrElse(FileCache())
              )
            )

          val scalafixOptions =
            configFilePathOpt.map(file => Seq("-c", file.toString)).getOrElse(Nil) ++
              Seq("--sourceroot", workspace.toString) ++
              Seq("--classpath", classPaths.mkString(java.io.File.pathSeparator)) ++
              options.scalafixConf.toList.flatMap(scalafixConf => List("--config", scalafixConf)) ++
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

  private def prepareErrorMessage(error: ScalafixError): String = error match
    case ParseError => "A source file failed to be parsed"
    case CommandLineError =>
      "A command-line argument was parsed incorrectly"
    case MissingSemanticdbError =>
      "A semantic rewrite was run on a source file that has no associated META-INF/semanticdb/.../*.semanticdb"
    case StaleSemanticdbError =>
      """The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
        |To resolve this error re-compile the project and re-run Scalafix""".stripMargin
    case TestError =>
      "A Scalafix test error was reported. Run `fix` without `--check` or `--diff` to fix the error"
    case LinterError  => "A Scalafix linter error was reported"
    case NoFilesError => "No files were provided to Scalafix so nothing happened"
    case NoRulesError => "No rules were provided to Scalafix so nothing happened"
    case _            => "Something unexpected happened running Scalafix"

}
