package scala.cli.commands.export0

import caseapp.*
import caseapp.core.help.HelpFormat
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.google.gson.{Gson, GsonBuilder}
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}

import java.io.{OutputStreamWriter, PrintStream}
import java.nio.charset.{Charset, StandardCharsets}

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Platform, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.SharedOptions
import scala.cli.exportCmd.*
import scala.cli.util.ArgHelpers.*
import scala.util.Using

object Export extends ScalaCommand[ExportOptions] {
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  override def helpFormat: HelpFormat = super.helpFormat.withPrimaryGroup("Build Tool export")

  private def prepareBuild(
    inputs: Inputs,
    buildOptions: BuildOptions,
    logger: Logger,
    verbosity: Int,
    scope: Scope
  ): Either[BuildException, (Sources, BuildOptions)] = either {

    logger.log("Preparing build")

    val (crossSources, _) = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
          buildOptions.archiveCache,
          buildOptions.internal.javaClassNameVersionOpt,
          () => buildOptions.javaHome().value.javaCommand
        ),
        logger,
        buildOptions.suppressWarningOptions.suppressDirectivesInMultipleFilesWarning
      )
    }
    val scopedSources = value(crossSources.scopedSources(buildOptions))
    val sources       = scopedSources.sources(scope, crossSources.sharedOptions(buildOptions))

    if (verbosity >= 3)
      pprint.err.log(sources)

    val options0 = buildOptions.orElse(sources.buildOptions)

    (sources, options0)
  }

  // FIXME Auto-update those
  def sbtProjectDescriptor(
    extraSettings: Seq[String],
    sbtVersion: String,
    logger: Logger
  ): SbtProjectDescriptor =
    SbtProjectDescriptor(sbtVersion, extraSettings, logger)
  def millProjectDescriptor(
    cache: FileCache[Task],
    projectName: Option[String],
    logger: Logger
  ): MillProjectDescriptor = {
    val launcherArtifacts = Seq(
      os.rel / "mill" -> s"https://github.com/lefou/millw/raw/${Constants.lefouMillwRef}/millw",
      os.rel / "mill.bat" -> s"https://github.com/lefou/millw/raw/${Constants.lefouMillwRef}/millw.bat"
    )
    val launcherTasks = launcherArtifacts.map {
      case (path, url) =>
        val art = Artifact(url).withChanging(true)
        cache.file(art).run.flatMap {
          case Left(e) => Task.fail(e)
          case Right(f) => Task.delay {
              val content = os.read.bytes(os.Path(f, Os.pwd))
              path -> content
            }
        }
    }
    val launchersTask = cache.logger.using(Task.gather.gather(launcherTasks))
    val launchers     = launchersTask.unsafeRun()(cache.ec)
    MillProjectDescriptor(Constants.millVersion, projectName, launchers, logger)
  }

  def jsonProjectDescriptor(projectName: Option[String], logger: Logger): JsonProjectDescriptor =
    JsonProjectDescriptor(projectName, logger)

  override def sharedOptions(opts: ExportOptions): Option[SharedOptions] = Some(opts.shared)

  override def runCommand(options: ExportOptions, args: RemainingArgs, logger: Logger): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)

    val output = options.output.getOrElse("dest")
    val dest   = os.Path(output, os.pwd)
    if (os.exists(dest)) {
      logger.error(
        s"""Error: $dest already exists.
           |To change the destination output directory pass --output path or remove the destination directory first.""".stripMargin
      )
      sys.exit(1)
    }

    val shouldExportToJson = options.json.getOrElse(false)
    val shouldExportToMill = options.mill.getOrElse(false)
    val shouldExportToSbt  = options.sbt.getOrElse(false)
    if (shouldExportToMill && shouldExportToSbt) {
      logger.error(
        s"Error: Cannot export to both mill and sbt. Please pick one build tool to export."
      )
      sys.exit(1)
    }

    if (!shouldExportToJson) {
      val buildToolName = if (shouldExportToMill) "mill" else "sbt"
      logger.message(s"Exporting to a $buildToolName project...")
    }
    else
      logger.message(s"Exporting to JSON...")

    val inputs = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val baseOptions =
      initialBuildOptions.copy(mainClass = options.mainClass.mainClass.filter(_.nonEmpty))

    val (sourcesMain, optionsMain0) =
      prepareBuild(
        inputs,
        baseOptions,
        logger,
        options.shared.logging.verbosity,
        Scope.Main
      )
        .orExit(logger)
    val (sourcesTest, optionsTest0) =
      prepareBuild(
        inputs,
        baseOptions,
        logger,
        options.shared.logging.verbosity,
        Scope.Test
      )
        .orExit(logger)

    for {
      svMain <- optionsMain0.scalaOptions.scalaVersion
      svTest <- optionsTest0.scalaOptions.scalaVersion
    } if (svMain != svTest) {
      logger.error(
        s"""Detected different Scala versions in main and test scopes.
           |Please set the Scala version explicitly in the main and test scope with using directives or pass -S, --scala-version as parameter""".stripMargin
      )
      sys.exit(1)
    }

    if (
      optionsMain0.scalaOptions.scalaVersion.isEmpty && optionsTest0.scalaOptions.scalaVersion.nonEmpty
    ) {
      logger.error(
        s"""Detected that the Scala version is only set in test scope.
           |Please set the Scala version explicitly in the main and test scopes with using directives or pass -S, --scala-version as parameter""".stripMargin
      )
      sys.exit(1)
    }

    val sbtVersion = options.sbtVersion.getOrElse("1.6.1")

    def sbtProjectDescriptor0 =
      sbtProjectDescriptor(options.sbtSetting.map(_.trim).filter(_.nonEmpty), sbtVersion, logger)

    val projectDescriptor =
      if (shouldExportToMill)
        millProjectDescriptor(options.shared.coursierCache, options.project, logger)
      else if (shouldExportToJson)
        jsonProjectDescriptor(options.project, logger)
      else // shouldExportToSbt isn't checked, as it's treated as default
        sbtProjectDescriptor0

    val project = projectDescriptor.`export`(optionsMain0, optionsTest0, sourcesMain, sourcesTest)

    os.makeDir.all(dest)
    project.writeTo(dest)
    logger.message(s"Exported to: $dest")
  }
}
