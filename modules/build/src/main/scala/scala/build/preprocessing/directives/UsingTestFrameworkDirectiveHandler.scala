package scala.build.preprocessing.directives
import scala.build.errors.{
  BuildException,
  NoTestFrameworkValueProvidedError,
  TooManyTestFrameworksProvidedError
}
import scala.build.options.{BuildOptions, TestOptions}
import scala.build.preprocessing.{ScopePath, Scoped}

case object UsingTestFrameworkDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Test framework"
  def description = "Set the test framework"
  def usage       = "using test-framework _class_name_"
  override def usageMd =
    "`using test-framework `_class_name_"
  override def examples = Seq(
    "using test-framework utest.runner.Framework"
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

  override def keys = Seq("test-framework")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, (Option[BuildOptions], Seq[Scoped[BuildOptions]])] = {
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
        Right((Some(options), Seq.empty))
      case _ =>
        Left(new TooManyTestFrameworksProvidedError)
    }
  }
}
