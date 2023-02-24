package scala.cli.commands.publish

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat

import scala.build.options.BuildOptions
import scala.build.{BuildThreads, Logger}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.{HelpCommandGroup, SharedOptions}
import scala.cli.config.ConfigDb
import scala.cli.util.ArgHelpers.*

object PublishLocal extends ScalaCommand[PublishLocalOptions] {

  override def group: String           = HelpCommandGroup.Main.toString
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED
  override def helpFormat: HelpFormat =
    super.helpFormat
      .withHiddenGroups(Publish.hiddenHelpGroups)
      .withPrimaryGroups(Publish.primaryHelpGroups)
  override def sharedOptions(options: PublishLocalOptions): Option[SharedOptions] =
    Some(options.shared)

  override def names: List[List[String]] = List(
    List("publish", "local")
  )

  override def runCommand(
    options: PublishLocalOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    Publish.maybePrintLicensesAndExit(options.publishParams)
    Publish.maybePrintChecksumsAndExit(options.sharedPublish)

    val baseOptions = buildOptionsOrExit(options)
    val inputs      = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = Publish.mkBuildOptions(
      baseOptions,
      options.publishParams,
      options.sharedPublish,
      PublishRepositoryOptions(),
      options.scalaSigning,
      options.mainClass,
      None
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker    = options.shared.compilerMaker(threads).orExit(logger)
    val docCompilerMaker = options.shared.compilerMaker(threads, scaladoc = true).orExit(logger)

    val cross = options.compileCross.cross.getOrElse(false)

    lazy val workingDir = options.sharedPublish.workingDir
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse {
        os.temp.dir(
          prefix = "scala-cli-publish-",
          deleteOnExit = true
        )
      }

    val ivy2HomeOpt = options.sharedPublish.ivy2Home
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))

    Publish.doRun(
      inputs,
      logger,
      initialBuildOptions,
      compilerMaker,
      docCompilerMaker,
      cross,
      workingDir,
      ivy2HomeOpt,
      publishLocal = true,
      forceSigningBinary = options.sharedPublish.forceSigningBinary,
      parallelUpload = Some(true),
      options.watch.watch,
      isCi = options.publishParams.isCi,
      () => ConfigDb.empty, // shouldn't be used, no need of repo credentials here
      options.mainClass,
      dummy = options.sharedPublish.dummy
    )
  }
}
