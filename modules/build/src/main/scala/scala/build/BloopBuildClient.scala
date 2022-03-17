package scala.build

import ch.epfl.scala.bsp4j

import scala.build.options.Scope

trait BloopBuildClient extends bsp4j.BuildClient {
  def setProjectParams(newParams: Seq[String]): Unit
  def setGeneratedSources(scope: Scope, newGeneratedSources: Seq[GeneratedSource]): Unit
  def diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  def clear(): Unit
}

object BloopBuildClient {
  def create(
    logger: Logger,
    keepDiagnostics: Boolean
  ): BloopBuildClient =
    new ConsoleBloopBuildClient(
      logger,
      keepDiagnostics
    )
}
