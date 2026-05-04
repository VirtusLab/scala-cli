package scala.cli.commands.listtargets

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, WriterConfig, writeToStream}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import scala.build.*
import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.build.options.BuildOptions
import scala.cli.CurrentParams
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, SpecificationLevel}

object ListTargets extends ScalaCommand[ListTargetsOptions] {
  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def names: List[List[String]] = List(
    List("list-targets")
  )
  override def sharedOptions(options: ListTargetsOptions): Option[SharedOptions] =
    Some(options.shared)

  private final case class TargetEntry(
    platform: String,
    scalaVersion: Option[String]
  )

  private given JsonValueCodec[List[TargetEntry]] = JsonCodecMaker.make

  private def loadCrossSources(
    inputs: Inputs,
    buildOptions: BuildOptions,
    logger: Logger
  ): Either[BuildException, CrossSources] =
    CrossSources.forInputs(
      inputs,
      Sources.defaultPreprocessors(
        buildOptions.archiveCache,
        buildOptions.internal.javaClassNameVersionOpt,
        () => buildOptions.javaHome().value.javaCommand
      ),
      logger,
      buildOptions.suppressWarningOptions,
      buildOptions.internal.exclude,
      download = buildOptions.downloader
    ).map(_._1)

  private def targetOf(options: BuildOptions): TargetEntry = {
    val platform = options.platform.value.repr
    val sv = options.scalaParams.toOption.flatten.map(_.scalaVersion)
      .orElse(options.scalaOptions.scalaVersion.flatMap(_.versionOpt))
      .orElse(options.scalaOptions.defaultScalaVersion)
    TargetEntry(platform, sv)
  }

  override def runCommand(
    options: ListTargetsOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)
    val inputs              = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val crossSources    = loadCrossSources(inputs, initialBuildOptions, logger).orExit(logger)
    val resolvedOptions = crossSources.sharedOptions(initialBuildOptions)

    val targets = (resolvedOptions +: resolvedOptions.crossOptions).map(targetOf).distinct.toList

    writeToStream(targets, System.out, WriterConfig.withIndentionStep(1))
    System.out.println()
  }
}
