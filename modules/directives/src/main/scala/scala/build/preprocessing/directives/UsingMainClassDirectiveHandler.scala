package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.BuildOptions

case object UsingMainClassDirectiveHandler extends UsingDirectiveHandler {

  def name = "Main class"

  def description = "Specify default main class"

  def usage = "//> using main-class _main class_ | //> using mainClass _main class_"

  override def usageMd =
    """`//> using main-class `_main class_
      |
      |`//> using mainClass `_main class_""".stripMargin

  override def examples = Seq(
    "//> using main-class \"helloWorld\""
  )

  def keys = Seq("main-class", "mainClass")

  override def getValueNumberBounds(key: String) = UsingDirectiveValueNumberBounds(1, 1)

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val mainClasses = groupedValues.scopedStringValues.map(_.positioned)
      val options = BuildOptions(
        mainClass = mainClasses.headOption.map(_.value)
      )
      ProcessedDirective(Some(options), Seq.empty)

    }

}
