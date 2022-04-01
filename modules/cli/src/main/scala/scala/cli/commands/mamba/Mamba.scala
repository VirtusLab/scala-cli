package scala.cli.commands.mamba

import caseapp.core.RemainingArgs
import coursier.cache.ArchiveCache

import scala.build.internal
import scala.cli.commands.ScalaCommand
import scala.cli.commands.mamba.SharedMambaOptionsOps._
import scala.cli.commands.util.CommonOps._

object Mamba extends ScalaCommand[MambaOptions] {
  override def hidden     = true
  override def inSipScala = false

  def run(options: MambaOptions, args: RemainingArgs): Unit = {
    val logger       = options.logging.logger
    val cache        = options.coursier.coursierCache(logger.coursierLogger(""))
    val archiveCache = ArchiveCache().withCache(cache)
    val launcher =
      internal.Mamba.launcher(
        options.mamba.microMambaVersion,
        options.mamba.microMambaSuffix,
        options.mamba.condaPlatform,
        archiveCache,
        options.logging.logger
      ).orExit(logger)

    if (args.remaining.nonEmpty) {
      logger.message("Warning: ignoring extra arguments " + args.remaining.mkString(", "))
      if (args.unparsed.isEmpty)
        logger.message("If you want to pass arguments to micromamba, pass them after \"--\"")
    }

    val retCode = internal.Runner.run(
      "micromamba",
      launcher.toString +: args.unparsed,
      logger,
      allowExecve = true
    ).waitFor()
    if (retCode != 0)
      sys.exit(retCode)
  }
}
