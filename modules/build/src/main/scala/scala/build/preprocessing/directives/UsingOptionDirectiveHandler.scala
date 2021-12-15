package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath

case object UsingOptionDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Compiler options"
  def description = "Add Scala compiler options"
  def usage       = "using option _option_ | using options _option1_ _option2_ …"
  override def usageMd =
    """`// using option `_option_
      |
      |`// using options `_option1_, _option2_ …""".stripMargin
  override def examples = Seq(
    "// using option \"-Xasync\"",
    "// using options \"-Xasync\", \"-Xfatal-warnings\""
  )

  override def keys = Seq("option", "options")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values        = directive.values
    val scalacOptions = DirectiveUtil.stringValues(values, path, cwd)
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = scalacOptions.map(_._1)
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
