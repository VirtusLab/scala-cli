package scala.build.compiler

import java.io.File

import scala.build.internal.Runner
import scala.build.{Logger, Positioned, Project}

/** A simple Scala compiler designed to handle scaladocs, Java projects & get `scalac` outputs.
  *
  * @param defaultJavaCommand
  *   the default `java` command to be used
  * @param defaultJavaOptions
  *   the default jvm options to be used with the `java` command
  * @param scaladoc
  *   a flag for setting whether this compiler will handle scaladocs
  */
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

  /** Run a synthetic (created in runtime) `scalac` as a JVM process with the specified parameters
    *
    * @param mainClass
    *   the main class of the synthetic Scala compiler
    * @param javaHomeOpt
    *   Java home path (optional)
    * @param javacOptions
    *   options to be passed for the Java compiler
    * @param scalacOptions
    *   options to be passed for the Scala compiler
    * @param classPath
    *   class path to be passed to `scalac`
    * @param compilerClassPath
    *   class path for the Scala compiler itself
    * @param sources
    *   sources to be passed when running `scalac` (optional)
    * @param outputDir
    *   output directory for the compiler (optional)
    * @param cwd
    *   working directory for running the compiler
    * @param logger
    *   logger
    * @return
    *   compiler process exit code
    */
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
      compilerClassPath,
      mainClass,
      args,
      logger,
      cwd = Some(cwd)
    ).waitFor()
  }

  /** Run a synthetic (created in runtime) `scalac` as a JVM process for a given
    * [[scala.build.Project]]
    *
    * @param project
    *   project to be compiled
    * @param mainClass
    *   the main class of the synthetic Scala compiler
    * @param outputDir
    *   the scala compiler output directory
    * @param logger
    *   logger
    * @return
    *   true if the process returned no errors, false otherwise
    */
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

  /** Run a synthetic (created in runtime) `scalac` as a JVM process with minimal parameters. (i.e.
    * to print `scalac` help)
    *
    * @param scalaVersion
    *   Scala version for which `scalac` is to be created
    * @param javaHomeOpt
    *   Java home path (optional)
    * @param javacOptions
    *   options to be passed for the Java compiler
    * @param scalacOptions
    *   options to be passed for the Scala compiler
    * @param fullClassPath
    *   classpath to be passed to the compiler (optional)
    * @param compilerClassPath
    *   classpath of the compiler itself
    * @param logger
    *   logger
    * @return
    *   compiler process exit code
    */
  def runSimpleScalacLike(
    scalaVersion: String,
    javaHomeOpt: Option[os.Path],
    javacOptions: Seq[String],
    scalacOptions: Seq[String],
    fullClassPath: Seq[os.Path],
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
          classPath = fullClassPath,
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
