package scala.cli.commands.default

import caseapp.*
import caseapp.core.Indexed

import scala.build.Logger
import scala.cli.ScalaCli
import scala.cli.commands.default.LegacyScalaOptions.*
import scala.cli.commands.package0.Package
import scala.cli.commands.tags

/** Options covering backwards compatibility with the old scala runner.
  */
// format: off
case class LegacyScalaOptions(
  @Group("Scala")
  @HelpMessage(s"Ignored legacy option. Deprecated equivalent of running a subsequent `$PowerString${Package.name}` command.")
  @Tag(tags.must)
  @Name("-save")
    save: Option[Indexed[Boolean]] = None,
  @Group("Scala")
  @HelpMessage("Ignored legacy option. Deprecated override canceling the `-nosave` option.")
  @Tag(tags.must)
  @Name("-nosave")
    nosave: Option[Indexed[Boolean]] = None,
) {
// format: on

  extension [T](indexedOption: Option[Indexed[T]]) {
    private def findArg(args: Array[String]): Option[String] =
      indexedOption.flatMap(io => args.lift(io.index))
  }

  def filterNonDeprecatedArgs(
    args: Array[String],
    progName: String,
    logger: Logger
  ): Array[String] = {
    val saveOptionString   = save.findArg(args)
    val noSaveOptionString = nosave.findArg(args)
    val deprecatedArgs     = Seq(saveOptionString, noSaveOptionString).flatten
    val filteredArgs       = args.filterNot(deprecatedArgs.contains)
    val filteredArgsString = filteredArgs.mkString(" ")
    saveOptionString.foreach { s =>
      logger.message(
        s"""Deprecated option '$s' is ignored.
           |The compiled project files will be saved in the '.scala-build' directory in the project root folder.
           |If you need to produce an actual jar file, run the '$PowerString${Package.name}' sub-command as follows:
           |  ${Console.BOLD}$progName $PowerString${Package.name} --library $filteredArgsString${Console.RESET}""".stripMargin
      )
    }
    noSaveOptionString.foreach { ns =>
      logger.message(
        s"""Deprecated option '$ns' is ignored.
           |A jar file is not saved unless the '$PowerString${Package.name}' sub-command is called.""".stripMargin
      )
    }
    filteredArgs
  }
}
object LegacyScalaOptions {
  implicit lazy val parser: Parser[LegacyScalaOptions] = Parser.derive
  implicit lazy val help: Help[LegacyScalaOptions]     = Help.derive

  private[default] lazy val PowerString =
    if ScalaCli.allowRestrictedFeatures then "" else "--power "
}
