package scala.cli.commands.export0

import caseapp._
import caseapp.core.help.HelpFormat
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.google.gson.{Gson, GsonBuilder}
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}

import java.io.{OutputStreamWriter, PrintStream}
import java.nio.charset.{Charset, StandardCharsets}

import scala.build.EitherCps.{either, value}
import scala.build._
import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Platform, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.shared.{HelpGroup, SharedOptions}
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.cli.exportCmd._
import scala.cli.util.ArgHelpers._
import scala.util.Using

object Export extends ScalaCommand[ExportOptions] {
  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.EXPERIMENTAL

  override def helpFormat: HelpFormat =
    super.helpFormat.withPrimaryGroup(HelpGroup.BuildToolExport)

  private def prepareBuild(
    inputs: Inputs,
    buildOptions: BuildOptions,
    logger: Logger,
    verbosity: Int,
    scope: Scope
  ): Either[BuildException, (Sources, BuildOptions)] = either {

    logger.log("Preparing build")

    val (crossSources: CrossSources, allInputs: Inputs) = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.archiveCache,
          buildOptions.internal.javaClassNameVersionOpt,
          () => buildOptions.javaHome().value.javaCommand
        ),
        logger,
        buildOptions.suppressWarningOptions,
        buildOptions.internal.exclude
      )
    }

    val scopedSources: ScopedSources = value(crossSources.scopedSources(buildOptions))
    val sources: Sources =
      scopedSources.sources(
        scope,
        crossSources.sharedOptions(buildOptions),
        inputs.workspace,
        logger
      )
        .orExit(logger)

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

  def mavenProjectDescriptor(
    mavenPluginVersion: String,
    mavenScalaPluginVersion: String,
    mavenExecPluginVersion: String,
    extraSettings: Seq[String],
    mavenGroupId: String,
    mavenArtifactId: String,
    mavenVersion: String,
    logger: Logger
  ): MavenProjectDescriptor =
    MavenProjectDescriptor(
      mavenPluginVersion,
      mavenScalaPluginVersion,
      mavenExecPluginVersion,
      extraSettings,
      mavenGroupId,
      mavenArtifactId,
      mavenVersion,
      logger
    )

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

  def jsonProjectDescriptor(
    projectName: Option[String],
    workspace: os.Path,
    logger: Logger
  ): JsonProjectDescriptor =
    JsonProjectDescriptor(projectName, workspace, logger)

  override def sharedOptions(opts: ExportOptions): Option[SharedOptions] = Some(opts.shared)

  override def runCommand(options: ExportOptions, args: RemainingArgs, logger: Logger): Unit = {
    if options.shared.scope.test.nonEmpty then {
      logger.error(
        s"""Including the test scope sources together with the main scope is currently not supported.
           |Note that test scope sources will still be exported as per the output build tool tests definition demands.""".stripMargin
      )
      sys.exit(1)
    }

    val initialBuildOptions = buildOptionsOrExit(options)

    val output                   = options.output.getOrElse("dest")
    val dest                     = os.Path(output, os.pwd)
    val shouldExportToJson       = options.json.getOrElse(false)
    val shouldExportJsonToStdout = shouldExportToJson && options.output.isEmpty

    if (!shouldExportJsonToStdout && os.exists(dest)) {
      logger.error(
        s"""Error: $dest already exists.
           |To change the destination output directory pass --output path or remove the destination directory first.""".stripMargin
      )
      sys.exit(1)
    }

    val shouldExportToMill  = options.mill.getOrElse(false)
    val shouldExportToSbt   = options.sbt.getOrElse(false)
    val shouldExportToMaven = options.maven.getOrElse(false)

    val exportOptions =
      (if shouldExportToMill then List("Mill") else Nil) ++
        (if shouldExportToSbt then List("SBT") else Nil) ++
        (if shouldExportToMaven then List("Maven") else Nil)
    val exportOptionsString = exportOptions.mkString(", ")
    if exportOptions.length > 1 then {
      logger.error(
        s"""Error: Cannot export to more than one tool at once (currently chosen: $exportOptionsString). 
           |Pick one build tool to export to.""".stripMargin
      )
      sys.exit(1)
    }

    if (!shouldExportToJson) {
      val buildToolName =
        if (shouldExportToMill) "mill" else if (shouldExportToMaven) "maven" else "sbt"
      logger.message(s"Exporting to a $buildToolName project...")
    }
    else if (!shouldExportJsonToStdout)
      logger.message(s"Exporting to JSON...")

    val inputs = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val baseOptions =
      initialBuildOptions
        .copy(
          scalaNativeOptions = initialBuildOptions.scalaNativeOptions.copy(
            maxDefaultNativeVersions =
              initialBuildOptions.scalaNativeOptions.maxDefaultNativeVersions ++
                (if shouldExportToMill && Constants.scalaNativeVersion != Constants.maxScalaNativeForMillExport
                 then
                   val warningMsg =
                     s"Mill export does not support Scala Native ${Constants.scalaNativeVersion}, ${Constants.maxScalaNativeForMillExport} should be used instead."
                   List(Constants.maxScalaNativeForMillExport -> warningMsg)
                 else Nil)
          ),
          mainClass = options.mainClass.mainClass.filter(_.nonEmpty)
        )

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

    if (shouldExportJsonToStdout) {
      val project = jsonProjectDescriptor(options.project, inputs.workspace, logger)
        .`export`(optionsMain0, optionsTest0, sourcesMain, sourcesTest)
        .orExit(logger)

      project.print(System.out)
    }
    else {
      val sbtVersion                  = options.sbtVersion.getOrElse(Constants.sbtVersion)
      val defaultMavenCompilerVersion = options.mvnVersion.getOrElse(Constants.mavenVersion)
      val defaultScalaMavenCompilerVersion =
        options.mvnScalaVersion.getOrElse(Constants.mavenScalaCompilerPluginVersion)
      val defaultMavenExecPluginVersion =
        options.mvnExecPluginVersion.getOrElse(Constants.mavenExecPluginVersion)
      val defaultMavenArtifactId =
        options.mvnAppArtifactId.getOrElse(Constants.mavenAppArtifactId)
      val defaultMavenGroupId =
        options.mvnAppGroupId.getOrElse(Constants.mavenAppGroupId)
      val defaultMavenVersion =
        options.mvnAppVersion.getOrElse(Constants.mavenAppVersion)

      def sbtProjectDescriptor0 =
        sbtProjectDescriptor(options.sbtSetting.map(_.trim).filter(_.nonEmpty), sbtVersion, logger)

      val projectDescriptor =
        if (shouldExportToMill)
          millProjectDescriptor(options.shared.coursierCache, options.project, logger)
        else if (shouldExportToMaven)
          mavenProjectDescriptor(
            defaultMavenCompilerVersion,
            defaultScalaMavenCompilerVersion,
            defaultMavenExecPluginVersion,
            Nil,
            defaultMavenGroupId,
            defaultMavenArtifactId,
            defaultMavenVersion,
            logger
          )
        else if (shouldExportToJson)
          jsonProjectDescriptor(options.project, inputs.workspace, logger)
        else // shouldExportToSbt isn't checked, as it's treated as default
          sbtProjectDescriptor0

      val project = projectDescriptor.`export`(optionsMain0, optionsTest0, sourcesMain, sourcesTest)
        .orExit(logger)

      os.makeDir.all(dest)
      project.writeTo(dest)
      logger.message(s"Exported to: $dest")
    }
  }
}
