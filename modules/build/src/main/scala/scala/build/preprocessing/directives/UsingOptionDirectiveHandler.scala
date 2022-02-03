package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.collections.BuildOptionsConverterImplicits._
import scala.build.options.collections.OptionPrefixes
import scala.build.options.{BuildOptions, ScalaOptions}
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
    val values        = directive.values
    val scalacOptions = DirectiveUtil.stringValues(values, path, cwd)
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions =
          scalacOptions.map(option => Positioned(option._2, option._1)).toStringOptionsList(
            OptionPrefixes.scalacPrefixes
          )
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
