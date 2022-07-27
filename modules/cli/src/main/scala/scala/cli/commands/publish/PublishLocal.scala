package scala.cli.commands.publish

import caseapp.core.RemainingArgs

import scala.build.BuildThreads
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.commands.{ScalaCommand, SharedOptions}
import scala.cli.config.ConfigDb

object PublishLocal extends ScalaCommand[PublishLocalOptions] {

  override def group: String       = "Main"
  override def inSipScala: Boolean = false
  override def sharedOptions(options: PublishLocalOptions): Option[SharedOptions] =
    Some(options.shared)

  override def names: List[List[String]] = List(
    List("publish", "local")
  )

  def run(options: PublishLocalOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)

    Publish.maybePrintLicensesAndExit(options.publishParams)
    Publish.maybePrintChecksumsAndExit(options.sharedPublish)

    CurrentParams.verbosity = options.shared.logging.verbosity
    val logger = options.shared.logger
    val inputs = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = Publish.mkBuildOptions(
      options.shared,
      options.publishParams,
      options.sharedPublish,
      PublishRepositoryOptions(),
      options.mainClass,
      None
    ).orExit(logger)
    val threads = BuildThreads.create()

    val compilerMaker    = options.shared.compilerMaker(threads)
    val docCompilerMaker = options.shared.compilerMaker(threads, scaladoc = true)

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
      options.mainClass
    )
  }
}
