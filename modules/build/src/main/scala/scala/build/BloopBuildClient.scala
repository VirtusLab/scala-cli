package scala.build

import ch.epfl.scala.bsp4j

import scala.build.bsp.buildtargets.ProjectName
import scala.build.options.Scope

trait BloopBuildClient extends bsp4j.BuildClient {
  def setProjectParams(newParams: Seq[String]): Unit
  def setGeneratedSources(projectName: ProjectName, newGeneratedSources: Seq[GeneratedSource]): Unit
  def diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  def clear(): Unit
}

object BloopBuildClient {
  def create(
    projectNameOpt: Option[ProjectName],
    logger: Logger,
    keepDiagnostics: Boolean
  ): BloopBuildClient =
    new ConsoleBloopBuildClient(
      projectNameOpt,
      logger,
      keepDiagnostics
    )
}
