package scala.build.input.compose

import scala.build.Build
import scala.build.options.BuildOptions
import scala.build.bsp.buildtargets.ProjectName
import scala.build.errors.BuildException
import scala.build.input.Module

object InputsComposerUtils {
  def argsToEmptyModules(
    args: Seq[String],
    projectNameOpt: Option[ProjectName]
  ): Either[BuildException, Module] = {
    assert(projectNameOpt.isDefined)
    val emptyInputs = Module.empty(projectNameOpt.get.name)
    Right(Build.updateInputs(emptyInputs, BuildOptions()))
  }
}
