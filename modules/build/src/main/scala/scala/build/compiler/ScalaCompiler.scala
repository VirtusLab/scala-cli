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

  def usesClassDir: Boolean = true
}

object ScalaCompiler {
  final case class IgnoreScala2(compiler: ScalaCompiler) extends ScalaCompiler {
    private def ignore(
      project: Project,
      logger: Logger
    ): Boolean = {
      val scalaVer = project.scalaCompiler.scalaVersion
      val isScala2 = scalaVer.startsWith("2.")
      logger.debug(s"Ignoring compilation for Scala version $scalaVer")
      isScala2
    }

    def jvmVersion: Option[Positioned[Int]] =
      compiler.jvmVersion
    def prepareProject(
      project: Project,
      logger: Logger
    ): Boolean =
      ignore(project, logger) ||
      compiler.prepareProject(project, logger)
    def compile(
      project: Project,
      logger: Logger
    ): Boolean =
      ignore(project, logger) ||
      compiler.compile(project, logger)
    def shutdown(): Unit =
      compiler.shutdown()
    override def usesClassDir: Boolean =
      compiler.usesClassDir
  }
}
