package scala.cli.commands.util

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.build.{Build, Logger}
import scala.cli.commands.package0.{Package => PackageCmd}
import scala.cli.commands.packaging.Spark
import scala.cli.commands.run.RunMode
import scala.cli.packaging.Library
import scala.util.Properties

object RunSpark {

  def run(
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    submitArgs: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path],
    extraJavaOpts: Seq[String] = Nil,
    extraEnv: Map[String, String] = Map.empty,
    withPy4j: Boolean = false,
    extraJars: Seq[os.Path] = Nil
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {

    // FIXME Get Spark.sparkModules via provided settings?
    val providedModules = Spark.sparkModules
    val providedFiles =
      value(PackageCmd.providedFiles(build, providedModules, logger)).toSet
    val depCp        = build.dependencyClassPath.filterNot(providedFiles) ++ extraJars
    val javaHomeInfo = build.options.javaHome().value
    val javaOpts     = extraJavaOpts ++ build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
    val ext          = if (Properties.isWin) ".cmd" else ""
    val submitCommand: String =
      Option(System.getenv("SPARK_HOME"))
        .map(os.Path(_, os.pwd))
        .map(_ / "bin" / s"spark-submit$ext")
        .filter(os.exists(_))
        .map(_.toString)
        .getOrElse(s"spark-submit$ext")
    val jarsArgs =
      if (depCp.isEmpty) Nil
      else Seq("--jars", depCp.mkString(","))

    scratchDirOpt.foreach(os.makeDir.all(_))
    val library = os.temp(
      Library.libraryJar(build),
      dir = scratchDirOpt.orNull,
      deleteOnExit = scratchDirOpt.isEmpty,
      prefix = "spark-job",
      suffix = ".jar"
    )

    val (actualMainClass, extraFirstArgs) =
      if (withPy4j)
        ("withpy4j.WithPy4j", Seq(mainClass))
      else
        (mainClass, Nil)

    val finalCommand =
      Seq(submitCommand, "--class", actualMainClass) ++
        jarsArgs ++
        javaOpts.flatMap(opt => Seq("--driver-java-options", opt)) ++
        submitArgs ++
        Seq(library.toString) ++
        extraFirstArgs ++
        args
    val envUpdates = javaHomeInfo.envUpdates(sys.env) ++ extraEnv
    if (showCommand)
      Left(Runner.envCommand(envUpdates) ++ finalCommand)
    else {
      val proc =
        if (allowExecve)
          Runner.maybeExec("spark-submit", finalCommand, logger, extraEnv = envUpdates)
        else
          Runner.run(finalCommand, logger, extraEnv = envUpdates)
      Right((
        proc,
        if (scratchDirOpt.isEmpty) Some(() => os.remove(library, checkExists = true))
        else None
      ))
    }
  }

  def runStandalone(
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    submitArgs: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path],
    extraJavaOpts: Seq[String] = Nil,
    extraEnv: Map[String, String] = Map.empty,
    withPy4j: Boolean = false,
    extraJars: Seq[os.Path] = Nil
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {

    // FIXME Get Spark.sparkModules via provided settings?
    val providedModules = Spark.sparkModules
    val sparkClassPath  = value(PackageCmd.providedFiles(build, providedModules, logger))

    scratchDirOpt.foreach(os.makeDir.all(_))
    val library = os.temp(
      Library.libraryJar(build),
      dir = scratchDirOpt.orNull,
      deleteOnExit = scratchDirOpt.isEmpty,
      prefix = "spark-job",
      suffix = ".jar"
    )

    val (actualMainClass, extraFirstArgs) =
      if (withPy4j)
        ("withpy4j.WithPy4j", Seq(mainClass))
      else
        (mainClass, Nil)

    val finalMainClass = "org.apache.spark.deploy.SparkSubmit"
    val depCp          = build.dependencyClassPath.filterNot(sparkClassPath.toSet) ++ extraJars
    val javaHomeInfo   = build.options.javaHome().value
    val javaOpts = extraJavaOpts ++ build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
    val jarsArgs =
      if (depCp.isEmpty) Nil
      else Seq("--jars", depCp.mkString(","))
    val finalArgs =
      Seq("--class", actualMainClass) ++
        jarsArgs ++
        javaOpts.flatMap(opt => Seq("--driver-java-options", opt)) ++
        submitArgs ++
        Seq(library.toString) ++
        extraFirstArgs ++
        args
    val envUpdates = javaHomeInfo.envUpdates(sys.env) ++ extraEnv
    if (showCommand) {
      val command = Runner.jvmCommand(
        javaHomeInfo.javaCommand,
        javaOpts,
        library +: (build.dependencyClassPath ++ extraJars),
        finalMainClass,
        finalArgs,
        extraEnv = envUpdates,
        useManifest = build.options.notForBloopOptions.runWithManifest,
        scratchDirOpt = scratchDirOpt
      )
      Left(command)
    }
    else {
      val proc = Runner.runJvm(
        javaHomeInfo.javaCommand,
        javaOpts,
        library +: (build.dependencyClassPath ++ extraJars),
        finalMainClass,
        finalArgs,
        logger,
        allowExecve = allowExecve,
        extraEnv = envUpdates,
        useManifest = build.options.notForBloopOptions.runWithManifest,
        scratchDirOpt = scratchDirOpt
      )
      Right((
        proc,
        if (scratchDirOpt.isEmpty) Some(() => os.remove(library, checkExists = true))
        else None
      ))
    }
  }
}
