package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import scala.build.GeneratedSource
import scala.build.options.Scope

trait HasGeneratedSources {

  import HasGeneratedSources._

  protected var projectNames: List[ProjectName] = Nil
  protected var generatedSources                = GeneratedSources(Nil)

  def targetIds: List[b.BuildTargetIdentifier] =
    projectNames
      .flatMap(_.targetUriOpt)
      .map(uri => new b.BuildTargetIdentifier(uri))

  def targetScopeIdOpt(scope: Scope): Option[b.BuildTargetIdentifier] =
    projectNames.filter(p => if (scope == Scope.Test) p.name.contains("-test") else true)
      .flatMap(_.targetUriOpt)
      .map(uri => new b.BuildTargetIdentifier(uri))
      .headOption

  def setProjectName(workspace: os.Path, name: String): Unit =
    if (!projectNames.exists(n => n.bloopWorkspace == workspace && n.name == name))
      projectNames = projectNames :+ ProjectName(workspace, name)
  def setProjectTestName(workspace: os.Path, name: String): Unit =
    setProjectName(workspace, s"$name-test")

  def setGeneratedSources(sources: Seq[GeneratedSource]): Unit = {
    generatedSources = GeneratedSources(sources)
  }

  protected def validTarget(id: b.BuildTargetIdentifier): Boolean =
    projectNames.flatMap(_.targetUriOpt).contains(id.getUri)

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
