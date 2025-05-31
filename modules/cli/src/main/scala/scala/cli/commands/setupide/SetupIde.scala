package scala.cli.commands.setupide

import caseapp.*
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.google.gson.GsonBuilder

import java.nio.charset.{Charset, StandardCharsets}

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.bsp.IdeInputs
import scala.build.errors.{BuildException, WorkspaceError}
import scala.build.input.{Inputs, OnDisk, Virtual, WorkspaceOrigin}
import scala.build.internal.Constants
import scala.build.internals.EnvVar
import scala.build.options.{BuildOptions, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.shared.{SharedBspFileOptions, SharedOptions}
import scala.cli.commands.{CommandUtils, ScalaCommand}
import scala.cli.errors.FoundVirtualInputsError
import scala.cli.launcher.LauncherOptions
import scala.jdk.CollectionConverters.*

object SetupIde extends ScalaCommand[SetupIdeOptions] {

  def downloadDeps(
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Artifacts] = {

    // ignoring errors related to sources themselves
    val maybeSourceBuildOptions = either {
      val (crossSources, allInputs) = value {
        CrossSources.forInputs(
          inputs,
          Sources.defaultPreprocessors(
            options.archiveCache,
            options.internal.javaClassNameVersionOpt,
            () => options.javaHome().value.javaCommand
          ),
          logger,
          options.suppressWarningOptions,
          options.internal.exclude,
          download = options.downloader
        )
      }

      crossSources.sharedOptions(options)

      val scopedSources = value(crossSources.scopedSources(options))
      val mainSources = value(scopedSources.sources(
        Scope.Main,
        crossSources.sharedOptions(options),
        allInputs.workspace,
        logger
      ))

      mainSources.buildOptions
    }

    val joinedBuildOpts = maybeSourceBuildOptions.toOption.map(options orElse _).getOrElse(options)
    joinedBuildOpts.artifacts(logger, Scope.Main)
  }

  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def runCommand(options: SetupIdeOptions, args: RemainingArgs, logger: Logger): Unit = {
    val buildOptions = buildOptionsOrExit(options)
    val inputs       = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val bspPath = writeBspConfiguration(
      options,
      inputs,
      buildOptions,
      previousCommandName = None,
      args = args.all
    ).orExit(logger)

    bspPath.foreach(path => println(s"Wrote configuration file for ide in: $path"))
  }

  def runSafe(
    options: SharedOptions,
    inputs: Inputs,
    logger: Logger,
    buildOptions: BuildOptions,
    previousCommandName: Option[String],
    args: Seq[String]
  ): Unit =
    writeBspConfiguration(
      SetupIdeOptions(shared = options),
      inputs,
      buildOptions,
      previousCommandName,
      args
    ) match {
      case Left(ex) =>
        logger.debug(s"Ignoring error during setup-ide: ${ex.message}")
      case Right(_) =>
    }

  override def sharedOptions(options: SetupIdeOptions): Option[SharedOptions] = Some(options.shared)

  private def writeBspConfiguration(
    options: SetupIdeOptions,
    inputs: Inputs,
    buildOptions: BuildOptions,
    previousCommandName: Option[String],
    args: Seq[String]
  ): Either[BuildException, Option[os.Path]] = either {

    val virtualInputs = inputs.elements.collect {
      case v: Virtual => v
    }
    if (virtualInputs.nonEmpty)
      value(Left(new FoundVirtualInputsError(virtualInputs)))

    val progName = argvOpt.flatMap(_.headOption).getOrElse {
      sys.error("setup-ide called in a non-standard way :|")
    }

    val logger = options.shared.logger

    if (buildOptions.classPathOptions.allExtraDependencies.toSeq.nonEmpty)
      value(downloadDeps(
        inputs,
        buildOptions,
        logger
      ))

    val (bspName, bspJsonDestination) = bspDetails(inputs.workspace, options.bspFile)
    val scalaCliBspJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-options-v2.json"
    val scalaCliBspLauncherOptsJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-launcher-options.json"
    val scalaCliBspInputsJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-inputs.json"
    val scalaCliBspEnvsJsonDestination =
      inputs.workspace / Constants.workspaceDirName / "ide-envs.json"

    val inputArgs = inputs.elements.collect { case d: OnDisk => d.path.toString }

    val ideInputs = IdeInputs(
      options.shared.validateInputArgs(args)
        .flatMap(_.toOption)
        .flatten
        .collect { case d: OnDisk => d.path.toString }
    )

    val debugOpt = options.shared.jvm.bspDebugPort.toSeq.map(port =>
      s"-J-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:$port,suspend=y"
    )

    val launcher = launcherOptions.scalaRunner.initialLauncherPath
      .getOrElse(CommandUtils.getAbsolutePathToScalaCli(progName))
    val finalLauncherOptions = launcherOptions.copy(cliVersion =
      launcherOptions.cliVersion.orElse(launcherOptions.scalaRunner.predefinedCliVersion)
    )
    val bspArgs =
      List(launcher) ++
        finalLauncherOptions.toCliArgs ++
        launcherJavaPropArgs ++
        List("bsp") ++
        debugOpt ++
        List("--json-options", scalaCliBspJsonDestination.toString) ++
        List("--json-launcher-options", scalaCliBspLauncherOptsJsonDestination.toString) ++
        List("--envs-file", scalaCliBspEnvsJsonDestination.toString) ++
        inputArgs
    val details = new BspConnectionDetails(
      bspName,
      bspArgs.asJava,
      Constants.version,
      bloop.rifle.internal.BuildInfo.bspVersion,
      List("scala", "java").asJava
    )

    val charset =
      options.charset
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(Charset.forName)
        .getOrElse(StandardCharsets.UTF_8)

    val gson = new GsonBuilder().setPrettyPrinting().create()

    implicit val mapCodec: JsonValueCodec[Map[String, String]] = JsonCodecMaker.make

    val json                         = gson.toJson(details)
    val scalaCliOptionsForBspJson    = writeToArray(options.shared)(SharedOptions.jsonCodec)
    val scalaCliLaunchOptsForBspJson = writeToArray(finalLauncherOptions)(LauncherOptions.jsonCodec)
    val scalaCliBspInputsJson        = writeToArray(ideInputs)
    val envsForBsp          = sys.env.filter((key, _) => EnvVar.allBsp.map(_.name).contains(key))
    val scalaCliBspEnvsJson = writeToArray(envsForBsp)

    if (inputs.workspaceOrigin.contains(WorkspaceOrigin.HomeDir))
      value(Left(new WorkspaceError(
        s"""$baseRunnerName can not determine where to write its BSP configuration.
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
        scalaCliBspLauncherOptsJsonDestination,
        scalaCliLaunchOptsForBspJson,
        createFolders = true
      )
      os.write.over(
        scalaCliBspInputsJsonDestination,
        scalaCliBspInputsJson,
        createFolders = true
      )
      os.write.over(
        scalaCliBspEnvsJsonDestination,
        scalaCliBspEnvsJson,
        createFolders = true
      )
      logger.debug(s"Wrote $bspJsonDestination")
      Some(bspJsonDestination)
    }
    else
      None
  }

  def bspDetails(workspace: os.Path, ops: SharedBspFileOptions): (String, os.Path) = {
    import ops.*
    val dir = bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(workspace / ".bsp")
    val bspName0 = bspName.map(_.trim).filter(_.nonEmpty).getOrElse(baseRunnerName)

    (bspName0, dir / s"$bspName0.json")
  }
}
