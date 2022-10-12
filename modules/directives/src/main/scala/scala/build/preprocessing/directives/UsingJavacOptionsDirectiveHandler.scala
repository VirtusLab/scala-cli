package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, JavaOptions, ShadowingSeq}

case object UsingJavacOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Javac options"
  def description = "Add Javac options which will be passed when compiling sources."
  def usage       = "//> using javac-opt _options_ | //> using javacOpt _options_"
  override def usageMd =
    """`//> using javac-opt `_options_
      |
      |`//> using javacOpt `_options_""".stripMargin
  override def examples = Seq(
    "//> using javacOpt \"source\", \"1.8\"",
    "\"target\", \"1.8\""
  )
  override def scalaSpecificationLevel = SpecificationLevel.SHOULD
  def keys = Seq("javacOpt", "javacOptions", "javac-opt", "javac-options")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValuesContainer =>
      val javacOptions = groupedValuesContainer.scopedStringValues
      val options = BuildOptions(
        javaOptions = JavaOptions(
          javacOptions = javacOptions.map(_.positioned)
        )
      )
      ProcessedDirective(Some(options), Seq.empty)
    }

}
