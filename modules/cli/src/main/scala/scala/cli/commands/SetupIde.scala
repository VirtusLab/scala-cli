package scala.cli.commands

import caseapp._
import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.GsonBuilder
import upickle.default._

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Artifacts, CrossSources, Inputs, Logger, Os, Sources}
import scala.jdk.CollectionConverters._
import scala.util.Try

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
    doRun(
      options,
      args,
      inputs = options.shared.inputsOrExit(args),
      previousCommandName = None
    ).orExit(options.shared.logging.logger)

  def runSafe(
    options: SharedOptions,
    args: RemainingArgs,
    inputs: Inputs,
    logger: Logger,
    previousCommandName: Option[String]
  ): Unit =
    doRun(SetupIdeOptions(shared = options), args, inputs, previousCommandName) match {
      case Left(ex) =>
        logger.debug(s"Caught $ex during setup-ide, ignoring it.")
      case Right(()) =>
    }

  def doRun(
    options: SetupIdeOptions,
    args: RemainingArgs,
    inputs: Inputs,
    previousCommandName: Option[String]
  ): Either[BuildException, Unit] = either {

    val rawArgv = argvOpt.getOrElse {
      sys.error("setup-ide called in a non-standard way :|")
    }

    val logger = options.shared.logger

    if (options.buildOptions.classPathOptions.extraDependencies.nonEmpty)
      value(downloadDeps(inputs, options.buildOptions, logger))

    val dir = options.bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(inputs.workspace / ".bsp")

    val bspName            = options.bspName.map(_.trim).filter(_.nonEmpty).getOrElse("scala-cli")
    val bspJsonDestination = dir / s"$bspName.json"
    val scalaCliBspJsonDestination = inputs.workspace / ".scala" / "ide-options.json"

    // Ensure the path to the CLI is absolute
    val absolutePathToScalaCli: String = {
      if (rawArgv(0).contains(File.separator))
        os.Path(rawArgv(0), Os.pwd).toString
      else
        /*
          In order to get absolute path we first try to get it from coursier.mainJar (this works for standalone launcher)
          If this fails we fallback to getting it from this class and finally we may also use rawArg if there is nothing left
         */
        sys.props.get("coursier.mainJar")
          .map(Paths.get(_).toAbsolutePath.toString)
          .orElse {
            Try(
              //This is weird but on windows we get /D:\a\scala-cli...
              Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
                .toAbsolutePath
                .toString
            ).toOption
          }
          .getOrElse(rawArgv(0))
    }

    val remainingArgs = args.remaining.map(arg => os.Path(arg, Os.pwd).toString)
    val unparsedArgs =
      if (args.unparsed.isEmpty) Nil
      else "--" +: args.unparsed.map(arg => os.Path(arg, Os.pwd).toString)

    val bspArgs =
      List(absolutePathToScalaCli, "bsp") ++
        remainingArgs ++
        List("--json-options", scalaCliBspJsonDestination.toString) ++
        unparsedArgs
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

    if (previousCommandName.isEmpty || !bspJsonDestination.toIO.exists()) {
      os.write.over(bspJsonDestination, json.getBytes(charset), createFolders = true)
      os.write.over(
        scalaCliBspJsonDestination,
        scalaCliOptionsForBspJson.getBytes(charset),
        createFolders = true
      )
      logger.debug(s"Wrote $bspJsonDestination")
    }
  }
}
