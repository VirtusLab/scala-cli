package scala.cli.commands.clean

import caseapp.*

import scala.build.input.Module
import scala.build.internal.Constants
import scala.build.{Logger, Os}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.setupide.SetupIde
import scala.cli.commands.shared.HelpCommandGroup

object Clean extends ScalaCommand[CleanOptions] {
  override def group: String = HelpCommandGroup.Main.toString

  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def runCommand(options: CleanOptions, args: RemainingArgs, logger: Logger): Unit = {
    val inputs = Module(
      args.all,
      Os.pwd,
      defaultInputs = () => Module.default(),
      forcedWorkspace = options.workspace.forcedWorkspaceOpt,
      allowRestrictedFeatures = allowRestrictedFeatures,
      extraClasspathWasPassed = false
    ) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val workDir       = inputs.workspace / Constants.workspaceDirName
    val (_, bspEntry) = SetupIde.bspDetails(inputs.workspace, options.bspFile)

    if (os.exists(workDir)) {
      logger.debug(s"Working directory: $workDir")
      if (os.isDir(workDir)) {
        logger.log(s"Removing $workDir")
        os.remove.all(workDir)
      }
      else
        logger.log(s"$workDir is not a directory, ignoring it.")
    }

    if (os.exists(bspEntry)) {
      logger.log(s"Removing $bspEntry")
      os.remove(bspEntry)
    }
    else
      logger.log(s"No BSP entry found, so ignoring.")
  }
}
