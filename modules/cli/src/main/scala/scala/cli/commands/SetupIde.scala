package scala.cli.commands

import caseapp._
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.GsonBuilder
import upickle.default._

import java.nio.charset.Charset

import scala.build.EitherCps.{either, value}
import scala.build.Inputs.WorkspaceOrigin
import scala.build.errors.{BuildException, WorkspaceError}
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Artifacts, CrossSources, Inputs, Logger, Sources}
import scala.cli.CurrentParams
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
    joinedBuildOpts.artifacts(logger)
  }

  def run(options: SetupIdeOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val bspPath = writeBspConfiguration(
      options,
      inputs,
      previousCommandName = None
    ).orExit(options.shared.logging.logger)

    bspPath.foreach(path => println(s"Wrote configuration file for ide in: $path"))
  }

  def runSafe(
    options: SharedOptions,
    inputs: Inputs,
    logger: Logger,
    previousCommandName: Option[String]
  ): Unit =
    writeBspConfiguration(SetupIdeOptions(shared = options), inputs, previousCommandName) match {
      case Left(ex) =>
        logger.debug(s"Ignoring error during setup-ide: ${ex.message}")
      case Right(_) =>
    }

  private def writeBspConfiguration(
    options: SetupIdeOptions,
    inputs: Inputs,
    previousCommandName: Option[String]
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

    if (options.buildOptions.classPathOptions.extraDependencies.toSeq.nonEmpty)
      value(downloadDeps(inputs, options.buildOptions, logger))

    val (bspName, bspJsonDestination) = options.bspFile.bspDetails(inputs.workspace)
    val scalaCliBspJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-options.json"

    val inputArgs = inputs.elements.collect {
      case d: Inputs.OnDisk =>
        val path = d.path
        if (os.isFile(path))
          path.toString().stripSuffix(s"${path.last}")
        else path.toString
    }

    val bspArgs =
      List(CommandUtils.getAbsolutePathToScalaCli(progName), "bsp") ++
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
    val scalaCliOptionsForBspJson = write(options.shared)

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
        scalaCliOptionsForBspJson.getBytes(charset),
        createFolders = true
      )
      logger.debug(s"Wrote $bspJsonDestination")
      Some(bspJsonDestination)
    }
    else
      None
  }
}
