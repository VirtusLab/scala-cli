package scala.build.compiler

import ch.epfl.scala.bsp4j.BuildClient

import scala.build.Logger

final case class SimpleScalaCompilerMaker(
  defaultJavaCommand: String,
  defaultJavaOptions: Seq[String],
  scaladoc: Boolean = false
) extends ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger
  ): SimpleScalaCompiler =
    SimpleScalaCompiler(defaultJavaCommand, defaultJavaOptions, scaladoc)
}
