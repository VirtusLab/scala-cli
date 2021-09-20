package scala.cli.commands

import caseapp._

import scala.build.Inputs
import scala.build.internal.Runner
import scala.cli.internal.FetchExternalBinary

object Fmt extends ScalaCommand[FmtOptions] {
  override def group                              = "Miscellaneous"
  override def sharedOptions(options: FmtOptions) = Some(options.shared)
  def run(options: FmtOptions, args: RemainingArgs): Unit = {

    // TODO If no input is given, just pass '.' to scalafmt?
    val (sourceFiles, workspace) =
      if (args.remaining.isEmpty)
        (Seq(os.pwd), os.pwd)
      else {
        val i = options.shared.inputsOrExit(args)
        val s = i.sourceFiles().collect {
          case sc: Inputs.Script    => sc.path
          case sc: Inputs.ScalaFile => sc.path
        }
        (s, i.workspace)
      }

    val logger = options.shared.logger
    val cache  = options.shared.coursierCache

    if (sourceFiles.isEmpty)
      logger.debug("No source files, not formatting anything")
    else {

      val fmtLauncher = options.scalafmtLauncher.filter(_.nonEmpty) match {
        case Some(launcher) =>
          os.Path(launcher, os.pwd)
        case None =>
          val (url, changing) = options.binaryUrl
          FetchExternalBinary.fetch(url, changing, cache, logger, "scalafmt")
      }

      logger.debug(s"Using scalafmt launcher $fmtLauncher")

      val command = Seq(fmtLauncher.toString) ++ sourceFiles.map(_.toString)
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
