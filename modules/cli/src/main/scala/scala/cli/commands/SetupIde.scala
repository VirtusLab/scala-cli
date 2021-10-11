package scala.cli.commands

import caseapp._
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.GsonBuilder
import upickle.default._

import java.io.File
import java.nio.charset.Charset

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Artifacts, CrossSources, Inputs, Logger, Os, Sources}
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
          Sources.defaultPreprocessors(CustomCodeWrapper)
        )
      }

      value(crossSources.scopedSources(options))
        .sources(Scope.Main, options)
        .buildOptions
    }

    val joinedBuildOpts = maybeSourceBuildOptions.toOption.map(options orElse _).getOrElse(options)
    joinedBuildOpts.artifacts(logger)
  }

  def run(options: SetupIdeOptions, args: RemainingArgs): Unit =
    run(options, args, previousCommandName = None)

  def run(
    options: SetupIdeOptions,
    args: RemainingArgs,
    previousCommandName: Option[String]
  ): Unit = {

    val rawArgv = argvOpt.getOrElse {
      System.err.println("setup-ide called in a non-standard way :|")
      sys.exit(1)
    }

    def inputs = options.shared.inputsOrExit(args)
    val logger = options.shared.logger
    if (options.buildOptions.classPathOptions.extraDependencies.nonEmpty)
      downloadDeps(inputs, options.buildOptions, logger).orExit(logger)

    val dir = options.bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(inputs.workspace / ".bsp")

    val bspName            = options.bspName.map(_.trim).filter(_.nonEmpty).getOrElse("scala-cli")
    val bspJsonDestination = dir / s"$bspName.json"
    val scalaCliBspJsonDestination = inputs.workspace / ".scala" / "scala-cli-bsp.json"

    // Ensure the path to the CLI is absolute
    val progName =
      if (rawArgv(0).contains(File.separator))
        os.FilePath(rawArgv(0)).resolveFrom(Os.pwd).toString
      else rawArgv(0)

    val details = new BspConnectionDetails(
      bspName,
      List(
        progName,
        "bsp",
        inputs.workspace.toNIO.toAbsolutePath.toString(),
        "--json-options",
        scalaCliBspJsonDestination.toString
      ).asJava,
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

    if (!previousCommandName.isDefined || !bspJsonDestination.toIO.exists()) {
      os.write.over(bspJsonDestination, json.getBytes(charset), createFolders = true)
      os.write.over(
        scalaCliBspJsonDestination,
        scalaCliOptionsForBspJson.getBytes(charset),
        createFolders = true
      )
      if (options.shared.logging.verbosity >= 0)
        options.shared.logger.debug(s"Wrote $bspJsonDestination")
    }
  }
}
