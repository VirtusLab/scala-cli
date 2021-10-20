package scala.cli.commands

import caseapp._

import scala.build.internal.{CustomCodeWrapper, Runner}
import scala.build.{CrossSources, Inputs, Sources}
import scala.cli.internal.FetchExternalBinary

object Fmt extends ScalaCommand[FmtOptions] {
  override def group                              = "Miscellaneous"
  override def sharedOptions(options: FmtOptions) = Some(options.shared)
  override def names = List(
    List("fmt"),
    List("format"),
    List("scalafmt")
  )
  def run(options: FmtOptions, args: RemainingArgs): Unit = {

    // TODO If no input is given, just pass '.' to scalafmt?
    val (sourceFiles, workspace, inputsOpt) =
      if (args.remaining.isEmpty)
        (Seq(os.pwd), os.pwd, None)
      else {
        val i = options.shared.inputsOrExit(args)
        val s = i.sourceFiles().collect {
          case sc: Inputs.Script    => sc.path
          case sc: Inputs.ScalaFile => sc.path
        }
        (s, i.workspace, Some(i))
      }

    val logger = options.shared.logger
    val cache  = options.shared.coursierCache

    if (sourceFiles.isEmpty)
      logger.debug("No source files, not formatting anything")
    else {

      def scalaVerOpt = inputsOpt.map { inputs =>
        val crossSources =
          CrossSources.forInputs(
            inputs,
            Sources.defaultPreprocessors(
              options.buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
            )
          ).orExit(logger)
        val sharedOptions = crossSources.sharedOptions(options.buildOptions)
        sharedOptions
          .scalaParams
          .orExit(logger)
          .scalaVersion
      }

      def dialectOpt = options.dialect.map(_.trim).filter(_.nonEmpty).orElse {
        scalaVerOpt.flatMap {
          case v if v.startsWith("2.12.") => Some("Scala212")
          case v if v.startsWith("2.13.") => Some("Scala213")
          case v if v.startsWith("3.")    => Some("Scala3")
          case _                          => None
        }
      }

      val dialectArgs =
        if (options.scalafmtArg.isEmpty && !os.exists(workspace / ".scalafmt.conf"))
          dialectOpt.toSeq.flatMap(dialect => Seq("--config-str", s"runner.dialect=$dialect"))
        else
          Nil

      val fmtLauncher = options.scalafmtLauncher.filter(_.nonEmpty) match {
        case Some(launcher) =>
          os.Path(launcher, os.pwd)
        case None =>
          val (url, changing) = options.binaryUrl
          FetchExternalBinary.fetch(url, changing, cache, logger, "scalafmt")
      }

      logger.debug(s"Using scalafmt launcher $fmtLauncher")

      val command = Seq(fmtLauncher.toString) ++
        sourceFiles.map(_.toString) ++
        dialectArgs ++
        options.scalafmtArg
      Runner.run(
        "scalafmt",
        command,
        logger,
        allowExecve = true,
        cwd = Some(workspace)
      )
    }
  }
}
