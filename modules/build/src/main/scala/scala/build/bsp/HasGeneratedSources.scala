package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import scala.build.GeneratedSource

trait HasGeneratedSources {

  import HasGeneratedSources._

  protected var projectNameOpt = Option.empty[ProjectName]
  protected var generatedSources = GeneratedSources(Nil)

  def targetIdOpt: Option[b.BuildTargetIdentifier] =
    projectNameOpt
      .flatMap(_.targetUriOpt)
      .map(uri => new b.BuildTargetIdentifier(uri))

  def setProjectName(workspace: os.Path, name: String): Unit =
    if (!projectNameOpt.exists(n => n.bloopWorkspace == workspace && n.name == name)) {
      projectNameOpt = Some(ProjectName(workspace, name))
    }
  def setGeneratedSources(sources: Seq[GeneratedSource]): Unit = {
    generatedSources = GeneratedSources(sources)
  }

  protected def validTarget(id: b.BuildTargetIdentifier): Boolean =
    projectNameOpt.flatMap(_.targetUriOpt).forall(_ == id.getUri)

}

object HasGeneratedSources {
  final case class GeneratedSources(
    sources: Seq[GeneratedSource]
  ) {

    lazy val uriMap: Map[String, GeneratedSource] =
      sources
        .flatMap { g =>
          g.reportingPath.toOption.toSeq.map { reportingPath =>
            g.generated.toNIO.toUri.toASCIIString -> g
          }
        }
        .toMap
  }

  final case class ProjectName(
    bloopWorkspace: os.Path,
    name: String,
    var targetUriOpt: Option[String] = None
  )
}
