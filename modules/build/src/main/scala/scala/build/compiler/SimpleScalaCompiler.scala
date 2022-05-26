package scala.build.compiler

import java.io.File

import scala.build.internal.Runner
import scala.build.{Logger, Positioned, Project}

final case class SimpleScalaCompiler(
  defaultJavaCommand: String,
  defaultJavaOptions: Seq[String],
  scaladoc: Boolean
) extends ScalaCompiler {

  def jvmVersion: Option[Positioned[Int]] =
    None // ??? TODO

  def prepareProject(
    project: Project,
    logger: Logger
  ): Boolean =
    // no incremental compilation, always compiling everything every time
    true

  override def usesClassDir: Boolean =
    !scaladoc

  private def runScalacLike(
    mainClass: String,
    javaHomeOpt: Option[os.Path],
    javacOptions: Seq[String],
    scalacOptions: Seq[String],
    classPath: Seq[os.Path],
    compilerClassPath: Seq[os.Path],
    sources: Seq[String],
    outputDir: Option[os.Path],
    cwd: os.Path,
    logger: Logger
  ): Int = {

    outputDir.foreach(os.makeDir.all(_))

    // initially adapted from https://github.com/VirtusLab/scala-cli/pull/103/files#diff-d13a7e6d602b8f84d9177e3138487872f0341d006accfe425886a561f029a9c3R120 and around
    val outputDirArgs = outputDir.map(od => Seq("-d", od.toString())).getOrElse(Nil)
    val classPathArgs =
      if (classPath.nonEmpty)
        Seq("-cp", classPath.map(_.toString).mkString(File.pathSeparator))
      else Nil

    val args = scalacOptions ++ outputDirArgs ++ classPathArgs ++ sources

    val javaCommand =
      javaHomeOpt.map(SimpleJavaCompiler.javaCommand(_)).getOrElse(defaultJavaCommand)

    val javaOptions = defaultJavaOptions ++
      javacOptions
        .filter(_.startsWith("-J"))
        .map(_.stripPrefix("-J"))

    Runner.runJvm(
      javaCommand,
      javaOptions,
      compilerClassPath.map(_.toIO),
      mainClass,
      args,
      logger,
      cwd = Some(cwd)
    ).waitFor()
  }

  private def runScalacLikeForProject(
    project: Project,
    mainClass: String,
    outputDir: os.Path,
    logger: Logger
  ): Boolean = {
    val res = runScalacLike(
      mainClass = mainClass,
      javaHomeOpt = project.javaHomeOpt,
      javacOptions = project.javacOptions,
      scalacOptions = project.scalaCompiler.map(_.scalacOptions).getOrElse(Nil),
      classPath = project.classPath,
      compilerClassPath = project.scalaCompiler.map(_.compilerClassPath).getOrElse(Nil),
      sources = project.sources.map(_.toString),
      outputDir = Some(outputDir),
      cwd = project.workspace,
      logger = logger
    )
    res == 0
  }

  def runSimpleScalacLike(
    scalaVersion: String,
    javaHomeOpt: Option[os.Path],
    javacOptions: Seq[String],
    scalacOptions: Seq[String],
    compilerClassPath: Seq[os.Path],
    logger: Logger
  ): Int =
    compilerMainClass(scalaVersion) match {
      case Some(mainClass) =>
        runScalacLike(
          mainClass = mainClass,
          javaHomeOpt = javaHomeOpt,
          javacOptions = javacOptions,
          scalacOptions = scalacOptions,
          classPath = Nil,
          compilerClassPath = compilerClassPath,
          sources = Nil,
          outputDir = None,
          cwd = os.pwd,
          logger = logger
        )
      case _ => 1
    }

  private def compilerMainClass(scalaVersion: String): Option[String] =
    if (scalaVersion.startsWith("2."))
      Some {
        if (scaladoc) "scala.tools.nsc.ScalaDoc"
        else "scala.tools.nsc.Main"
      }
    else if (scaladoc) None
    else Some("dotty.tools.dotc.Main")

  def compile(
    project: Project,
    logger: Logger
  ): Boolean =
    if (project.sources.isEmpty) true
    else
      project.scalaCompiler match {
        case Some(compiler) =>
          val isScala2 = compiler.scalaVersion.startsWith("2.")
          compilerMainClass(compiler.scalaVersion).forall { mainClass =>

            val outputDir =
              if (isScala2 && scaladoc) project.scaladocDir
              else project.classesDir

            runScalacLikeForProject(project, mainClass, outputDir, logger)
          }

        case None =>
          scaladoc ||
          SimpleJavaCompiler(defaultJavaCommand, defaultJavaOptions).compile(project, logger)
      }

  def shutdown(): Unit =
    ()

}
