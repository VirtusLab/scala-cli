package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Java options")
@DirectiveExamples("//> using javaOpt \"-Xmx2g\", \"-Dsomething=a\"")
@DirectiveUsage(
  "//> using java-opt _options_ | //> using javaOpt _options_",
  """`//> using java-opt `_options_
    |
    |`//> using javaOpt `_options_""".stripMargin
)
@DirectiveDescription("Add Java options which will be passed when running an application.")
@DirectiveLevel(SpecificationLevel.MUST)
// format: off
final case class JavaOptions(
  @DirectiveName("javaOpt")
    javaOptions: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      javaOptions = options.JavaOptions(
        javaOpts = ShadowingSeq.from(javaOptions.map(_.map(JavaOpt(_))))
      )
    )
    Right(buildOpt)
  }
}

object JavaOptions {
  val handler: DirectiveHandler[JavaOptions] = DirectiveHandler.derive
}
