package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ShadowingSeq, TestOptions}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Test framework")
@DirectiveExamples("//> using testFramework \"utest.runner.Framework\"")
@DirectiveUsage(
  "using testFramework _class_name_",
  "`//> using testFramework ` _class_name_"
)
@DirectiveDescription("Set the test framework")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Tests(
  testFramework: Option[String] = None
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      testOptions = TestOptions(
        frameworkOpt = testFramework
      )
    )
    Right(buildOpt)
  }
}

object Tests {
  val handler: DirectiveHandler[Tests] = DirectiveHandler.derive
}
