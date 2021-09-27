package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, TestOptions}

case object UsingTestFrameworkDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Test framework"
  def description = "Sets test framework"
  def usage       = "using test-framework _class_name_"
  override def usageMd =
    "`using test-framework `_class_name_"
  override def examples = Seq(
    "using test-framework utest.runner.Framework"
  )

  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
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
}
