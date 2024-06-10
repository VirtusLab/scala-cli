package scala.build.input.compose

import scala.build.input.WorkspaceOrigin
import scala.build.input.ModuleInputs
import scala.build.options.BuildOptions

sealed trait Inputs {
  
  def modules: Seq[ModuleInputs]

  /** Module targeted by the user. If a command requires a target to be executed (e.g. run or compile), it should be executed on this module. */
  def targetModule: ModuleInputs
  /** Build order for modules to execute the command on the [[targetModule]] */
  def modulesBuildOrder: Seq[ModuleInputs]
  def workspaceOrigin: WorkspaceOrigin
  def workspace: os.Path

  def preprocessInputs(preprocess: ModuleInputs => (ModuleInputs, BuildOptions)): (Inputs, Seq[BuildOptions])
}

/** Result of using [[InputsComposer]] with module config file present */
case class ComposedInputs(
  modules: Seq[ModuleInputs],
  targetModule: ModuleInputs,
  workspace: os.Path,
) extends Inputs {

  // Forced to be the directory where module config file (modules.yaml) resides
  override val workspaceOrigin: WorkspaceOrigin = WorkspaceOrigin.Forced

  lazy val modulesBuildOrder: Seq[ModuleInputs] = {
    val nameMap = modules.map(m => m.projectName -> m)
    val dependencyGraph = modules.map(m => m.projectName -> m.moduleDependencies)

    val visited = mutable.Set.empty[Name] // Track visited nodes
    val result = mutable.Stack.empty[Name] // Use a stack to build the result in reverse order

    def visit(node: ProjectName): Unit = {
      if (!visited.contains(node)) {
        visited += node
        dependencyGraph.getOrElse(node, Nil).foreach(visit) // Visit all the linked nodes first
        result.push(node) // Add the current node after visiting linked nodes
      }
    }

    dependencyGraph.keys.foreach(visit)

    result.toSeq.reverse
  }

  def preprocessInputs(preprocess: ModuleInputs => (ModuleInputs, BuildOptions)): (ComposedInputs, Seq[BuildOptions]) = {
    val (preprocessedModules, buildOptions) =>
      modules.filter(_.projectName == targetModule.projectName)
        .map(preprocess)
        .unzip

      val preprocessedTargetModule = preprocess(targetModule)

      copy(modules = preprocessedModules ++ preprocessedTargetModule, targetModule = preprocessedTargetModule) -> buildOptions
  }
}

/** Essentially a wrapper over a single module, no config file for modules involved */
case class SimpleInputs(
  singleModule: ModuleInputs,
) extends Inputs {
  override val modules: Seq[ModuleInputs] = Seq(singleModule)

  override val targetModule: ModuleInputs = singleModule

  override val modulesBuildOrder = modules

  override val workspace: os.Path = singleModule.workspace

  override val workspaceOrigin: WorkspaceOrigin = singleModule.workspaceOrigin

  def preprocessInputs(preprocess: ModuleInputs => (ModuleInputs, BuildOptions)): (ComposedInputs, Seq[BuildOptions]) =
    copy(singleModule = preprocess(singleModule)) -> buildOptions
}
