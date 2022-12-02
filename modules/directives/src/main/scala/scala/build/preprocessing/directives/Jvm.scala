package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("JVM version")
@DirectiveExamples("//> using jvm \"11\"")
@DirectiveExamples("//> using jvm \"adopt:11\"")
@DirectiveExamples("//> using jvm \"graalvm:21\"")
@DirectiveUsage(
  "//> using jvm _value_",
  "`//> using jvm` _value_"
)
@DirectiveDescription("Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Jvm(
  jvm: Option[Positioned[String]] = None
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      javaOptions = options.JavaOptions(
        jvmIdOpt = jvm
      )
    )
    Right(buildOpt)
  }
}

object Jvm {
  val handler: DirectiveHandler[Jvm] = DirectiveHandler.derive
}
