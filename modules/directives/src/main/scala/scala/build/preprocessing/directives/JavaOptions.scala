package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, Scope, ShadowingSeq, WithBuildRequirements}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Java options")
@DirectiveExamples("//> using javaOpt -Xmx2g, -Dsomething=a")
@DirectiveExamples("//> using test.javaOpt -Dsomething=a")
@DirectiveUsage(
  "//> using javaOpt _options_",
  "`//> using javaOpt `_options_"
)
@DirectiveDescription("Add Java options which will be passed when running an application.")
@DirectiveLevel(SpecificationLevel.MUST)
final case class JavaOptions(
  @DirectiveName("javaOpt")
  javaOptions: List[Positioned[String]] = Nil,
  @DirectiveName("test.javaOpt")
  testJavaOptions: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    JavaOptions.buildOptions(javaOptions).map(_.withEmptyRequirements),
    JavaOptions.buildOptions(testJavaOptions).map(_.withScopeRequirement(Scope.Test))
  )
}

object JavaOptions {
  val handler: DirectiveHandler[JavaOptions] = DirectiveHandler.derive
  def buildOptions(javaOptions: List[Positioned[String]]): Either[BuildException, BuildOptions] =
    Right {
      BuildOptions(javaOptions =
        options.JavaOptions(javaOpts = ShadowingSeq.from(javaOptions.map(_.map(JavaOpt(_)))))
      )
    }
}
