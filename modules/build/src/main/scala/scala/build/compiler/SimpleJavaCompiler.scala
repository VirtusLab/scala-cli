package scala.build.compiler

import java.io.File

import scala.build.internal.Runner
import scala.build.{Logger, Project}
import scala.util.Properties

final case class SimpleJavaCompiler(
  defaultJavaCommand: String,
  defaultJavaOptions: Seq[String]
) {

  def compile(
    project: Project,
    logger: Logger
  ): Boolean =
    project.sources.isEmpty || {
      val javacCommand = SimpleJavaCompiler.javaCommand(project, "javac")
        .getOrElse(defaultJavaCommand)

      val args = project.javacOptions ++
        Seq(
          "-d",
          project.classesDir.toString,
          "-cp",
          project.classPath.map(_.toString).mkString(File.pathSeparator)
        ) ++
        project.sources.map(_.toString)

      val proc = Runner.run(
        Seq(javacCommand) ++ args,
        logger,
        cwd = Some(project.workspace)
      )

      val res = proc.waitFor()

      res == 0
    }
}

object SimpleJavaCompiler {

  def javaCommand(project: Project, command: String = "java"): Option[String] =
    project.javaHomeOpt.map { javaHome =>
      val ext  = if (Properties.isWin) ".exe" else ""
      val path = javaHome / "bin" / s"$command$ext"
      path.toString
    }
}
