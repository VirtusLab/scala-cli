package scala.cli.commands.bsp

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

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
  override def sharedOptions(options: BspOptions): Option[SharedOptions] =
    Option(latestSharedOptions(options))

  // not reusing buildOptions here, since they should be reloaded live instead
  override def runCommand(options: BspOptions, args: RemainingArgs, logger: Logger): Unit = {
    if (options.shared.logging.verbosity >= 3)
      pprint.err.log(args)

    val getSharedOptions: () => SharedOptions = () => latestSharedOptions(options)

    val preprocessInputs: Seq[String] => Either[BuildException, (Inputs, BuildOptions)] =
      argsSeq =>
        either {
          val sharedOptions = getSharedOptions()
          val initialInputs = value(sharedOptions.inputs(argsSeq, () => Inputs.default()))

          if (sharedOptions.logging.verbosity >= 3)
            pprint.err.log(initialInputs)

          val baseOptions      = buildOptions(sharedOptions)
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
      val sharedOptions = getSharedOptions()
      BspReloadableOptions(
        buildOptions = buildOptions(sharedOptions) orElse finalBuildOptions,
        bloopRifleConfig = sharedOptions.bloopRifleConfig(Some(finalBuildOptions))
          .orExit(sharedOptions.logger),
        logger = sharedOptions.logging.logger,
        verbosity = sharedOptions.logging.verbosity
      )
    }

    val bspReloadableOptionsReference = BspReloadableOptions.Reference { () =>
      val sharedOptions = getSharedOptions()
      val bloopRifleConfig = sharedOptions.bloopRifleConfig(Some(finalBuildOptions))
        .orExit(sharedOptions.logger)

      BspReloadableOptions(
        buildOptions = buildOptions(sharedOptions),
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

  private def buildOptions(sharedOptions: SharedOptions): BuildOptions = {
    val logger      = sharedOptions.logger
    val baseOptions = sharedOptions.buildOptions().orExit(logger)
    baseOptions.copy(
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
  }
}
