package scala.cli.commands.bloop

import caseapp.core.RemainingArgs

import scala.build.Logger
import scala.build.blooprifle.BloopRifleConfig
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.{CoursierOptions, LoggingOptions}
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.util.SharedCompilationServerOptionsUtil.*

object BloopOutput extends ScalaCommand[BloopOutputOptions] {
  
  override def hidden       = true
  
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED
  
  override def names: List[List[String]] = List(
    List("bloop", "output")
  )
  override def runCommand(options: BloopOutputOptions, args: RemainingArgs, logger: Logger): Unit = {
    val bloopRifleConfig = options.compilationServer.bloopRifleConfig(
      logger,
      CoursierOptions().coursierCache(logger.coursierLogger("Downloading Bloop")), // unused here
      options.logging.verbosity,
      "unused-java", // unused here
      options.directories.directories
    )
    val outputFile = bloopRifleConfig.address match {
      case s: BloopRifleConfig.Address.DomainSocket =>
        logger.debug(s"Bloop server directory: ${s.path}")
        logger.debug(s"Bloop server output path: ${s.outputPath}")
        os.Path(s.outputPath, os.pwd)
      case tcp: BloopRifleConfig.Address.Tcp =>
        if (options.logging.verbosity >= 0)
          System.err.println(
            s"Error: Bloop server is listening on TCP at ${tcp.render}, output not available."
          )
        sys.exit(1)
    }
    if (!os.isFile(outputFile)) {
      if (options.logging.verbosity >= 0)
        System.err.println(s"Error: $outputFile not found")
      sys.exit(1)
    }
    val content = os.read.bytes(outputFile)
    logger.debug(s"Read ${content.length} bytes from $outputFile")
    System.out.write(content)
  }
}
