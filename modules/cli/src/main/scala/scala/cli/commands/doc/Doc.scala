package scala.cli.commands.doc

import caseapp.*
import caseapp.core.help.HelpFormat
import dependency.*

import java.io.File

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.compiler.{ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.BuildException
import scala.build.interactive.InteractiveFileOps
import scala.build.internal.Runner
import scala.build.options.{BuildOptions, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.{CommandUtils, ScalaCommand, SpecificationLevel}
import scala.cli.config.Keys
import scala.cli.errors.ScaladocGenerationFailedError
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.util.Properties

object Doc extends ScalaCommand[DocOptions] {
  override def group: String = HelpCommandGroup.Main.toString

  override def sharedOptions(options: DocOptions): Option[SharedOptions] = Some(options.shared)

  override def buildOptions(options: DocOptions): Option[BuildOptions] =
    sharedOptions(options)
      .map(shared => shared.buildOptions().orExit(shared.logger))

  override def helpFormat: HelpFormat = super.helpFormat.withPrimaryGroup(HelpGroup.Doc)

  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.MUST

  override def runCommand(options: DocOptions, args: RemainingArgs, logger: Logger): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)
    val inputs              = options.shared.inputs(args.remaining).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val threads = BuildThreads.create()

    val maker               = options.shared.compilerMaker(threads)
    val compilerMaker       = ScalaCompilerMaker.IgnoreScala2(maker)
    val docCompilerMakerOpt = Some(SimpleScalaCompilerMaker("java", Nil, scaladoc = true))

    val configDb = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val withTestScope = options.shared.scope.test.getOrElse(false)
    Build.build(
      inputs,
      initialBuildOptions,
      compilerMaker,
      docCompilerMakerOpt,
      logger,
      crossBuilds = false,
      buildTests = withTestScope,
      partial = None,
      actionableDiagnostics = actionableDiagnostics
    )
      .orExit(logger).docBuilds match {
      case b if b.forall(_.success) =>
        val successfulBuilds = b.collect { case s: Build.Successful => s }
        val res0 = doDoc(
          logger,
          options.output.filter(_.nonEmpty),
          options.force,
          successfulBuilds,
          args.unparsed,
          withTestScope
        )
        res0.orExit(logger)
      case b if b.exists(bb => !bb.success && !bb.cancelled) =>
        System.err.println("Compilation failed")
        sys.exit(1)
      case _ =>
        System.err.println("Build cancelled")
        sys.exit(1)
    }
  }

  private def doDoc(
    logger: Logger,
    outputOpt: Option[String],
    force: Boolean,
    builds: Seq[Build.Successful],
    extraArgs: Seq[String],
    withTestScope: Boolean
  ): Either[BuildException, Unit] = either {

    def defaultName = "scala-doc"

    val dest          = outputOpt.getOrElse(defaultName)
    val destPath      = os.Path(dest, Os.pwd)
    val printableDest = CommandUtils.printablePath(destPath)

    def alreadyExistsCheck(): Either[BuildException, Unit] = {
      val alreadyExists = !force && os.exists(destPath)
      if (alreadyExists)
        builds.head.options.interactive.map { interactive =>
          InteractiveFileOps.erasingPath(interactive, printableDest, destPath) { () =>
            val msg = s"$printableDest already exists"
            System.err.println(s"Error: $msg. Pass -f or --force to force erasing it.")
            sys.exit(1)
          }
        }
      else
        Right(())
    }

    value(alreadyExistsCheck())

    val docJarPath = value(generateScaladocDirPath(builds, logger, extraArgs, withTestScope))
    value(alreadyExistsCheck())
    if force then os.copy.over(docJarPath, destPath) else os.copy(docJarPath, destPath)

    val printableOutput = CommandUtils.printablePath(destPath)

    logger.message(s"Wrote Scaladoc to $printableOutput")
  }

  // from https://github.com/VirtusLab/scala-cli/pull/103/files#diff-1039b442cbd23f605a61fdb9c3620b600aa4af6cab757932a719c54235d8e402R60
  private def defaultScaladocArgs = Seq(
    "-snippet-compiler:compile",
    "-Ygenerate-inkuire",
    "-external-mappings:" +
      ".*/scala/.*::scaladoc3::https://scala-lang.org/api/3.x/," +
      ".*/java/.*::javadoc::https://docs.oracle.com/javase/8/docs/api/",
    "-author",
    "-groups"
  )

  def generateScaladocDirPath(
    builds: Seq[Build.Successful],
    logger: Logger,
    extraArgs: Seq[String],
    withTestScope: Boolean
  ): Either[BuildException, os.Path] = either {
    val docContentDir = builds.head.scalaParams
      .map(sp => sp -> sp.scalaVersion.startsWith("2.")) match {
      case Some((_, true)) if withTestScope =>
        builds.find(_.scope == Scope.Test).getOrElse(builds.head).project.scaladocDir
      case Some((_, true)) => builds.head.project.scaladocDir
      case Some((scalaParams, _)) =>
        val res = value {
          Artifacts.fetchAnyDependencies(
            Seq(Positioned.none(dep"org.scala-lang::scaladoc:${scalaParams.scalaVersion}")),
            value(builds.head.options.finalRepositories),
            Some(scalaParams),
            logger,
            builds.head.options.finalCache,
            None
          )
        }
        val destDir = builds.head.project.scaladocDir
        os.makeDir.all(destDir)
        val ext = if Properties.isWin then ".exe" else ""
        val baseArgs = Seq(
          "-classpath",
          builds.flatMap(_.fullClassPath).distinct.map(_.toString).mkString(File.pathSeparator),
          "-d",
          destDir.toString
        )
        val defaultArgs =
          if builds.head.options.notForBloopOptions.packageOptions.useDefaultScaladocOptions
              .getOrElse(true)
          then defaultScaladocArgs
          else Nil
        val args = baseArgs ++
          builds.head.project.scalaCompiler.map(_.scalacOptions).getOrElse(Nil) ++
          extraArgs ++
          defaultArgs ++
          builds.map(_.output.toString)
        val retCode = Runner.runJvm(
          (builds.head.options.javaHomeLocation().value / "bin" / s"java$ext").toString,
          Nil, // FIXME Allow to customize that?
          res.files.map(os.Path(_, os.pwd)),
          "dotty.tools.scaladoc.Main",
          args,
          logger,
          cwd = Some(builds.head.inputs.workspace)
        ).waitFor()
        if retCode == 0 then destDir
        else value(Left(new ScaladocGenerationFailedError(retCode)))
      case None =>
        val destDir = builds.head.project.scaladocDir
        os.makeDir.all(destDir)
        val ext = if (Properties.isWin) ".exe" else ""
        val javaSources =
          builds
            .flatMap(b => b.sources.paths.map(_._1) ++ b.generatedSources.map(_.generated))
            .distinct
            .filter(_.last.endsWith(".java"))
        val command = Seq(
          (builds.head.options.javaHomeLocation().value / "bin" / s"javadoc$ext").toString,
          "-d",
          destDir.toString,
          "-classpath",
          builds.flatMap(_.fullClassPath).distinct.map(_.toString).mkString(File.pathSeparator)
        ) ++ javaSources.map(_.toString)
        val retCode = Runner.run(
          command,
          logger,
          cwd = Some(builds.head.inputs.workspace)
        ).waitFor()
        if retCode == 0 then destDir
        else value(Left(new ScaladocGenerationFailedError(retCode)))
    }
    docContentDir
  }

}
