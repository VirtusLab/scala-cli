package scala.build.preprocessing.directives
import scala.build.errors.{
  BuildException,
  NoTestFrameworkValueProvidedError,
  TooManyTestFrameworksProvidedError
}
import scala.build.options.{BuildOptions, TestOptions}
import scala.build.preprocessing.ScopePath

case object UsingTestFrameworkDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Test framework"
  def description = "Set the test framework"
  def usage       = "using testFramework _class_name_ | using `test-framework` _class_name_"
  override def usageMd =
    "`// using testFramework `_class_name_ | ``// using `test-framework` ``_class_name_"
  override def examples = Seq(
    "// using testFramework \"utest.runner.Framework\""
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("test-framework", fw) =>
        val options = BuildOptions(
          testOptions = TestOptions(
            frameworkOpt = Some(fw)
          )
        )
        Some(Right(options))
      case _ =>
        None
    }

  override def keys = Seq("test-framework", "testFramework")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values = directive.values
    DirectiveUtil.stringValues(values, path, cwd) match {
      case Seq() =>
        Left(new NoTestFrameworkValueProvidedError)
      case Seq(fw) =>
        val options = BuildOptions(
          testOptions = TestOptions(
            frameworkOpt = Some(fw._1)
          )
        )
        Right(ProcessedDirective(Some(options), Seq.empty))
      case _ =>
        Left(new TooManyTestFrameworksProvidedError)
    }
  }
}
