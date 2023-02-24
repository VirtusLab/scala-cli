package scala.cli.commands.shared

import caseapp.*

import scala.build.errors.{MainClassError, NoMainClassFoundError}
import scala.cli.commands.tags

// format: off
final case class MainClassOptions(
  @Group(HelpGroup.Entrypoint.toString)
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Tag(tags.must)
  @Name("M")
    mainClass: Option[String] = None,

  @Group(HelpGroup.Entrypoint.toString)
  @HelpMessage("List main classes available in the current context")
  @Name("mainClassList")
  @Name("listMainClass")
  @Name("listMainClasses")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
    mainClassLs: Option[Boolean] = None
) {
  // format: on

  def maybePrintMainClasses(
    mainClasses: Seq[String],
    shouldExit: Boolean = true
  ): Either[MainClassError, Unit] =
    mainClassLs match {
      case Some(true) if mainClasses.nonEmpty =>
        println(mainClasses.mkString(" "))
        if (shouldExit) sys.exit(0)
        else Right(())
      case Some(true) => Left(new NoMainClassFoundError)
      case _          => Right(())
    }
}

object MainClassOptions {
  implicit lazy val parser: Parser[MainClassOptions] = Parser.derive
  implicit lazy val help: Help[MainClassOptions]     = Help.derive
}
