package scala.build.compiler

import ch.epfl.scala.bsp4j.BuildClient

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.BuildOptions

final case class SimpleScalaCompilerMaker(
  defaultJavaCommand: String,
  defaultJavaOptions: Seq[String],
  scaladoc: Boolean = false
) extends ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger,
    buildOptions: BuildOptions
  ): Either[BuildException, ScalaCompiler] =
    Right(SimpleScalaCompiler(defaultJavaCommand, defaultJavaOptions, scaladoc))
}
