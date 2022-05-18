package scala.cli.commands

import caseapp._
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.google.gson.GsonBuilder

import java.nio.charset.Charset

import scala.build.EitherCps.{either, value}
import scala.build.Inputs.WorkspaceOrigin
import scala.build.bsp.IdeInputs
import scala.build.errors.{BuildException, WorkspaceError}
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Artifacts, CrossSources, Inputs, Logger, Os, Sources}
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.errors.FoundVirtualInputsError
import scala.jdk.CollectionConverters._

object SetupIde extends ScalaCommand[SetupIdeOptions] {

  def downloadDeps(
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Artifacts] = {

    // ignoring errors related to sources themselves
    val maybeSourceBuildOptions = either {
      val crossSources = value {
        CrossSources.forInputs(
          inputs,
          Sources.defaultPreprocessors(CustomCodeWrapper),
          logger
        )
      }

      value(crossSources.scopedSources(options))
        .sources(Scope.Main, crossSources.sharedOptions(options))
        .buildOptions
    }

    val joinedBuildOpts = maybeSourceBuildOptions.toOption.map(options orElse _).getOrElse(options)
    joinedBuildOpts.artifacts(logger, Scope.Main)
  }

  def run(options: SetupIdeOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val bspPath = writeBspConfiguration(
      options,
      inputs,
      previousCommandName = None,
      args = args.all
    ).orExit(options.shared.logging.logger)

    bspPath.foreach(path => println(s"Wrote configuration file for ide in: $path"))
  }

  def runSafe(
    options: SharedOptions,
    inputs: Inputs,
    logger: Logger,
    previousCommandName: Option[String],
    args: Seq[String]
  ): Unit =
    writeBspConfiguration(
      SetupIdeOptions(shared = options),
      inputs,
      previousCommandName,
      args
    ) match {
      case Left(ex) =>
        logger.debug(s"Ignoring error during setup-ide: ${ex.message}")
      case Right(_) =>
    }

  private def buildOptions(opts: SetupIdeOptions): BuildOptions =
    opts.shared.buildOptions()

  private def writeBspConfiguration(
    options: SetupIdeOptions,
    inputs: Inputs,
    previousCommandName: Option[String],
    args: Seq[String]
  ): Either[BuildException, Option[os.Path]] = either {

    val virtualInputs = inputs.elements.collect {
      case v: Inputs.Virtual => v
    }
    if (virtualInputs.nonEmpty)
      value(Left(new FoundVirtualInputsError(virtualInputs)))

    val progName = argvOpt.flatMap(_.headOption).getOrElse {
      sys.error("setup-ide called in a non-standard way :|")
    }

    val logger = options.shared.logger

    if (buildOptions(options).classPathOptions.extraDependencies.toSeq.nonEmpty)
      value(downloadDeps(inputs, buildOptions(options), logger))

    val (bspName, bspJsonDestination) = bspDetails(inputs.workspace, options.bspFile)
    val scalaCliBspJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-options-v2.json"
    val scalaCliBspInputsJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-inputs.json"

    val inputArgs = inputs.elements.collect { case d: Inputs.OnDisk => d.path.toString }

    val ideInputs = IdeInputs(
      options.shared.validateInputArgs(args)
        .flatMap(_.toOption)
        .flatten
        .collect { case d: Inputs.OnDisk => d.path.toString }
    )

    val debugOpt = options.shared.jvm.bspDebugPort.toSeq.map(port =>
      s"-J-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:$port,suspend=y"
    )

    val bspArgs =
      List(CommandUtils.getAbsolutePathToScalaCli(progName), "bsp") ++
        debugOpt ++
        List("--json-options", scalaCliBspJsonDestination.toString) ++
        inputArgs
    val details = new BspConnectionDetails(
      bspName,
      bspArgs.asJava,
      Constants.version,
      scala.build.blooprifle.internal.Constants.bspVersion,
      List("scala", "java").asJava
    )

    val charset = options.charset
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(Charset.forName)
      .getOrElse(Charset.defaultCharset()) // Should it be UTF-8?

    val gson = new GsonBuilder().setPrettyPrinting().create()

    val json                      = gson.toJson(details)
    val scalaCliOptionsForBspJson = writeToArray(options.shared)(SharedOptions.jsonCodec)
    val scalaCliBspInputsJson     = writeToArray(ideInputs)

    if (inputs.workspaceOrigin.contains(WorkspaceOrigin.HomeDir))
      value(Left(new WorkspaceError(
        """scala-cli can not determine where to write its BSP configuration.
          |Set an explicit BSP directory path via `--bsp-directory`.
          |""".stripMargin
      )))

    if (previousCommandName.isEmpty || !bspJsonDestination.toIO.exists()) {
      os.write.over(bspJsonDestination, json.getBytes(charset), createFolders = true)
      os.write.over(
        scalaCliBspJsonDestination,
        scalaCliOptionsForBspJson,
        createFolders = true
      )
      os.write.over(
        scalaCliBspInputsJsonDestination,
        scalaCliBspInputsJson,
        createFolders = true
      )
      logger.debug(s"Wrote $bspJsonDestination")
      Some(bspJsonDestination)
    }
    else
      None
  }

  def bspDetails(workspace: os.Path, ops: SharedBspFileOptions): (String, os.Path) = {
    import ops._
    val dir = bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(workspace / ".bsp")
    val bspName0 = bspName.map(_.trim).filter(_.nonEmpty).getOrElse("scala-cli")

    (bspName0, dir / s"$bspName0.json")
  }
}
