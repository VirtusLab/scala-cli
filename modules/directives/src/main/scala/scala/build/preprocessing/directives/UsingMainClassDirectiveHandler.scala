package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.{BuildException, SeveralMainClassesFoundError}
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath

case object UsingMainClassDirectiveHandler extends UsingDirectiveHandler {

  def name        = "Main class"
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

  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val mainClasses = DirectiveUtil.stringValues(directive.values, path, cwd).map(_._1)
    if (mainClasses.size >= 2)
      Left(
        new SeveralMainClassesFoundError(
          ::(mainClasses.head.value, mainClasses.tail.toList.map(_.value)),
          mainClasses.flatMap(_.positions)
        )
      )
    else {
      val options = BuildOptions(
        mainClass = mainClasses.headOption.map(_.value)
      )
      Right(ProcessedDirective(Some(options), Seq.empty))
    }
  }

}
