package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions, ScalacOpt, ShadowingSeq}

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

  override def scalaSpecificationLevel = SpecificationLevel.MUST

  def keys = Seq("option", "options")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val scalacOptions = groupedValues.scopedStringValues.map(_.positioned)
      val options = BuildOptions(
        scalaOptions = ScalaOptions(
          scalacOptions = ShadowingSeq.from(scalacOptions.map(_.map(ScalacOpt(_))))
        )
      )
      ProcessedDirective(Some(options), Seq.empty)
    }

}
