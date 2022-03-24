package scala.build.compiler

import scala.build.{Logger, Positioned, Project}

trait ScalaCompiler {
  def jvmVersion: Option[Positioned[Int]]
  def prepareProject(
    project: Project,
    logger: Logger
  ): Boolean
  def compile(
    project: Project,
    logger: Logger
  ): Boolean
  def shutdown(): Unit
}
