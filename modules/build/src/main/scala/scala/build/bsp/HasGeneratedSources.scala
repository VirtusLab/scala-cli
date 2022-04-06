package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import scala.build.GeneratedSource
import scala.build.internal.Constants
import scala.build.options.Scope

trait HasGeneratedSources {
  def targetIds: List[b.BuildTargetIdentifier]
  def targetScopeIdOpt(scope: Scope): Option[b.BuildTargetIdentifier]
  def setProjectName(workspace: os.Path, name: String, scope: Scope): Unit
  def setGeneratedSources(scope: Scope, sources: Seq[GeneratedSource]): Unit
}

object HasGeneratedSources {
  final case class GeneratedSources(
    sources: Seq[GeneratedSource]
  ) {

    lazy val uriMap: Map[String, GeneratedSource] =
      sources
        .flatMap { g =>
          g.reportingPath.toOption.toSeq.map { _ =>
            g.generated.toNIO.toUri.toASCIIString -> g
          }
        }
        .toMap
  }

  final case class ProjectName(
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
