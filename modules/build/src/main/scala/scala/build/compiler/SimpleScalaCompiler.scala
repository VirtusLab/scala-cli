package scala.build.compiler

import java.io.File

import scala.build.internal.Runner
import scala.build.{Logger, Positioned, Project}
import scala.util.Properties

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

  def compile(
    project: Project,
    logger: Logger
  ): Boolean =
    if (project.sources.isEmpty) true
    else {

      val isScala2 = project.scalaCompiler.exists(_.scalaVersion.startsWith("2."))

      val mainClassOpt =
        if (isScala2)
          Some {
            if (scaladoc) "scala.tools.nsc.ScalaDoc"
            else "scala.tools.nsc.Main"
          }
        else if (scaladoc) None
        else Some("dotty.tools.dotc.Main")

      mainClassOpt.forall { mainClass =>

        val outputDir =
          if (isScala2 && scaladoc) project.scaladocDir
          else project.classesDir

        os.makeDir.all(outputDir)

        // initially adapted from https://github.com/VirtusLab/scala-cli/pull/103/files#diff-d13a7e6d602b8f84d9177e3138487872f0341d006accfe425886a561f029a9c3R120 and around

        val args =
          project.scalaCompiler.map(_.scalacOptions).getOrElse(Nil) ++
            Seq(
              "-d",
              outputDir.toString,
              "-cp",
              project.classPath.map(_.toString).mkString(File.pathSeparator)
            ) ++
            project.sources.map(_.toString)

        val javaCommand = project.javaHomeOpt match {
          case Some(javaHome) =>
            val ext  = if (Properties.isWin) ".exe" else ""
            val path = javaHome / "bin" / s"java$ext"
            path.toString
          case None => defaultJavaCommand
        }

        val javaOptions = defaultJavaOptions ++
          project.javacOptions
            .filter(_.startsWith("-J"))
            .map(_.stripPrefix("-J"))

        val res = Runner.runJvm(
          javaCommand,
          javaOptions,
          project.scalaCompiler.map(_.compilerClassPath.map(_.toIO)).getOrElse(Nil),
          mainClass,
          args,
          logger,
          cwd = Some(project.workspace)
        ).waitFor()

        res == 0
      }
    }

  def shutdown(): Unit =
    ()

}
