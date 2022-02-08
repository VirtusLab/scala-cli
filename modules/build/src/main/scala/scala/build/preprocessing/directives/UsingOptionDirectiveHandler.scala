package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions, ScalacOpt, ShadowingSeq}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

case object UsingOptionDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Compiler options"
  def description = "Add Scala compiler options"
  def usage       = "using option _option_ | using options _option1_ _option2_ …"
  override def usageMd =
    """`//> using option `_option_
      |
      |`//> using options `_option1_, _option2_ …""".stripMargin
  override def examples = Seq(
    "//> using option \"-Xasync\"",
    "//> using options \"-Xasync\", \"-Xfatal-warnings\""
  )

  override def keys = Seq("option", "options")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values = directive.values
    val scalacOptions = DirectiveUtil.stringValues(values, path, cwd).map {
      case (value, pos, _) =>
        Positioned(Seq(pos), ScalacOpt(value))
    }
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = ShadowingSeq.from(scalacOptions)
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
