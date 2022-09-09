package scala.cli.commands

import caseapp._
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{CrossSources, Inputs, Logger, Os, Sources}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.exportCmd._

object Export extends ScalaCommand[ExportOptions] {
  override def isRestricted = true

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
        logger
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
  def sbtBuildTool(extraSettings: Seq[String], sbtVersion: String, logger: Logger): Sbt =
    Sbt(sbtVersion, extraSettings, logger)
  def millBuildTool(cache: FileCache[Task], logger: Logger): Mill = {
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
    Mill(Constants.millVersion, launchers, logger)
  }

  def run(options: ExportOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val logger = options.shared.logger

    val output = options.output.getOrElse("dest")
    val dest   = os.Path(output, os.pwd)
    if (os.exists(dest)) {
      System.err.println(
        s"""Error: $dest already exists.
           |To change the destination output directory pass --output path or remove the destination directory first.""".stripMargin
      )
      sys.exit(1)
    }

    val shouldExportToMill = options.mill.getOrElse(false)
    val shouldExportToSbt  = options.sbt.getOrElse(false)
    if (shouldExportToMill && shouldExportToSbt) {
      System.err.println(
        s"Error: Cannot export to both mill and sbt. Please pick one build tool to export."
      )
      sys.exit(1)
    }

    val buildToolName = if (shouldExportToMill) "mill" else "sbt"
    System.out.println(s"Exporting to a $buildToolName project...")

    val inputs = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val baseOptions =
      options.shared.buildOptions()
        .copy(mainClass = options.mainClass.mainClass.filter(_.nonEmpty))

    val (sourcesMain, optionsMain0) =
      prepareBuild(inputs, baseOptions, logger, options.shared.logging.verbosity, Scope.Main)
        .orExit(logger)
    val (sourcesTest, optionsTest0) =
      prepareBuild(inputs, baseOptions, logger, options.shared.logging.verbosity, Scope.Test)
        .orExit(logger)

    for {
      svMain <- optionsMain0.scalaOptions.scalaVersion
      svTest <- optionsTest0.scalaOptions.scalaVersion
    } if (svMain != svTest) {
      System.err.println(
        s"""Detected different Scala versions in main and test scopes.
           |Please set the Scala version explicitly in the main and test scope with using directives or pass -S, --scala-version as parameter""".stripMargin
      )
      sys.exit(1)
    }

    if (
      optionsMain0.scalaOptions.scalaVersion.isEmpty && optionsTest0.scalaOptions.scalaVersion.nonEmpty
    ) {
      System.err.println(
        s"""Detected that the Scala version is only set in test scope.
           |Please set the Scala version explicitly in the main and test scopes with using directives or pass -S, --scala-version as parameter""".stripMargin
      )
      sys.exit(1)
    }

    val sbtVersion = options.sbtVersion.getOrElse("1.6.1")
    def sbtBuildTool0 =
      sbtBuildTool(options.sbtSetting.map(_.trim).filter(_.nonEmpty), sbtVersion, logger)

    val buildTool =
      if (shouldExportToMill)
        millBuildTool(options.shared.coursierCache, logger)
      else // shouldExportToSbt isn't checked, as it's treated as default
        sbtBuildTool0

    val project = buildTool.`export`(optionsMain0, optionsTest0, sourcesMain, sourcesTest)

    os.makeDir.all(dest)
    project.writeTo(dest)
    System.out.println(s"Exported to: $dest")
  }
}
