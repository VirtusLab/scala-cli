package scala.cli.commands.publish

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat

import scala.build.{BuildThreads, Logger}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.{HelpCommandGroup, SharedOptions}
import scala.cli.config.ConfigDb
import scala.cli.util.ArgHelpers.*

object PublishLocal extends ScalaCommand[PublishLocalOptions] {
  override def group: String           = HelpCommandGroup.Main.toString
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def helpFormat: HelpFormat  =
    super.helpFormat
      .withHiddenGroups(Publish.hiddenHelpGroups)
      .withPrimaryGroups(Publish.primaryHelpGroups)
  override def sharedOptions(options: PublishLocalOptions): Option[SharedOptions] =
    Some(options.shared)

  override def buildOptions(options: PublishLocalOptions): Some[scala.build.options.BuildOptions] =
    Some(options.buildOptions().orExit(options.shared.logger))

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

    if options.m2 && options.sharedPublish.ivy2Home.exists(_.trim.nonEmpty) then {
      logger.error("--m2 and --ivy2-home are mutually exclusive.")
      sys.exit(1)
    }

    val baseOptions = buildOptionsOrExit(options)
    val inputs      = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = Publish.mkBuildOptions(
      baseOptions,
      options.shared.sharedVersionOptions,
      options.publishParams,
      options.sharedPublish,
      PublishRepositoryOptions(),
      options.scalaSigning,
      PublishConnectionOptions(),
      options.mainClass,
      None
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker       = options.shared.compilerMaker(threads)
    val docCompilerMakerOpt = options.sharedPublish.docCompilerMakerOpt

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

    val m2HomeOpt = options.m2Home
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))

    Publish.doRun(
      inputs = inputs,
      logger = logger,
      initialBuildOptions = initialBuildOptions,
      compilerMaker = compilerMaker,
      docCompilerMaker = docCompilerMakerOpt,
      cross = cross,
      workingDir = workingDir,
      ivy2HomeOpt = ivy2HomeOpt,
      publishLocal = true,
      m2Local = options.m2,
      m2HomeOpt = m2HomeOpt,
      forceSigningExternally = options.scalaSigning.forceSigningExternally.getOrElse(false),
      parallelUpload = Some(true),
      watch = options.watch.watch,
      isCi = options.publishParams.isCi,
      configDb = () => ConfigDb.empty, // shouldn't be used, no need of repo credentials here
      mainClassOptions = options.mainClass,
      dummy = options.sharedPublish.dummy,
      buildTests = options.shared.scope.test.getOrElse(false)
    )
  }
}
