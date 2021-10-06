package scala.cli.commands

import caseapp._
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.GsonBuilder

import java.io.File
import java.nio.charset.Charset

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Artifacts, CrossSources, Inputs, Logger, Os, Sources}
import scala.collection.JavaConverters._

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

  def run(options: SetupIdeOptions, args: RemainingArgs): Unit = {

    val rawArgv = argvOpt.getOrElse {
      System.err.println("setup-ide called in a non-standard way :|")
      sys.exit(1)
    }

    def inputs = options.shared.inputsOrExit(args)
    val logger = options.shared.logger
    if (options.buildOptions.classPathOptions.extraDependencies.nonEmpty)
      downloadDeps(inputs, options.buildOptions, logger).orExit(logger)

    val argv = {
      val commandIndex = rawArgv.indexOf("setup-ide")
      val withBspCommand =
        if (commandIndex < 0) rawArgv // shouldn't happen
        else rawArgv.take(commandIndex) ++ Array("bsp") ++ rawArgv.drop(commandIndex + 1)

      // Ensure the path to the CLI is absolute
      val progName = rawArgv(0)
      if (progName.contains(File.pathSeparator)) {
        val absoluteProgPath = os.FilePath(progName).resolveFrom(Os.pwd).toString
        absoluteProgPath +: withBspCommand.drop(1)
      }
      else withBspCommand
    }

    val name = options.bspName.map(_.trim).filter(_.nonEmpty).getOrElse("scala-cli")

    val details = new BspConnectionDetails(
      name,
      argv.toList.asJava,
      Constants.version,
      scala.build.blooprifle.internal.Constants.bspVersion,
      List("scala", "java").asJava
    )

    val gson = new GsonBuilder().setPrettyPrinting().create()

    val json = gson.toJson(details)

    val dir = options.bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(inputs.workspace / ".bsp")

    val dest = dir / s"$name.json"

    val charset = options.charset
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(Charset.forName)
      .getOrElse(Charset.defaultCharset()) // Should it be UTF-8?

    os.write.over(dest, json.getBytes(charset), createFolders = true)

    if (options.shared.logging.verbosity >= 0)
      System.err.println(s"Wrote $dest")
  }
}
