package scala.cli.commands.setupide

import caseapp.*
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.google.gson.GsonBuilder

import java.nio.charset.{Charset, StandardCharsets}

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.bsp.IdeInputs
import scala.build.errors.{BuildException, WorkspaceError}
import scala.build.input.compose.{ComposedInputs, InputsComposer, SimpleInputs}
import scala.build.input.{ModuleInputs, OnDisk, Virtual, WorkspaceOrigin, compose}
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
    inputs: ModuleInputs,
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Artifacts] = {

    // ignoring errors related to sources themselves
    val maybeSourceBuildOptions = either {
      val (crossSources, allInputs) = value {
        CrossSources.forModuleInputs(
          inputs,
          Sources.defaultPreprocessors(
            options.archiveCache,
            options.internal.javaClassNameVersionOpt,
            () => options.javaHome().value.javaCommand
          ),
          logger,
          options.suppressWarningOptions,
          options.internal.exclude
        )
      }

      val sharedOptions = crossSources.sharedOptions(options)

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
    val inputs       = options.shared.composeInputs(args.all).orExit(logger)
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
    inputs: ModuleInputs,
    logger: Logger,
    buildOptions: BuildOptions,
    previousCommandName: Option[String],
    args: Seq[String]
  ): Unit =
    writeBspConfiguration(
      SetupIdeOptions(shared = options),
      SimpleInputs(inputs),
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
    inputs: compose.Inputs,
    buildOptions: BuildOptions,
    previousCommandName: Option[String],
    args: Seq[String]
  ): Either[BuildException, Option[os.Path]] = either {

    val virtualInputs = inputs.modules.flatMap(_.elements).collect {
      case v: Virtual => v
    }
    if (virtualInputs.nonEmpty)
      value(Left(new FoundVirtualInputsError(virtualInputs)))

    val progName = argvOpt.flatMap(_.headOption).getOrElse {
      sys.error("setup-ide called in a non-standard way :|")
    }

    val logger = options.shared.logger

    if (buildOptions.classPathOptions.allExtraDependencies.toSeq.nonEmpty)
      for (module <- inputs.modules) do value(downloadDeps(module, buildOptions, logger))

    val workspace = inputs.workspace

    val (bspName, bspJsonDestination) = bspDetails(workspace, options.bspFile)
    val scalaCliBspJsonDestination = workspace / Constants.workspaceDirName / "ide-options-v2.json"
    val scalaCliBspLauncherOptsJsonDestination =
      workspace / Constants.workspaceDirName / "ide-launcher-options.json"
    val scalaCliBspInputsJsonDestination =
      workspace / Constants.workspaceDirName / "ide-inputs.json"
    val scalaCliBspEnvsJsonDestination = workspace / Constants.workspaceDirName / "ide-envs.json"

    // FIXME single modules can also be defined with module config toml file
    val inputArgs = inputs match
      case ComposedInputs(modules, targetModule, workspace) =>
        InputsComposer.findModuleConfig(args, Os.pwd)
          .orExit(logger)
          .fold(args)(p => Seq(p.toString))
      case SimpleInputs(singleModule) => singleModule.elements
          .collect { case d: OnDisk => d.path.toString }

    val ideInputs = IdeInputs(
      options.shared.validateInputArgs(args)
        .flatMap(_.toOption)
        .flatten
        .collect { case d: OnDisk => d.path.toString }
    )

    val debugOpt = options.shared.jvm.bspDebugPort.toSeq.map(port =>
      s"-J-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:$port,suspend=y"
    )

    val bspArgs =
      List(CommandUtils.getAbsolutePathToScalaCli(progName)) ++
        launcherOptions.toCliArgs ++
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
    val scalaCliLaunchOptsForBspJson = writeToArray(launcherOptions)(LauncherOptions.jsonCodec)
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
