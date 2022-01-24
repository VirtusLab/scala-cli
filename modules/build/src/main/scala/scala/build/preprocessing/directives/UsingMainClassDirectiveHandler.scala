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

  override def keys = Seq("main-class", "mainClass")

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val mainClasses = DirectiveUtil.stringValues(directive.values, path, cwd).map(_._1)
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
      Right(ProcessedDirective(Some(options), Seq.empty))
    }
  }

}
