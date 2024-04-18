package scala.build.bsp.buildtargets

import ch.epfl.scala.bsp4j as b

import scala.build.GeneratedSource
import scala.build.bsp.buildtargets.ManagesBuildTargets
import scala.build.errors.{BuildException, WorkspaceError}
import scala.build.input.Inputs
import scala.build.internal.Constants
import scala.build.options.Scope
import scala.collection.mutable
import scala.util.Try

trait ManagesBuildTargetsImpl extends ManagesBuildTargets {

  import ManagesBuildTargets.*

//  protected val projectNames     = mutable.Map[Scope, OldProjectName]()
//  protected val generatedSources = mutable.Map[Scope, GeneratedSources]()
  protected var managedTargets = mutable.Map[ProjectName, BuildTarget]()

  override def targetIds: List[b.BuildTargetIdentifier] =
    managedTargets.values.toList.map(_.targetId)

  override def targetProjectIdOpt(projectName: ProjectName): Option[b.BuildTargetIdentifier] =
    managedTargets.get(projectName).map(_.targetId)

  override def resetTargets(): Unit = managedTargets.clear()
  override def addTarget(
    projectName: ProjectName,
    workspace: os.Path,
    scope: Scope,
    generatedSources: Seq[GeneratedSource] = Nil
  ): Unit =
    managedTargets.put(projectName, BuildTarget(projectName, workspace, scope, generatedSources))

  // TODO MG
  override def newInputs(inputs: Inputs): Unit = {
    resetTargets()
    addTarget(inputs.projectName, inputs.workspace, Scope.Main)
    addTarget(inputs.scopeProjectName(Scope.Test), inputs.workspace, Scope.Test)
  }
  override def setGeneratedSources(
    projectName: ProjectName,
    sources: Seq[GeneratedSource]
  ): Unit = {
    val buildTarget = Try(managedTargets(projectName))
      // TODO MG
      .getOrElse(throw WorkspaceError("No BuildTarget to put generated sources"))

    managedTargets.put(projectName, buildTarget.copy(generatedSources = sources))
  }

  protected def targetWorkspaceDirOpt(id: b.BuildTargetIdentifier): Option[String] =
    managedTargets.values.collectFirst {
      case b: BuildTarget if b.targetId == id =>
        (b.bloopWorkspace / Constants.workspaceDirName).toIO.toURI.toASCIIString
    }
  protected def targetProjectNameOpt(id: b.BuildTargetIdentifier): Option[ProjectName] =
    managedTargets.collectFirst {
      case projectName -> buildTarget if buildTarget.targetId == id => projectName
    }

  protected def validTarget(id: b.BuildTargetIdentifier): Boolean =
    targetProjectNameOpt(id).nonEmpty

}
