package scala.cli.commands

import caseapp._
import dependency._

import java.io.File
import scala.build.EitherCps.{either, value}
import scala.build._
import scala.build.compiler.{ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.errors.ScaladocGenerationFailedError
import scala.util.Properties

object Doc extends ScalaCommand[DocOptions] {
  override def group                              = "Main"
  override def sharedOptions(options: DocOptions) = Some(options.shared)
  def run(options: DocOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args.remaining)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = options.shared.buildOptions(enableJmh = false, jmhVersion = None)
    val logger              = options.shared.logger
    val threads             = BuildThreads.create()

    val maker               = options.shared.compilerMaker(threads)
    val compilerMaker       = ScalaCompilerMaker.IgnoreScala2(maker)
    val docCompilerMakerOpt = Some(SimpleScalaCompilerMaker("java", Nil, scaladoc = true))

    val builds =
      Build.build(
        inputs,
        initialBuildOptions,
        compilerMaker,
        docCompilerMakerOpt,
        logger,
        crossBuilds = false,
        buildTests = false,
        partial = None
      )
        .orExit(logger)
    builds.main match {
      case s: Build.Successful =>
        val res0 = doDoc(
          logger,
          options.output.filter(_.nonEmpty),
          options.force,
          s,
          args.unparsed
        )
        res0.orExit(logger)
      case _: Build.Failed =>
        System.err.println("Compilation failed")
        sys.exit(1)
      case _: Build.Cancelled =>
        System.err.println("Build cancelled")
        sys.exit(1)
    }
  }

  private def doDoc(
    logger: Logger,
    outputOpt: Option[String],
    force: Boolean,
    build: Build.Successful,
    extraArgs: Seq[String]
  ): Either[BuildException, Unit] = either {

    def defaultName = "scala-doc"

    val dest          = outputOpt.getOrElse(defaultName)
    val destPath      = os.Path(dest, Os.pwd)
    val printableDest = CommandUtils.printablePath(destPath)

    def alreadyExistsCheck(): Unit = {
      val alreadyExists = !force && os.exists(destPath)
      if (alreadyExists) {
        val msg = s"$printableDest already exists"
        System.err.println(s"Error: $msg. Pass -f or --force to force erasing it.")
        sys.exit(1)
      }
    }

    alreadyExistsCheck()

    val outputPath = {
      val docJarPath = value(generateScaladocDirPath(build, logger, extraArgs))
      alreadyExistsCheck()
      if (force) os.copy.over(docJarPath, destPath)
      else os.copy(docJarPath, destPath)
      destPath
    }

    val printableOutput = CommandUtils.printablePath(outputPath)

    logger.message(s"Wrote Scaladoc to $printableOutput")
  }

  // from https://github.com/VirtusLab/scala-cli/pull/103/files#diff-1039b442cbd23f605a61fdb9c3620b600aa4af6cab757932a719c54235d8e402R60
  private def defaultScaladocArgs = Seq(
    "-snippet-compiler:compile",
    "-Ygenerate-inkuire",
    "-external-mappings:" +
      ".*scala.*::scaladoc3::https://scala-lang.org/api/3.x/," +
      ".*java.*::javadoc::https://docs.oracle.com/javase/8/docs/api/",
    "-author",
    "-groups"
  )

  def generateScaladocDirPath(
    build: Build.Successful,
    logger: Logger,
    extraArgs: Seq[String]
  ): Either[BuildException, os.Path] = either {
    val docContentDir = build.scalaParams match {
      case Some(scalaParams) if scalaParams.scalaVersion.startsWith("2.") =>
        build.project.scaladocDir
      case Some(scalaParams) =>
        val res = value {
          Artifacts.fetch(
            Positioned.none(Seq(dep"org.scala-lang::scaladoc:${scalaParams.scalaVersion}")),
            build.options.finalRepositories,
            Some(scalaParams),
            logger,
            build.options.finalCache,
            None
          )
        }
        val destDir = build.project.scaladocDir
        os.makeDir.all(destDir)
        val ext = if (Properties.isWin) ".exe" else ""
        val baseArgs = Seq(
          "-classpath",
          build.fullClassPath.map(_.toString).mkString(File.pathSeparator),
          "-d",
          destDir.toString
        )
        val defaultArgs =
          if (
            build.options.notForBloopOptions.packageOptions.useDefaultScaladocOptions.getOrElse(
              true
            )
          )
            defaultScaladocArgs
          else
            Nil
        val args = baseArgs ++
          build.project.scalaCompiler.map(_.scalacOptions).getOrElse(Nil) ++
          extraArgs ++
          defaultArgs ++
          Seq(build.output.toString)
        val retCode = Runner.runJvm(
          (build.options.javaHomeLocation().value / "bin" / s"java$ext").toString,
          Nil, // FIXME Allow to customize that?
          res.files,
          "dotty.tools.scaladoc.Main",
          args,
          logger,
          cwd = Some(build.inputs.workspace)
        ).waitFor()
        if (retCode == 0)
          destDir
        else
          value(Left(new ScaladocGenerationFailedError(retCode)))
      case None =>
        val destDir = build.project.scaladocDir
        os.makeDir.all(destDir)
        val ext = if (Properties.isWin) ".exe" else ""
        val javaSources =
          (build.sources.paths.map(_._1) ++ build.generatedSources.map(_.generated))
            .filter(_.last.endsWith(".java"))
        val command = Seq(
          (build.options.javaHomeLocation().value / "bin" / s"javadoc$ext").toString,
          "-d",
          destDir.toString,
          "-classpath",
          build.project.classesDir.toString
        ) ++
          javaSources.map(_.toString)
        val retCode = Runner.run(
          command,
          logger,
          cwd = Some(build.inputs.workspace)
        ).waitFor()
        if (retCode == 0)
          destDir
        else
          value(Left(new ScaladocGenerationFailedError(retCode)))
    }
    docContentDir
  }

}
