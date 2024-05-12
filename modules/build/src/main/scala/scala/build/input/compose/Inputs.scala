package scala.build.input.compose

import scala.build.bsp.buildtargets.ProjectName
import scala.build.input.{ModuleInputs, WorkspaceOrigin}
import scala.build.options.BuildOptions
import scala.collection.mutable

sealed trait Inputs {

  def modules: Seq[ModuleInputs]
  def workspaceOrigin: Option[WorkspaceOrigin]
  def workspace: os.Path

  def preprocessInputs(preprocess: ModuleInputs => (ModuleInputs, BuildOptions))
    : (Inputs, Seq[BuildOptions])

  def sourceHash: String =
    modules.map(_.sourceHash())
      .mkString
}

/** Result of using [[InputsComposer]] with module config file present */
case class ComposedInputs(
  modules: Seq[ModuleInputs],
  workspace: os.Path
) extends Inputs {

  // Forced to be the directory where module config file (modules.yaml) resides
  override val workspaceOrigin: Option[WorkspaceOrigin] = Some(WorkspaceOrigin.Forced)

  def preprocessInputs(preprocess: ModuleInputs => (ModuleInputs, BuildOptions))
    : (ComposedInputs, Seq[BuildOptions]) = {
    val (preprocessedModules, buildOptions) =
      modules.map(preprocess)
        .unzip

    copy(modules = preprocessedModules) -> buildOptions
  }
}

/** Essentially a wrapper over a single module, no config file for modules involved */
case class SimpleInputs(
  singleModule: ModuleInputs
) extends Inputs {
  override val modules: Seq[ModuleInputs] = Seq(singleModule)

  override val workspace: os.Path = singleModule.workspace

  override val workspaceOrigin: Option[WorkspaceOrigin] = singleModule.workspaceOrigin

  override def preprocessInputs(preprocess: ModuleInputs => (ModuleInputs, BuildOptions))
    : (SimpleInputs, Seq[BuildOptions]) =
    val (preprocessedModule, buildOptions) = preprocess(singleModule)
    copy(singleModule = preprocessedModule) -> Seq(buildOptions)
}
