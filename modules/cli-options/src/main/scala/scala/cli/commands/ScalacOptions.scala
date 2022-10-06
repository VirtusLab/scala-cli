package scala.cli.commands

import caseapp._
import caseapp.core.{Arg, Error}
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.util.Formatter
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

// format: off
final case class ScalacOptions(
  @Group("Scala")
  @HelpMessage("Add a scalac option")
  @ValueDescription("option")
  @Name("scala-opt")
  @Name("scala-option")
  @Name("O")
    scalacOption: List[String] = Nil
)
// format: on

object ScalacOptions {

  private val scalacOptionsArg = Arg("scalacOption")
    .withExtraNames(Seq(Name("scala-opt"), Name("O"), Name("scala-option")))
    .withValueDescription(Some(ValueDescription("option")))
    .withHelpMessage(Some(HelpMessage(
      "Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`."
    )))
    .withGroup(Some(Group("Scala")))
    .withOrigin(Some("ScalacOptions"))
  // .withIsFlag(true) // The scalac options we handle accept no value after the -… argument
  private val scalacOptionsPurePrefixes =
    Set("-V", "-W", "-X", "-Y")
  private val scalacOptionsPrefixes =
    Set("-g", "-language", "-opt", "-P", "-target") ++ scalacOptionsPurePrefixes
  private val scalacAliasedOptions = // these options don't require being passed after -O
    Set("-encoding", "-release")

  /** This includes all the scalac options which disregard inputs and print a help and/or context
    * message instead.
    */
  val ScalacPrintOptions: Set[String] =
    scalacOptionsPurePrefixes ++ Set("-help", "-Xshow-phases", "-Vphases")

  /** This includes all the scalac options which are redirected to native Scala CLI options. */
  val ScalaCliRedirectedOptions = Set(
    "-classpath", // redirected to --extra-jars
    "-d"          // redirected to --compilation-output
  )

  private val scalacOptionsArgument: Argument[List[String]] =
    new Argument[List[String]] {

      val underlying: StandardArgument[List[String]] = StandardArgument(scalacOptionsArg)

      val arg: Arg = scalacOptionsArg

      def withDefaultOrigin(origin: String): Argument[List[String]] = this
      def init: Option[List[String]]                                = Some(Nil)
      def step(
        args: List[String],
        index: Int,
        acc: Option[List[String]],
        formatter: Formatter[Name]
      ): Either[(Error, List[String]), Option[(Option[List[String]], List[String])]] =
        args match {
          case h :: t if scalacOptionsPrefixes.exists(h.startsWith) =>
            Right(Some((Some(h :: acc.getOrElse(Nil)), t)))
          case h :: t if scalacAliasedOptions.contains(h) =>
            // check if the next scalac arg is a different option or a param to the current option
            val maybeOptionArg = t.headOption.filter(!_.startsWith("-"))
            // if it's a param, it'll be treated as such and considered already parsed
            val newTail = maybeOptionArg.map(_ => t.drop(1)).getOrElse(t)
            val newHead = List(h) ++ maybeOptionArg
            Right(Some((Some(newHead ++ acc.getOrElse(Nil)), newTail)))
          case _ => underlying.step(args, index, acc, formatter)
        }
      def get(acc: Option[List[String]], formatter: Formatter[Name]): Either[Error, List[String]] =
        Right(acc.getOrElse(Nil))
    }

  implicit lazy val parser = {
    val baseParser =
      scalacOptionsArgument ::
        NilParser
    baseParser.to[ScalacOptions]
  }
  implicit lazy val help: Help[ScalacOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalacOptions] = JsonCodecMaker.make
}
