package scala.build.preprocessing.directives

import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, TestOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Test framework")
@DirectiveExamples("//> using testFramework utest.runner.Framework")
@DirectiveExamples("//> using test.frameworks utest.runner.Framework munit.Framework")
@DirectiveUsage(
  """using testFramework _class_name_
    |
    |using testFrameworks _class_name_ _another_class_name_
    |
    |using test.framework _class_name_
    |
    |using test.frameworks _class_name_ _another_class_name_""".stripMargin,
  "`//> using testFramework`  _class-name_"
)
@DirectiveDescription("Set the test framework")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Tests(
  @DirectiveName("testFramework")
  @DirectiveName("test.framework")
  @DirectiveName("test.frameworks")
  testFrameworks: Seq[Positioned[String]] = Nil
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      testOptions = TestOptions(
        frameworks = testFrameworks
      )
    )
    Right(buildOpt)
  }
}

object Tests {
  val handler: DirectiveHandler[Tests] = DirectiveHandler.derive
}
