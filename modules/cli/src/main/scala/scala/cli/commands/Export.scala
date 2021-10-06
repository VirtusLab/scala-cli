package scala.cli.commands

import caseapp._
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.CustomCodeWrapper
import scala.build.options.{BuildOptions, Scope}
import scala.build.{CrossSources, Inputs, Logger, Os, Sources}
import scala.cli.export._

object Export extends ScalaCommand[ExportOptions] {

  private def prepareBuild(
    inputs: Inputs,
    buildOptions: BuildOptions,
    logger: Logger,
    verbosity: Int
  ): Either[BuildException, (Sources, BuildOptions)] = either {

    logger.log("Preparing build")

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        )
      )
    }
    val scopedSources = value(crossSources.scopedSources(buildOptions))
    val sources       = scopedSources.sources(Scope.Main, buildOptions)

    if (verbosity >= 3)
      pprint.better.log(sources)

    val options0 = buildOptions.orElse(sources.buildOptions)

    (sources, options0)
  }

  // FIXME Auto-update those
  def sbtBuildTool(extraSettings: Seq[String]) = Sbt("1.5.5", extraSettings)
  def millBuildTool(cache: FileCache[Task]) = {
    val launcherArtifacts = Seq(
      os.rel / "mill"     -> "https://github.com/lefou/millw/raw/main/millw",
      os.rel / "mill.bat" -> "https://github.com/lefou/millw/raw/main/millw.bat"
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
    Mill(
      "0.9.8",
      launchers
    )
  }

  def run(options: ExportOptions, args: RemainingArgs): Unit = {

    val logger      = options.shared.logger
    val inputs      = options.shared.inputsOrExit(args)
    val baseOptions = options.buildOptions

    val (sources, options0) =
      prepareBuild(inputs, baseOptions, logger, options.shared.logging.verbosity)
        .orExit(logger)

    def sbtBuildTool0 = sbtBuildTool(options.sbtSetting.map(_.trim).filter(_.nonEmpty))

    val buildTool =
      if (options.sbt.getOrElse(false))
        sbtBuildTool0
      else if (options.mill.getOrElse(false))
        millBuildTool(options.shared.coursierCache)
      else
        sbtBuildTool0

    val project = buildTool.export(options0, sources)

    val output = options.output.getOrElse("dest")
    val dest   = os.Path(output, os.pwd)
    if (os.exists(dest)) {
      System.err.println(s"Error: $output already exists.")
      sys.exit(1)
    }

    os.makeDir.all(dest)
    project.writeTo(dest)
  }
}
