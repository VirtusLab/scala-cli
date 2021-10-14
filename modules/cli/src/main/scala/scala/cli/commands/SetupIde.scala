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
    runSafe(
      options,
      args,
      inputs = options.shared.inputsOrExit(args),
      previousCommandName = None
    ) match {
      case Left(error: BuildException) =>
        options.shared.logger.exit(error)
      case Left(error: Exception) =>
        options.shared.logger.message(error.getMessage)
        sys.exit(1)
      case Right(_) => ()
    }

  def runSafe(
    options: SetupIdeOptions,
    args: RemainingArgs,
    inputs: Inputs,
    previousCommandName: Option[String]
  ): Either[Exception, Unit] = {
    val rawArgs = argvOpt match {
      case Some(value) => Right(value)
      case None        => Left(new RuntimeException("setup-ide called in a non-standard way :|"))
    }

    val logger = options.shared.logger

    def downloadDepsIfNeeded(): Either[BuildException, Unit] =
      options.buildOptions.classPathOptions.extraDependencies.length match {
        case 0 => Right()
        case _ => downloadDeps(inputs, options.buildOptions, logger).map(_ => ())
      }

    downloadDepsIfNeeded().flatMap(_ => rawArgs).map { rawArgv =>
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
          sys.props.get("coursier.mainJar").map(Paths.get(_)) match {
            case Some(value) => value.toAbsolutePath.toString
            case None =>
              Try(
                //This is weird but on windows we get /D:\a\scala-cli...
                Paths.get(
                  this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI
                ).toAbsolutePath.toString
              ).getOrElse(rawArgv(0))
          }
      }

      val unparsedArgs =
        if (args.unparsed.isEmpty)
          args.unparsed
        else "--" +: args.unparsed
      val remainingArgs = args.remaining.map { arg =>
        Try(os.Path(arg, Os.pwd)).filter(_.toIO.exists()).map(_.toString()).getOrElse(arg)
      }

      val details = new BspConnectionDetails(
        bspName,
        (List(absolutePathToScalaCli, "bsp") ++ remainingArgs ++ List(
          "--json-options",
          scalaCliBspJsonDestination.toString
        ) ++ unparsedArgs).asJava,
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
}
