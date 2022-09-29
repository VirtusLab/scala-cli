package scala.cli.internal

import coursier.Repositories
import coursier.cache.{ArchiveCache, FileCache}
import coursier.util.Task
import dependency._
import org.scalajs.testing.adapter.{TestAdapterInitializer => TAI}

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, ScalaJsLinkingError}
import scala.build.internal.Util.{DependencyOps, ModuleOps}
import scala.build.internal.{ExternalBinaryParams, FetchExternalBinary, Runner, ScalaJsLinkerConfig}
import scala.build.options.scalajs.ScalaJsLinkerOptions
import scala.build.{Logger, Positioned}
import scala.util.Properties

object ScalaJsLinker {

  private def linkerMainClass = "org.scalajs.cli.Scalajsld"

  private def linkerCommand(
    options: ScalaJsLinkerOptions,
    javaCommand: String,
    logger: Logger,
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task],
    scalaJsVersion: String
  ): Either[BuildException, Seq[String]] = either {

    options.linkerPath match {
      case Some(path) =>
        Seq(path.toString)
      case None =>
        val scalaJsCliVersion = options.finalScalaJsCliVersion
        val scalaJsCliDep = {
          val mod =
            if (scalaJsCliVersion.contains("-sc"))
              mod"io.github.alexarchambault.tmp:scalajs-cli_2.13"
            else
              mod"org.scala-js:scalajs-cli_2.13"
          dependency.Dependency(mod, scalaJsCliVersion)
        }

        val forcedVersions = Seq(
          mod"org.scala-js:scalajs-linker_2.13" -> scalaJsVersion
        )

        val extraRepos =
          if (scalaJsVersion.endsWith("SNAPSHOT") || scalaJsCliVersion.endsWith("SNAPSHOT"))
            Seq(Repositories.sonatype("snapshots").root)
          else
            Nil

        options.finalUseJvm match {
          case Right(()) =>
            val linkerClassPath = value {
              scala.build.Artifacts.fetch0(
                Positioned.none(Seq(scalaJsCliDep.toCs)),
                extraRepos,
                None,
                forcedVersions.map { case (m, v) => (m.toCs, v) },
                logger,
                cache,
                None
              )
            }.files

            val command = Seq[os.Shellable](
              javaCommand,
              options.javaArgs,
              "-cp",
              linkerClassPath.map(_.getAbsolutePath).mkString(File.pathSeparator),
              linkerMainClass
            )

            command.flatMap(_.value)

          case Left(osArch) =>
            val useLatest = scalaJsCliVersion == "latest"
            val ext       = if (Properties.isWin) ".zip" else ".gz"
            val tag       = if (useLatest) "launchers" else s"v$scalaJsCliVersion"
            val url =
              s"https://github.com/scala-cli/scala-js-cli-native-image/releases/download/$tag/scala-js-ld-$scalaJsVersion-$osArch$ext"
            val params = ExternalBinaryParams(
              url,
              useLatest,
              "scala-js-ld",
              Seq(scalaJsCliDep),
              linkerMainClass,
              forcedVersions = forcedVersions,
              extraRepos = extraRepos
            )
            val binary = value {
              FetchExternalBinary.fetch(params, archiveCache, logger, () => javaCommand)
            }
            binary.command
        }
    }
  }

  def link(
    options: ScalaJsLinkerOptions,
    javaCommand: String,
    classPath: Seq[os.Path],
    mainClassOrNull: String,
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    linkingDir: os.Path,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger,
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task],
    scalaJsVersion: String
  ): Either[BuildException, Unit] = either {

    val command = value {
      linkerCommand(options, javaCommand, logger, cache, archiveCache, scalaJsVersion)
    }

    val allArgs = {
      val outputArgs = Seq("--outputDir", linkingDir.toString)
      val mainClassArgs =
        Option(mainClassOrNull).toSeq.flatMap(mainClass => Seq("--mainMethod", mainClass + ".main"))
      val testInitializerArgs =
        if (addTestInitializer)
          Seq("--mainMethodWithNoArgs", TAI.ModuleClassName + "." + TAI.MainMethodName)
        else
          Nil
      val optArg =
        if (noOpt) "--noOpt"
        else if (fullOpt) "--fullOpt"
        else "--fastOpt"

      Seq[os.Shellable](
        outputArgs,
        mainClassArgs,
        testInitializerArgs,
        optArg,
        config.linkerCliArgs,
        classPath.map(_.toString)
      )
    }

    val cmd     = command ++ allArgs.flatMap(_.value)
    val res     = Runner.run(cmd, logger)
    val retCode = res.waitFor()

    if (retCode == 0)
      logger.debug("Scala.js linker ran successfully")
    else {
      logger.debug(s"Scala.js linker exited with return code $retCode")
      value(Left(new ScalaJsLinkingError))
    }
  }
  def updateSourceMappingURL(mainJsPath: os.Path) =
    val content = os.read(mainJsPath)
    content.replace(
      "//# sourceMappingURL=main.js.map",
      s"//# sourceMappingURL=${mainJsPath.last}.map"
    )

}
