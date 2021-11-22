package scala.cli.commands

import caseapp._
import caseapp.core.Arg
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.util.Formatter
import upickle.default.{ReadWriter, macroRW}

// format: off
final case class ScalacOptions(
  @Group("Scala")
  @HelpMessage("Add a scalac option")
  @ValueDescription("option")
  @Name("scala-opt")
  @Name("O")
    scalacOption: List[String] = Nil
)
// format: on

object ScalacOptions {

  private val scalacOptionsArg = Arg("scalacOption")
    .withExtraNames(Seq(Name("scala-opt"), Name("O")))
    .withValueDescription(Some(ValueDescription("option")))
    .withHelpMessage(Some(HelpMessage(
      "Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`."
    )))
    .withGroup(Some(Group("Scala")))
    .withOrigin(Some("ScalacOptions"))
  // .withIsFlag(true) // The scalac options we handle accept no value after the -… argument
  private val scalacOptionsPrefixes =
    Set("-g", "-language", "-opt", "-P", "-target", "-V", "-W", "-X", "-Y")
  private val scalacOptionsArgument: Argument[List[String]] =
    new Argument[List[String]] {

      val underlying = StandardArgument[List[String]](scalacOptionsArg)

      val arg = scalacOptionsArg

      def withDefaultOrigin(origin: String) = this
      def init                              = Some(Nil)
      def step(args: List[String], acc: Option[List[String]], formatter: Formatter[Name]) =
        args match {
          case h :: t if scalacOptionsPrefixes.exists(h.startsWith) =>
            Right(Some((Some(h :: acc.getOrElse(Nil)), t)))
          case _ => underlying.step(args, acc, formatter)
        }
      def get(acc: Option[List[String]], formatter: Formatter[Name]) =
        Right(acc.getOrElse(Nil))
    }

  implicit lazy val parser = {
    val baseParser =
      scalacOptionsArgument ::
        NilParser
    baseParser.to[ScalacOptions]
  }
  implicit lazy val help: Help[SharedOptions]            = Help.derive
  implicit lazy val jsonCodec: ReadWriter[ScalacOptions] = macroRW
}
