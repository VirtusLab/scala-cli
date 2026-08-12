package scala.cli.commands.util

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.build.internal.util.WarningMessages
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.internals.EnvVar
import scala.build.postprocessing.{SlothAgent, SlothPatcher}
import scala.build.{Build, Logger}
import scala.cli.commands.package0.Package as PackageCmd
import scala.cli.commands.packaging.Spark
import scala.cli.packaging.Library
import scala.util.Properties

object RunSpark {

  def run(
    builds: Seq[Build.Successful],
    mainClass: String,
    args: Seq[String],
    submitArgs: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path]
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {
    // Batch --sloth can patch the library JAR and --jars classpath; the agent cannot be
    // attached to an external `spark-submit` process.
    if builds.head.options.notForBloopOptions.slothAgent then
      logger.message(
        s"$warnPrefix ${WarningMessages.slothNotApplicable("spark-submit", forAgent = true)}"
      )

    // FIXME Get Spark.sparkModules via provided settings?
    val providedModules = Spark.sparkModules
    val providedFiles   =
      value(PackageCmd.providedFiles(builds, providedModules, logger)).toSet
    val depCp0 = builds.flatMap(_.dependencyClassPath).distinct.filterNot(providedFiles)
    val depCp  = value(SlothPatcher.transformClassPath(
      depCp0,
      builds.head.options,
      logger,
      patchProjectClassDirs = false,
      projectClassDirs = Set.empty
    ))
    val javaHomeInfo          = builds.head.options.javaHome().value
    val javaOpts              = builds.head.options.javaOptions.javaOpts.toSeq.map(_.value.value)
    val ext                   = if Properties.isWin then ".cmd" else ""
    val submitCommand: String =
      EnvVar.Spark.sparkHome.valueOpt
        .map(os.Path(_, os.pwd))
        .map(_ / "bin" / s"spark-submit$ext")
        .filter(os.exists(_))
        .map(_.toString)
        .getOrElse(s"spark-submit$ext")
    val jarsArgs =
      if (depCp.isEmpty) Nil
      else Seq("--jars", depCp.mkString(","))

    scratchDirOpt.foreach(os.makeDir.all(_))
    val library        = Library.libraryJar(builds)
    val patchedLibrary =
      value(SlothPatcher.patchJarFile(
        library,
        builds.head.options,
        logger,
        hierarchyClassPath = library +: depCp0,
        source = SlothPatcher.SlothSource.Project
      ))

    val finalCommand =
      Seq(submitCommand, "--class", mainClass) ++
        jarsArgs ++
        javaOpts.flatMap(opt => Seq("--driver-java-options", opt)) ++
        submitArgs ++
        Seq(patchedLibrary.toString) ++
        args
    val envUpdates = javaHomeInfo.envUpdates(sys.env)
    if showCommand then Left(Runner.envCommand(envUpdates) ++ finalCommand)
    else {
      val proc =
        if allowExecve then
          Runner.maybeExec("spark-submit", finalCommand, logger, extraEnv = envUpdates)
        else Runner.run(finalCommand, logger, extraEnv = envUpdates)
      Right((
        proc,
        if scratchDirOpt.isEmpty then
          Some(() => os.remove(library, checkExists = true))
        else None
      ))
    }
  }

  def runStandalone(
    builds: Seq[Build.Successful],
    mainClass: String,
    args: Seq[String],
    submitArgs: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path]
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {

    // FIXME Get Spark.sparkModules via provided settings?
    val providedModules              = Spark.sparkModules
    val sparkClassPath: Seq[os.Path] = value(PackageCmd.providedFiles(
      builds,
      providedModules,
      logger
    ))

    scratchDirOpt.foreach(os.makeDir.all(_))
    val library        = Library.libraryJar(builds)
    val patchedLibrary =
      value(SlothPatcher.patchJarFile(
        library,
        builds.head.options,
        logger,
        hierarchyClassPath = builds.flatMap(_.fullClassPath).distinct,
        source = SlothPatcher.SlothSource.Project
      ))

    val finalMainClass = "org.apache.spark.deploy.SparkSubmit"
    val depCp0         =
      builds.flatMap(_.dependencyClassPath).distinct.filterNot(sparkClassPath.toSet)
    val depCp = value(SlothPatcher.transformClassPath(
      depCp0,
      builds.head.options,
      logger,
      patchProjectClassDirs = false,
      projectClassDirs = Set.empty
    ))
    val javaHomeInfo       = builds.head.options.javaHome().value
    val baseJavaOpts       = builds.head.options.javaOptions.javaOpts.toSeq.map(_.value.value)
    val slothAgentJavaOpts =
      value(SlothAgent.javaAgentArgs(builds.head.options, logger))
    val javaOpts  = slothAgentJavaOpts ++ baseJavaOpts
    val jarsArgs  = if depCp.isEmpty then Nil else Seq("--jars", depCp.mkString(","))
    val finalArgs =
      Seq("--class", mainClass) ++
        jarsArgs ++
        javaOpts.flatMap(opt => Seq("--driver-java-options", opt)) ++
        submitArgs ++
        Seq(patchedLibrary.toString) ++
        args
    val envUpdates = javaHomeInfo.envUpdates(sys.env)
    if showCommand then
      Left {
        Runner.jvmCommand(
          javaHomeInfo.javaCommand,
          javaOpts,
          sparkClassPath,
          finalMainClass,
          finalArgs,
          extraEnv = envUpdates,
          useManifest = builds.head.options.notForBloopOptions.runWithManifest,
          scratchDirOpt = scratchDirOpt
        )
      }
    else {
      val proc = Runner.runJvm(
        javaHomeInfo.javaCommand,
        javaOpts,
        sparkClassPath,
        finalMainClass,
        finalArgs,
        logger,
        allowExecve = allowExecve,
        extraEnv = envUpdates,
        useManifest = builds.head.options.notForBloopOptions.runWithManifest,
        scratchDirOpt = scratchDirOpt
      )
      Right((
        proc,
        if scratchDirOpt.isEmpty then Some(() => os.remove(library, checkExists = true)) else None
      ))
    }
  }
}
