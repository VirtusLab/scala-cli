package scala.cli.commands.bsp

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.bsp.{BspReloadableOptions, BspThreads}
import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.build.options.{BuildOptions, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.shared.SharedOptions
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.launcher.LauncherOptions
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

  // not reusing buildOptions here, since they should be reloaded live instead
  override def runCommand(options: BspOptions, args: RemainingArgs, logger: Logger): Unit = {
    if (options.shared.logging.verbosity >= 3)
      pprint.err.log(args)

    val getSharedOptions: () => SharedOptions      = () => latestSharedOptions(options)
    val getLauncherOptions: () => LauncherOptions  = () => latestLauncherOptions(options)
    val getEnvsFromFile: () => Map[String, String] = () => latestEnvsFromFile(options)

    val preprocessInputs: Seq[String] => Either[BuildException, (Inputs, BuildOptions)] =
      argsSeq =>
        either {
          val sharedOptions   = getSharedOptions()
          val launcherOptions = getLauncherOptions()
          val envs            = getEnvsFromFile()
          val initialInputs   = value(sharedOptions.inputs(argsSeq, () => Inputs.default()))

          if (sharedOptions.logging.verbosity >= 3)
            pprint.err.log(initialInputs)

          val baseOptions      = buildOptions(sharedOptions, launcherOptions, envs)
          val latestLogger     = sharedOptions.logging.logger
          val persistentLogger = new PersistentDiagnosticLogger(latestLogger)

          val crossResult = CrossSources.forInputs(
            initialInputs,
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
                scopedSources.buildOptionsFor(Scope.Main).foldRight(sharedBuildOptions)(_ orElse _)
            yield (crossInputs, resolvedBuildOptions)
          }.getOrElse(initialInputs -> baseOptions)

          Build.updateInputs(allInputs, baseOptions) -> finalBuildOptions
        }

    val (inputs, finalBuildOptions) = preprocessInputs(args.all).orExit(logger)

    /** values used for lauching the bsp, especially for launching a bloop server, they include
      * options extracted from sources
      */
    val initialBspOptions = {
      val sharedOptions   = getSharedOptions()
      val launcherOptions = getLauncherOptions()
      val envs            = getEnvsFromFile()
      val bspBuildOptions = buildOptions(sharedOptions, launcherOptions, envs)
        .orElse(finalBuildOptions)
      BspReloadableOptions(
        buildOptions = bspBuildOptions,
        bloopRifleConfig = sharedOptions.bloopRifleConfig(Some(bspBuildOptions))
          .orExit(sharedOptions.logger),
        logger = sharedOptions.logging.logger,
        verbosity = sharedOptions.logging.verbosity
      )
    }

    val bspReloadableOptionsReference = BspReloadableOptions.Reference { () =>
      val sharedOptions   = getSharedOptions()
      val launcherOptions = getLauncherOptions()
      val envs            = getEnvsFromFile()
      val bloopRifleConfig = sharedOptions.bloopRifleConfig(Some(finalBuildOptions))
        .orExit(sharedOptions.logger)

      BspReloadableOptions(
        buildOptions = buildOptions(sharedOptions, launcherOptions, envs),
        bloopRifleConfig = sharedOptions.bloopRifleConfig().orExit(sharedOptions.logger),
        logger = sharedOptions.logging.logger,
        verbosity = sharedOptions.logging.verbosity
      )
    }

    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions

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
    val withEnvs = envs.get("JAVA_HOME")
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
          (withEnvs.classPathOptions.extraRepositories ++ launcherOptions.cliPredefinedRepository).distinct
      ),
      scalaOptions = withEnvs.scalaOptions.copy(
        defaultScalaVersion = launcherOptions.cliUserScalaVersion
      )
    )
    withLauncherOptions
  }
}
