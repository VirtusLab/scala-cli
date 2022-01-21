package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import scala.build.GeneratedSource
import scala.build.options.Scope
import scala.collection.mutable

trait HasGeneratedSources {

  import HasGeneratedSources._

  protected val projectNames     = mutable.Map[Scope, ProjectName]()
  protected val generatedSources = mutable.Map[Scope, GeneratedSources]()

  def targetIds: List[b.BuildTargetIdentifier] =
    projectNames
      .toList
      .sortBy(_._1)
      .map(_._2)
      .flatMap(_.targetUriOpt)
      .map(uri => new b.BuildTargetIdentifier(uri))

  def targetScopeIdOpt(scope: Scope): Option[b.BuildTargetIdentifier] =
    projectNames
      .get(scope)
      .flatMap(_.targetUriOpt)
      .map(uri => new b.BuildTargetIdentifier(uri))

  def setProjectName(workspace: os.Path, name: String, scope: Scope): Unit =
    if (!projectNames.contains(scope))
      projectNames(scope) = ProjectName(workspace, name)

  def setGeneratedSources(scope: Scope, sources: Seq[GeneratedSource]): Unit = {
    generatedSources(scope) = GeneratedSources(sources)
  }

  protected def targetScopeOpt(id: b.BuildTargetIdentifier): Option[Scope] =
    projectNames.collectFirst {
      case (scope, projName) if projName.targetUriOpt.contains(id.getUri) =>
        scope
    }
  protected def validTarget(id: b.BuildTargetIdentifier): Boolean =
    targetScopeOpt(id).nonEmpty

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
      Some((bloopWorkspace / ".scala").toIO.toURI.toASCIIString.stripSuffix("/") + "/?id=" + name)
  }
}
