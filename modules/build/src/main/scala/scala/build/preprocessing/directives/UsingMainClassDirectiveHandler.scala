package scala.build.preprocessing.directives
import scala.build.Position
import scala.build.errors.{BuildException, SeveralMainClassesFoundError}
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath

case object UsingMainClassDirectiveHandler extends UsingDirectiveHandler {

  def name        = "Main class"
  def description = "Specify default main class"

  def usage = "using main-class _main class_ | using mainClass _main class_"

  override def usageMd =
    """`using main-class `_main class_
      |
      |`using mainClass `_main class_""".stripMargin

  override def examples = Seq(
    "using main-class helloWorld"
  )

  override def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("main-class", mainClass) =>
        val options = BuildOptions(
          mainClass = Some(mainClass)
        )
        Some(Right(options))
      case _ =>
        None
    }

  override def keys = Seq("main-class", "mainClass")

  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = {
    val mainClasses = DirectiveUtil.stringValues(values)
    if (mainClasses.size >= 2)
      Left(
        new SeveralMainClassesFoundError(
          ::(mainClasses.head, mainClasses.tail.toList)
        )
      )
    else {
      val options = BuildOptions(
        mainClass = mainClasses.headOption
      )
      Right(options)
    }
  }

}
