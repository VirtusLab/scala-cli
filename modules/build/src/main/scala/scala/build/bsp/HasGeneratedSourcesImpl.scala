package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import scala.build.options.Scope
import scala.build.{GeneratedSource, Inputs}
import scala.collection.mutable

trait HasGeneratedSourcesImpl extends HasGeneratedSources {

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

  def resetProjectNames(): Unit =
    projectNames.clear()
  def setProjectName(workspace: os.Path, name: String, scope: Scope): Unit =
    if (!projectNames.contains(scope))
      projectNames(scope) = ProjectName(workspace, name)

  def newInputs(inputs: Inputs): Unit = {
    resetProjectNames()
    setProjectName(inputs.workspace, inputs.projectName, Scope.Main)
    setProjectName(inputs.workspace, inputs.scopeProjectName(Scope.Test), Scope.Test)
  }

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
