package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

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
    values: Seq[Value[_]],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] =
    DirectiveUtil.stringValues(values) match {
      case Seq() =>
        Left(new NoTestFrameworkValueProvidedError)
      case Seq(fw) =>
        val options = BuildOptions(
          testOptions = TestOptions(
            frameworkOpt = Some(fw._1)
          )
        )
        Right(options)
      case _ =>
        Left(new TooManyTestFrameworksProvidedError)
    }
}
