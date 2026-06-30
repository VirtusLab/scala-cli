package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using lazyvalgrade")
@DirectiveUsage("//> using lazyvalgrade", "`//> using lazyvalgrade`")
@DirectiveDescription(
  "Patch Scala 3.0-3.7.x lazy val bytecode on the classpath for JDK 26+ compatibility"
)
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class LazyValGrade(
  @DirectiveName("lazy.val.grade")
  lazyvalgrade: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(BuildOptions(
      notForBloopOptions = PostBuildOptions(lazyValGradeOpt = Some(true))
    ))
}

object LazyValGrade {
  val handler: DirectiveHandler[LazyValGrade] = DirectiveHandler.derive
}
