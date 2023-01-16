package scala.cli.commands.bsp

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.bsp.{BspReloadableOptions, BspThreads}
import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.build.internal.CustomCodeWrapper
import scala.build.options.BuildOptions
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
    options.jsonOptions.map { optionsPath =>
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

    val argsToInputs: Seq[String] => Either[BuildException, Inputs] =
      argsSeq =>
        either {
          val sharedOptions = getSharedOptions()
          val initialInputs = value(sharedOptions.inputs(argsSeq, () => Inputs.default()))

          if (sharedOptions.logging.verbosity >= 3)
            pprint.err.log(initialInputs)

          val buildOptions0    = buildOptions(sharedOptions)
          val latestLogger     = sharedOptions.logging.logger
          val persistentLogger = new PersistentDiagnosticLogger(latestLogger)

          val allInputs =
            CrossSources.forInputs(
              initialInputs,
              Sources.defaultPreprocessors(
                buildOptions0.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
                buildOptions0.archiveCache,
                buildOptions0.internal.javaClassNameVersionOpt,
                () => buildOptions0.javaHome().value.javaCommand
              ),
              persistentLogger,
              sharedOptions.suppress.suppressDirectivesInMultipleFilesWarning
            ).map(_._2).getOrElse(initialInputs)

          Build.updateInputs(allInputs, buildOptions(sharedOptions))
        }

    val bspReloadableOptionsReference = BspReloadableOptions.Reference { () =>
      val sharedOptions = getSharedOptions()
      BspReloadableOptions(
        buildOptions = buildOptions(sharedOptions),
        bloopRifleConfig = sharedOptions.bloopRifleConfig().orExit(sharedOptions.logger),
        logger = sharedOptions.logging.logger,
        verbosity = sharedOptions.logging.verbosity
      )
    }

    val inputs = argsToInputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val configDb = options.shared.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions

    BspThreads.withThreads { threads =>
      val bsp = scala.build.bsp.Bsp.create(
        argsToInputs,
        bspReloadableOptionsReference,
        threads,
        System.in,
        System.out,
        actionableDiagnostics
      )

      try {
        val doneFuture = bsp.run(inputs)
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
        generateSemanticDbs = baseOptions.scalaOptions.generateSemanticDbs.orElse(Some(true))
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        addRunnerDependencyOpt =
          baseOptions.notForBloopOptions.addRunnerDependencyOpt.orElse(Some(false))
      )
    )
  }
}
