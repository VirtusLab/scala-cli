package scala.cli.commands.util

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.build.{Build, Logger}
import scala.cli.commands.package0.{Package => PackageCmd}
import scala.cli.commands.packaging.Spark

object RunHadoop {

  def run(
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path]
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {

    // FIXME Get Spark.hadoopModules via provided settings?
    val providedModules = Spark.hadoopModules
    scratchDirOpt.foreach(os.makeDir.all(_))
    val assembly = os.temp(
      dir = scratchDirOpt.orNull,
      prefix = "hadoop-job",
      suffix = ".jar",
      deleteOnExit = scratchDirOpt.isEmpty
    )
    value {
      PackageCmd.assembly(
        build,
        assembly,
        // "hadoop jar" doesn't accept a main class as second argument if the jar as first argument has a main class in its manifest…
        None,
        providedModules,
        withPreamble = false,
        () => Right(()),
        logger
      )
    }

    val javaOpts = build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
    val extraEnv =
      if (javaOpts.isEmpty) Map[String, String]()
      else
        Map(
          "HADOOP_CLIENT_OPTS" -> javaOpts.mkString(" ") // no escaping…
        )
    val hadoopJarCommand = Seq("hadoop", "jar")
    val finalCommand =
      hadoopJarCommand ++ Seq(assembly.toString, mainClass) ++ args
    if (showCommand)
      Left(Runner.envCommand(extraEnv) ++ finalCommand)
    else {
      val proc =
        if (allowExecve)
          Runner.maybeExec("hadoop", finalCommand, logger, extraEnv = extraEnv)
        else
          Runner.run(finalCommand, logger, extraEnv = extraEnv)
      Right((
        proc,
        if (scratchDirOpt.isEmpty) Some(() => os.remove(assembly, checkExists = true))
        else None
      ))
    }

  }

}
