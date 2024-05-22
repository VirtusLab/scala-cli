package scala.cli.internal

import coursier.Repositories
import coursier.cache.{ArchiveCache, FileCache}
import coursier.core.Version
import coursier.util.Task
import dependency._
import org.scalajs.testing.adapter.{TestAdapterInitializer => TAI}

import java.io.{File, OutputStream}

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, ScalaJsLinkingError}
import scala.build.internal.Util.{DependencyOps, ModuleOps}
import scala.build.internal.{ExternalBinaryParams, FetchExternalBinary, Runner, ScalaJsLinkerConfig}
import scala.build.options.scalajs.ScalaJsLinkerOptions
import scala.build.{Logger, Positioned}
import scala.io.Source
import scala.util.Properties

object ScalaJsLinker {

  case class LinkJSInput(
    options: ScalaJsLinkerOptions,
    javaCommand: String,
    classPath: Seq[os.Path],
    mainClassOrNull: String,
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    fullOpt: Boolean,
    noOpt: Boolean,
    scalaJsVersion: String
  )

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
          val mod = mod"org.virtuslab.scala-cli:scalajscli_2.13"
          dependency.Dependency(mod, s"$scalaJsCliVersion+")
        }

        val forcedVersions = Seq(
          mod"org.scala-js:scalajs-linker_2.13" -> scalaJsVersion
        )

        val extraRepos =
          if (scalaJsVersion.endsWith("SNAPSHOT") || scalaJsCliVersion.endsWith("SNAPSHOT"))
            Seq(Repositories.sonatype("snapshots"))
          else
            Nil

        options.finalUseJvm match {
          case Right(()) =>
            val (_, linkerRes) = value {
              scala.build.Artifacts.fetchCsDependencies(
                Seq(Positioned.none(scalaJsCliDep.toCs)),
                extraRepos,
                None,
                forcedVersions.map { case (m, v) => (m.toCs, v) },
                logger,
                cache,
                None
              )
            }
            val linkerClassPath = linkerRes.files

            val command = Seq[os.Shellable](
              javaCommand,
              options.javaArgs,
              "-cp",
              linkerClassPath.map(_.getAbsolutePath).mkString(File.pathSeparator),
              linkerMainClass
            )

            command.flatMap(_.value)

          case Left(osArch) =>
            val useLatest = scalaJsVersion == "latest"
            val ext       = if (Properties.isWin) ".zip" else ".gz"
            val tag       = if (useLatest) "launchers" else s"v$scalaJsCliVersion"
            val url =
              s"https://github.com/virtusLab/scala-js-cli/releases/download/$tag/scala-js-ld-$osArch$ext"
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

  private def getCommand(
    input: LinkJSInput,
    linkingDir: os.Path,
    logger: Logger,
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task],
    useLongRunning: Boolean
  ) = either {
    val command = value {
      linkerCommand(
        input.options,
        input.javaCommand,
        logger,
        cache,
        archiveCache,
        input.scalaJsVersion
      )
    }

    val allArgs = {
      val outputArgs  = Seq("--outputDir", linkingDir.toString)
      val longRunning = if (useLongRunning) Seq("--longRunning") else Seq.empty[String]
      val mainClassArgs =
        Option(input.mainClassOrNull).toSeq.flatMap(mainClass =>
          Seq("--mainMethod", mainClass + ".main")
        )
      val testInitializerArgs =
        if (input.addTestInitializer)
          Seq("--mainMethodWithNoArgs", TAI.ModuleClassName + "." + TAI.MainMethodName)
        else
          Nil
      val optArg =
        if (input.noOpt) "--noOpt"
        else if (input.fullOpt) "--fullOpt"
        else "--fastOpt"

      Seq[os.Shellable](
        outputArgs,
        mainClassArgs,
        testInitializerArgs,
        optArg,
        input.config.linkerCliArgs,
        input.classPath.map(_.toString),
        longRunning
      )
    }

    command ++ allArgs.flatMap(_.value)
  }

  def link(
    input: LinkJSInput,
    linkingDir: os.Path,
    logger: Logger,
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task]
  ): Either[BuildException, Unit] = either {
    val useLongRunning = !input.fullOpt

    if (useLongRunning)
      longRunningProcess.runOrGet(input, linkingDir, logger, cache, archiveCache)
    else {
      val cmd =
        value(getCommand(input, linkingDir, logger, cache, archiveCache, useLongRunning = false))
      val res     = Runner.run(cmd, logger)
      val retCode = res.waitFor()

      if (retCode == 0)
        logger.debug("Scala.js linker ran successfully")
      else {
        logger.debug(s"Scala.js linker exited with return code $retCode")
        value(Left(new ScalaJsLinkingError))
      }
    }
  }

  private object longRunningProcess {
    case class Proc(process: Process, stdin: OutputStream, stdout: Iterator[String])
    case class Input(input: LinkJSInput, linkingDir: os.Path)
    var currentInput: Option[Input] = None
    var currentProc: Option[Proc]   = None

    def runOrGet(
      linkJsInput: LinkJSInput,
      linkingDir: os.Path,
      logger: Logger,
      cache: FileCache[Task],
      archiveCache: ArchiveCache[Task]
    ) = either {
      val input = Input(linkJsInput, linkingDir)

      def createProcess(): Proc = {
        val cmd =
          value(getCommand(
            linkJsInput,
            linkingDir,
            logger,
            cache,
            archiveCache,
            useLongRunning = true
          ))
        val process = Runner.run(cmd, logger, inheritStreams = false)
        val stdin   = process.getOutputStream()
        val stdout  = Source.fromInputStream(process.getInputStream()).getLines
        val proc    = Proc(process, stdin, stdout)
        currentProc = Some(proc)
        currentInput = Some(input)
        proc
      }

      def loop(proc: Proc): Unit =
        if (proc.stdout.hasNext) {
          val line = proc.stdout.next()

          if (line == "SCALA_JS_LINKING_DONE")
            logger.debug("Scala.js linker ran successfully")
          else {
            // inherit other stdout from Scala.js
            println(line)

            loop(proc)
          }
        }
        else {
          val retCode = proc.process.waitFor()
          logger.debug(s"Scala.js linker exited with return code $retCode")
          value(Left(new ScalaJsLinkingError))
        }

      val proc = currentProc match {
        case Some(proc) if currentInput.contains(input) && proc.process.isAlive() =>
          // trigger new linking
          proc.stdin.write('\n')
          proc.stdin.flush()

          proc
        case Some(proc) =>
          proc.process.destroy()
          createProcess()
        case _ =>
          createProcess()
      }

      loop(proc)
    }
  }

  def updateSourceMappingURL(mainJsPath: os.Path) =
    val content = os.read(mainJsPath)
    content.replace(
      "//# sourceMappingURL=main.js.map",
      s"//# sourceMappingURL=${mainJsPath.last}.map"
    )

}
