package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import scala.build.options.BuildOptions

@HelpMessage("Generate documentation from Scala code")
final case class DocOptions(
  @Recurse shared: SharedOptions = SharedOptions(),
  @Recurse watch: SharedWatchOptions = SharedWatchOptions()
) extends CompileLikeOptions {

  def buildOptions: BuildOptions = {
    val default = shared.buildOptions(enableJmh = false, jmhVersion = None)
    default.copy(
      internalDependencies = default.internalDependencies.copy(addRunnerDependencyOpt = Some(false)),
      scalaOptions = default.scalaOptions.copy(runScaladoc = true)
    )
  }
}

object DocOptions {
  implicit val parser = Parser[CompileOptions]
  implicit val help = Help[CompileOptions]

  val defaultScaladoc2Options = Seq("-groups")
}
