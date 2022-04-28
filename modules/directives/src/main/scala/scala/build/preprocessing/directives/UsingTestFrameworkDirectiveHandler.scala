package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, TestOptions}
import scala.build.Positioned

case object UsingTestFrameworkDirectiveHandler 
  extends BuildOptionsUsingDirectiveHandler[Positioned[String]] {
    def name = "Test framework"
    def description = fromCommand("test-framework", scala.cli.commands.TestOptions.help)
    def keys = Seq("test-framework", "testFramework")
    def constrains = Single(ValueType.String)
    def usagesCode: Seq[String] =
      Seq("using testFramework <class_name> ", "using test-framework <class_name>")

    override def examples = Seq(
      "//> using testFramework \"utest.runner.Framework\""
    )

    def process(v: Positioned[String])(using Ctx) = 
      Right(BuildOptions(testOptions = TestOptions(frameworkOpt = Some(v.value))))
}