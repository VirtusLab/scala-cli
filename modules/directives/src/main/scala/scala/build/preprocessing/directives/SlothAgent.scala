package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using slothAgent")
@DirectiveUsage("//> using slothAgent", "`//> using slothAgent`")
@DirectiveDescription(
  "Patch Scala 3.0-3.7.x lazy val bytecode at class load time via the sloth Java agent for JDK 26+ compatibility"
)
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class SlothAgent(
  slothAgent: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(BuildOptions(
      notForBloopOptions = PostBuildOptions(slothAgentOpt = Some(true))
    ))
}

object SlothAgent {
  val handler: DirectiveHandler[SlothAgent] = DirectiveHandler.derive
}
