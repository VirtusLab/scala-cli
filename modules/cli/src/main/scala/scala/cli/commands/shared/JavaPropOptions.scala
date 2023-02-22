package scala.cli.commands.shared

import caseapp.*
import caseapp.core.Scala3Helpers.*
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class JavaPropOptions(
  @Group(HelpGroup.Java.toString)
  @HelpMessage("Set java properties")
  @ValueDescription("key=value|key")
  @Tag(tags.must)
    javaProp: List[String] = Nil
)                                  
// format: on

object JavaPropOptions {

  private val javaPropOptionsArg = Arg("javaPropOption").copy(
    extraNames = Seq(Name("java-prop")),
    valueDescription = Some(ValueDescription("key=value|key")),
    helpMessage = Some(HelpMessage(
      "Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`."
    )),
    group = Some(Group("Java")),
    origin = Some("JavaPropOptions")
  )

  private val javaPropOptionsArgument: Argument[List[String]] =
    new Argument[List[String]] {

      val underlying: StandardArgument[List[String]] = StandardArgument(javaPropOptionsArg)

      val arg: Arg = javaPropOptionsArg

      def withDefaultOrigin(origin: String): Argument[List[String]] = this
      def init: Option[List[String]]                                = Some(Nil)
      def step(
        args: List[String],
        index: Int,
        acc: Option[List[String]],
        formatter: Formatter[Name]
      ): Either[(Error, List[String]), Option[(Option[List[String]], List[String])]] =
        args match {
          case s"-D${prop}" :: t =>
            Right(Some((Some(prop :: acc.getOrElse(Nil)), t)))
          case _ => underlying.step(args, index, acc, formatter)
        }
      def get(acc: Option[List[String]], formatter: Formatter[Name]): Either[Error, List[String]] =
        Right(acc.getOrElse(Nil))
    }

  implicit lazy val parser: Parser[JavaPropOptions] = {
    val baseParser =
      javaPropOptionsArgument ::
        NilParser
    baseParser.to[JavaPropOptions]
  }
  implicit lazy val help: Help[JavaPropOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[JavaPropOptions] = JsonCodecMaker.make
}
