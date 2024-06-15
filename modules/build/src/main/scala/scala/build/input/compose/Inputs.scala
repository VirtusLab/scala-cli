package scala.build.input.compose

import scala.build.bsp.buildtargets.ProjectName
import scala.build.input.{Module, WorkspaceOrigin}
import scala.build.options.BuildOptions
import scala.collection.mutable

sealed trait Inputs {

  def modules: Seq[Module]

  /** Module targeted by the user. If a command requires a target to be executed (e.g. run or
    * compile), it should be executed on this module.
    */
  def targetModule: Module

  /** Order in which to build all modules */
  def modulesBuildOrder: Seq[Module]

  /** Order in which to build the target module with its dependencies, e.g. to execute a command on
    * [[targetModule]]
    */
  def targetBuildOrder: Seq[Module]
  def workspaceOrigin: Option[WorkspaceOrigin]
  def workspace: os.Path

  def preprocessInputs(preprocess: Module => (Module, BuildOptions))
    : (Inputs, Seq[BuildOptions])
}

/** Result of using [[InputsComposer]] with module config file present */
case class ComposedInputs(
                           modules: Seq[Module],
                           targetModule: Module,
                           workspace: os.Path
) extends Inputs {

  // Forced to be the directory where module config file (modules.yaml) resides
  override val workspaceOrigin: Option[WorkspaceOrigin] = Some(WorkspaceOrigin.Forced)

  private val nameMap: Map[ProjectName, Module] = modules.map(m => m.projectName -> m).toMap
  private val dependencyGraph = modules.map(m => m.projectName -> m.moduleDependencies).toMap

  private def buildOrderForModule(
                                   root: Module,
                                   visitedPreviously: Set[ProjectName]
  ): Seq[ProjectName] = {
    val visited = mutable.Set.from(visitedPreviously) // Track visited nodes
    val result =
      mutable.Stack.empty[ProjectName] // Use a stack to build the result in reverse order

    def visit(node: ProjectName): Unit = {
      if (!visited.contains(node)) {
        visited += node
        dependencyGraph.getOrElse(node, Nil).foreach(visit) // Visit all the linked nodes first
        result.push(node) // Add the current node after visiting linked nodes
      }
    }

    visit(root.projectName)
    result.reverse.toSeq
  }

  override lazy val modulesBuildOrder: Seq[Module] =
    modules.foldLeft(Seq.empty[ProjectName]) { (acc, module) =>
      val buildOrder = buildOrderForModule(module, visitedPreviously = acc.toSet)
      acc.appendedAll(buildOrder)
    }.map(nameMap)

  override lazy val targetBuildOrder: Seq[Module] =
    buildOrderForModule(targetModule, Set.empty).map(nameMap)

  def preprocessInputs(preprocess: Module => (Module, BuildOptions))
    : (ComposedInputs, Seq[BuildOptions]) = {
    val (preprocessedModules, buildOptions) =
      modules.filterNot(_.projectName == targetModule.projectName)
        .map(preprocess)
        .unzip

    val (preprocessedTargetModule, targetBuildOptions) = preprocess(targetModule)

    copy(
      modules = preprocessedModules.appended(preprocessedTargetModule),
      targetModule = preprocessedTargetModule
    ) -> buildOptions.appended(targetBuildOptions)
  }
}

/** Essentially a wrapper over a single module, no config file for modules involved */
case class SimpleInputs(
  singleModule: Module
) extends Inputs {
  override val modules: Seq[Module] = Seq(singleModule)

  override val targetModule: Module = singleModule

  override val modulesBuildOrder: Seq[Module] = modules

  override val targetBuildOrder: Seq[Module] = modules

  override val workspace: os.Path = singleModule.workspace

  override val workspaceOrigin: Option[WorkspaceOrigin] = singleModule.workspaceOrigin

  override def preprocessInputs(preprocess: Module => (Module, BuildOptions))
    : (SimpleInputs, Seq[BuildOptions]) =
    val (preprocessedModule, buildOptions) = preprocess(singleModule)
    copy(singleModule = preprocessedModule) -> Seq(buildOptions)
}
