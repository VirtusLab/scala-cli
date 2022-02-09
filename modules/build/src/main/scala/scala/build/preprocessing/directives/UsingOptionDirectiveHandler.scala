package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions, ScalacOpt, ShadowingSeq}
import scala.build.preprocessing.ScopePath

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

  def keys = Seq("option", "options")
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values        = directive.values
    val scalacOptions = DirectiveUtil.stringValues(values, path, cwd).map(_._1)
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = ShadowingSeq.from(scalacOptions.map(_.map(ScalacOpt(_))))
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
