package scala.cli.commands.bsp

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.bsp.{BspReloadableOptions, BspThreads}
import scala.build.errors.BuildException
import scala.build.input.{ModuleInputs, compose}
import scala.build.internals.EnvVar
import scala.build.options.{BuildOptions, Scope}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.shared.SharedOptions
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.launcher.LauncherOptions
import scala.cli.util.ConfigDbUtils
import scala.cli.{CurrentParams, ScalaCli}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Bsp extends ScalaCommand[BspOptions] {
  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION
  private def latestSharedOptions(options: BspOptions): SharedOptions =
    options.jsonOptions
      .map(path => os.Path(path, os.pwd))
      .filter(path => os.exists(path) && os.isFile(path))
      .map { optionsPath =>
        val content = os.read.bytes(os.Path(optionsPath, os.pwd))
        readFromArray(content)(SharedOptions.jsonCodec)
      }.getOrElse(options.shared)

  private def latestLauncherOptions(options: BspOptions): LauncherOptions =
    options.jsonLauncherOptions
      .map(path => os.Path(path, os.pwd))
      .filter(path => os.exists(path) && os.isFile(path))
      .map { optionsPath =>
        val content = os.read.bytes(os.Path(optionsPath, os.pwd))
        readFromArray(content)(LauncherOptions.jsonCodec)
      }.getOrElse(launcherOptions)
  private def latestEnvsFromFile(options: BspOptions): Map[String, String] =
    options.envs
      .map(path => os.Path(path, os.pwd))
      .filter(path => os.exists(path) && os.isFile(path))
      .map { envsPath =>
        val content = os.read.bytes(os.Path(envsPath, os.pwd))
        implicit val mapCodec: JsonValueCodec[Map[String, String]] = JsonCodecMaker.make
        readFromArray(content)
      }
      .getOrElse(Map.empty)
  override def sharedOptions(options: BspOptions): Option[SharedOptions] =
    Option(latestSharedOptions(options))

  private def refreshPowerMode(
    latestLauncherOptions: LauncherOptions,
    latestSharedOptions: SharedOptions,
    latestEnvs: Map[String, String]
  ): Unit = {
    val previousPowerMode = ScalaCli.allowRestrictedFeatures
    val configPowerMode = ConfigDbUtils.getLatestConfigDbOpt(latestSharedOptions.logger)
      .flatMap(_.get(Keys.power).toOption)
      .flatten
      .getOrElse(false)
    val envPowerMode       = latestEnvs.get(EnvVar.ScalaCli.power.name).exists(_.toBoolean)
    val launcherPowerArg   = latestLauncherOptions.powerOptions.power
    val subCommandPowerArg = latestSharedOptions.powerOptions.power
    val latestPowerMode = configPowerMode || launcherPowerArg || subCommandPowerArg || envPowerMode
    // only set power mode if it's been turned on since, never turn it off in BSP
    if !previousPowerMode && latestPowerMode then ScalaCli.setPowerMode(latestPowerMode)
  }

  // not reusing buildOptions here, since they should be reloaded live instead
  override def runCommand(options: BspOptions, args: RemainingArgs, logger: Logger): Unit = {
    if (options.shared.logging.verbosity >= 3)
      pprint.err.log(args)

    val getSharedOptions: () => SharedOptions      = () => latestSharedOptions(options)
    val getLauncherOptions: () => LauncherOptions  = () => latestLauncherOptions(options)
    val getEnvsFromFile: () => Map[String, String] = () => latestEnvsFromFile(options)

    refreshPowerMode(getLauncherOptions(), getSharedOptions(), getEnvsFromFile())

    val preprocessInputs
      : Seq[String] => Either[BuildException, (compose.Inputs, Seq[BuildOptions])] =
      argsSeq =>
        either {
          val sharedOptions   = getSharedOptions()
          val launcherOptions = getLauncherOptions()
          val envs            = getEnvsFromFile()

          refreshPowerMode(launcherOptions, sharedOptions, envs)

          val baseOptions      = buildOptions(sharedOptions, launcherOptions, envs)
          val latestLogger     = sharedOptions.logging.logger
          val persistentLogger = new PersistentDiagnosticLogger(latestLogger)

          val initialInputs: compose.Inputs = value(sharedOptions.composeInputs(argsSeq))

          if (sharedOptions.logging.verbosity >= 3)
            pprint.err.log(initialInputs)

          initialInputs.preprocessInputs { moduleInputs =>
            val crossResult = CrossSources.forModuleInputs(
              moduleInputs,
              Sources.defaultPreprocessors(
                baseOptions.archiveCache,
                baseOptions.internal.javaClassNameVersionOpt,
                () => baseOptions.javaHome().value.javaCommand
              ),
              persistentLogger,
              baseOptions.suppressWarningOptions,
              baseOptions.internal.exclude
            )

            val (allInputs, finalBuildOptions) = {
              for
                crossSourcesAndInputs <- crossResult
                // compiler bug, can't do :
                // (crossSources, crossInputs) <- crossResult
                (crossSources, crossInputs) = crossSourcesAndInputs
                sharedBuildOptions          = crossSources.sharedOptions(baseOptions)
                scopedSources <- crossSources.scopedSources(sharedBuildOptions)
                resolvedBuildOptions =
                  scopedSources.buildOptionsFor(Scope.Main).foldRight(sharedBuildOptions)(
                    _ orElse _
                  )
              yield (crossInputs, resolvedBuildOptions)
            }.getOrElse(moduleInputs -> baseOptions)

            allInputs -> finalBuildOptions
          }
        }

    val inputsAndBuildOptions = preprocessInputs(args.all).orExit(logger)

    // We use this sequence of options to pick a suitable version of the JVM for Bloop
    val allModulesBuildOptions = inputsAndBuildOptions._2
    val inputs                 = inputsAndBuildOptions._1

    if (options.shared.logging.verbosity >= 3)
      pprint.err.log(allModulesBuildOptions)

    val bspReloadableOptionsReference = BspReloadableOptions.Reference { () =>
      val sharedOptions   = getSharedOptions()
      val launcherOptions = getLauncherOptions()
      val envs            = getEnvsFromFile()

      refreshPowerMode(launcherOptions, sharedOptions, envs)

      BspReloadableOptions(
        buildOptions = buildOptions(sharedOptions, launcherOptions, envs),
        bloopRifleConfig = sharedOptions.bloopRifleConfig().orExit(sharedOptions.logger),
        logger = sharedOptions.logging.logger,
        verbosity = sharedOptions.logging.verbosity
      )
    }

    /** values used for launching the bsp, especially for launching the bloop server, they do not
      * include options extracted from sources, except in bloopRifleConfig - it's needed for
      * correctly launching the bloop server
      */
    val initialBspOptions = {
      val sharedOptions = getSharedOptions()

      bspReloadableOptionsReference.get.copy(
        bloopRifleConfig =
          sharedOptions.bloopRifleConfig(allModulesBuildOptions).orExit(sharedOptions.logger)
      )
    }

    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val actionableDiagnostics = options.shared.logging.verbosityOptions.actions

    BspThreads.withThreads { threads =>
      val bsp = scala.build.bsp.Bsp.create(
        preprocessInputs.andThen(_.map(_._1)),
        bspReloadableOptionsReference,
        threads,
        System.in,
        System.out,
        actionableDiagnostics
      )

      try {
        val doneFuture = bsp.run(inputs, initialBspOptions)
        Await.result(doneFuture, Duration.Inf)
      }
      finally bsp.shutdown()
    }
  }

  private def buildOptions(
    sharedOptions: SharedOptions,
    launcherOptions: LauncherOptions,
    envs: Map[String, String]
  ): BuildOptions = {
    val logger      = sharedOptions.logger
    val baseOptions = sharedOptions.buildOptions().orExit(logger)
    val withDefaults = baseOptions.copy(
      classPathOptions = baseOptions.classPathOptions.copy(
        fetchSources = baseOptions.classPathOptions.fetchSources.orElse(Some(true))
      ),
      scalaOptions = baseOptions.scalaOptions.copy(
        semanticDbOptions = baseOptions.scalaOptions.semanticDbOptions.copy(
          generateSemanticDbs =
            baseOptions.scalaOptions.semanticDbOptions.generateSemanticDbs.orElse(Some(true))
        )
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        addRunnerDependencyOpt =
          baseOptions.notForBloopOptions.addRunnerDependencyOpt.orElse(Some(false))
      )
    )
    val withEnvs = envs.get(EnvVar.Java.javaHome.name)
      .filter(_ => withDefaults.javaOptions.javaHomeOpt.isEmpty)
      .map(javaHome =>
        withDefaults.copy(javaOptions =
          withDefaults.javaOptions.copy(javaHomeOpt =
            Some(Positioned(
              Seq(Position.Custom("ide.env.JAVA_HOME")),
              os.Path(javaHome, Os.pwd)
            ))
          )
        )
      )
      .getOrElse(withDefaults)
    val withLauncherOptions = withEnvs.copy(
      classPathOptions = withEnvs.classPathOptions.copy(
        extraRepositories =
          (withEnvs.classPathOptions.extraRepositories ++ launcherOptions.scalaRunner
            .cliPredefinedRepository).distinct
      ),
      scalaOptions = withEnvs.scalaOptions.copy(
        defaultScalaVersion = launcherOptions.scalaRunner.cliUserScalaVersion
      )
    )
    withLauncherOptions
  }
}
