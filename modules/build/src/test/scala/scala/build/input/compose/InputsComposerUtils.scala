package scala.build.input.compose

import scala.build.Build
import scala.build.options.BuildOptions
import scala.build.bsp.buildtargets.ProjectName
import scala.build.errors.BuildException
import scala.build.input.ModuleInputs

object InputsComposerUtils {
  def argsToEmptyModules(
    args: Seq[String],
    projectNameOpt: Option[ProjectName]
  ): Either[BuildException, ModuleInputs] = {
    assert(projectNameOpt.isDefined)
    val emptyInputs = ModuleInputs.empty(projectNameOpt.get.name)
    Right(Build.updateInputs(emptyInputs, BuildOptions()))
  }
}
