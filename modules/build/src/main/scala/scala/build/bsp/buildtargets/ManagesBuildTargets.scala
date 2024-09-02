package scala.build.bsp.buildtargets

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j as b

import scala.build.GeneratedSource
import scala.build.input.{ModuleInputs, compose}
import scala.build.internal.Constants
import scala.build.options.Scope

trait ManagesBuildTargets {
  def targetIds: List[b.BuildTargetIdentifier]
  def targetProjectIdOpt(projectName: ProjectName): Option[b.BuildTargetIdentifier]
  def addTarget(
    projectName: ProjectName,
    workspace: os.Path,
    scope: Scope,
    generatedSources: Seq[GeneratedSource] = Nil
  ): Unit
  def resetTargets(): Unit
  def newInputs(inputs: compose.Inputs): Unit
  def setGeneratedSources(projectName: ProjectName, sources: Seq[GeneratedSource]): Unit
}

object ManagesBuildTargets {

  /** Represents a BuildTarget managed by the BSP
    * @originalSources
    *   \- paths of sources seen by the user
    */
  final case class BuildTarget(
    projectName: ProjectName,
    bloopWorkspace: os.Path,
    scope: Scope,
//    originalSources: Seq[os.Path],
    generatedSources: Seq[GeneratedSource]
  ) {
    val targetId: BuildTargetIdentifier = {
      val identifier = (bloopWorkspace / Constants.workspaceDirName)
        .toIO
        .toURI
        .toASCIIString
        .stripSuffix("/") +
        "/?id=" + projectName.name
      new b.BuildTargetIdentifier(identifier)
    }

    lazy val uriMap: Map[String, GeneratedSource] =
      generatedSources
        .flatMap { g =>
          g.reportingPath match {
            case Left(_)  => Nil
            case Right(_) => Seq(g.generated.toNIO.toUri.toASCIIString -> g)
          }
        }
        .toMap
  }

  final case class GeneratedSources(sources: Seq[GeneratedSource]) {
    lazy val uriMap: Map[String, GeneratedSource] =
      sources
        .flatMap { g =>
          g.reportingPath match {
            case Left(_)  => Nil
            case Right(_) => Seq(g.generated.toNIO.toUri.toASCIIString -> g)
          }
        }
        .toMap
  }

  final case class OldProjectName(
    bloopWorkspace: os.Path,
    name: String,
    var targetUriOpt: Option[String] = None
  ) {
    targetUriOpt =
      Some(
        (bloopWorkspace / Constants.workspaceDirName)
          .toIO
          .toURI
          .toASCIIString
          .stripSuffix("/") +
          "/?id=" + name
      )
  }
}
